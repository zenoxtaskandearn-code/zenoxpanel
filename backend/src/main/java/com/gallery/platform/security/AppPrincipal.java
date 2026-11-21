package com.gallery.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppPrincipal implements UserDetails {
    public enum SubjectType { ADMIN, DEVICE }

    private final SubjectType subjectType;
    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final List<SimpleGrantedAuthority> authorities;
    private final Long deviceId; // for device principals convenience

    public AppPrincipal(SubjectType subjectType, Long id, String username, String password, boolean enabled,
                        List<String> roles, Long deviceId) {
        this.subjectType = subjectType;
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = roles == null ? List.of() : roles.stream().map(SimpleGrantedAuthority::new).toList();
        this.deviceId = deviceId;
    }

    public static AppPrincipal admin(Long id, String email, String password, boolean enabled, String role) {
        return new AppPrincipal(SubjectType.ADMIN, id, email, password, enabled, List.of("ROLE_" + role), null);
    }

    public static AppPrincipal device(Long deviceDbId, String deviceUuid) {
        return new AppPrincipal(SubjectType.DEVICE, deviceDbId, deviceUuid, "", true, List.of("ROLE_DEVICE"), deviceDbId);
    }

    public SubjectType getSubjectType() { return subjectType; }
    public Long getId() { return id; }
    public Long getDeviceId() { return deviceId; }
    public boolean isAdmin() { return subjectType == SubjectType.ADMIN; }
    public boolean isDevice() { return subjectType == SubjectType.DEVICE; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
