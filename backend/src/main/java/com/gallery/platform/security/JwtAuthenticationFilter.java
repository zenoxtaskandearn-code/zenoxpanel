package com.gallery.platform.security;

import com.gallery.platform.admin.AdminRepository;
import com.gallery.platform.device.DeviceRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final DeviceRepository deviceRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AdminRepository adminRepository, DeviceRepository deviceRepository) {
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (StringUtils.hasText(request.getParameter("access_token"))) {
            // allow token for websocket handshake or streaming contracts if needed
            token = request.getParameter("access_token");
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isValid(token)) {
            try {
                Claims claims = jwtService.parse(token);
                String typ = claims.get("typ", String.class);
                AppPrincipal principal = null;
                if ("ADMIN".equals(typ)) {
                    Long aid = ((Number) claims.get("aid")).longValue();
                    var admin = adminRepository.findById(aid).orElse(null);
                    if (admin != null && admin.isEnabled()) {
                        principal = AppPrincipal.admin(admin.getId(), admin.getEmail(), admin.getPasswordHash(), true, admin.getRole());
                    }
                } else if ("DEVICE".equals(typ)) {
                    Long did = ((Number) claims.get("did")).longValue();
                    var device = deviceRepository.findById(did).orElse(null);
                    if (device != null && !device.isRevoked()) {
                        principal = AppPrincipal.device(device.getId(), device.getDeviceUuid());
                    }
                }
                if (principal != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // invalid/expired/malformed - leave unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }
}
