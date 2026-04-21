package com.laneflow.engine.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserPrincipal(
        String id,
        String username,
        String password,
        String roleCode,
        boolean active,
        List<GrantedAuthority> authorities
) implements UserDetails {

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                      { return password; }
    @Override public String getUsername()                                      { return username; }
    @Override public boolean isEnabled()                                       { return active; }
    @Override public boolean isAccountNonExpired()                             { return true; }
    @Override public boolean isAccountNonLocked()                              { return true; }
    @Override public boolean isCredentialsNonExpired()                         { return true; }
}
