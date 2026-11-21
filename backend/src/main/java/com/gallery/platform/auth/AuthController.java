package com.gallery.platform.auth;

import com.gallery.platform.auth.dto.AuthDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request,
                                                          HttpServletRequest http) {
        return ResponseEntity.ok(authService.register(request, http));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                       HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request,
                                                         HttpServletRequest http) {
        return ResponseEntity.ok(authService.refresh(request, http));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request,
                                                     @AuthenticationPrincipal AppPrincipal principal,
                                                     HttpServletRequest http) {
        authService.logout(request, principal, http);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.AdminProfile> me(@AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthDtos.AdminProfile> updateProfile(@AuthenticationPrincipal AppPrincipal principal,
                                                               @Valid @RequestBody AuthDtos.UpdateProfileRequest request,
                                                               HttpServletRequest http) {
        return ResponseEntity.ok(authService.updateProfile(principal, request, http));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal AppPrincipal principal,
                                                              @Valid @RequestBody AuthDtos.ChangePasswordRequest request,
                                                              HttpServletRequest http) {
        authService.changePassword(principal, request, http);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
