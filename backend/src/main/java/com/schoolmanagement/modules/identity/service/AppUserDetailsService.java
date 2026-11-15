package com.schoolmanagement.modules.identity.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        AppUser appUser = appUserRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                    new UsernameNotFoundException("Invalid email or password"));

        String[] authorities = appUser.getRoles()
                .stream()
                .map(role -> "ROLE_" + role.name())
                .toArray(String[]::new);

        return User.withUsername(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .authorities(authorities)
                .disabled(!appUser.isEnabled())
                .build();
    }
}