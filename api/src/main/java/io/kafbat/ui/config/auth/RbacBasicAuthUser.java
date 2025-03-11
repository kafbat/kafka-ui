package io.kafbat.ui.config.auth;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class RbacBasicAuthUser implements UserDetails, RbacUser {
  private final UserDetails userDetails;
  private final Collection<String> groups;

  public RbacBasicAuthUser(UserDetails userDetails, Collection<String> groups) {
    this.userDetails = userDetails;
    this.groups = groups;
  }

  @Override
  public String name() {
    return userDetails.getUsername();
  }

  @Override
  public Collection<String> groups() {
    return groups;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return userDetails.getAuthorities();
  }

  @Override
  public String getPassword() {
    return userDetails.getPassword();
  }

  @Override
  public String getUsername() {
    return userDetails.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return userDetails.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return userDetails.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return userDetails.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return userDetails.isEnabled();
  }
}
