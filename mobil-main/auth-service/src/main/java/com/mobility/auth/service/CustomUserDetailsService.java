// src/main/java/com/mobility/auth/service/CustomUserDetailsService.java
package com.mobility.auth.service;

import com.mobility.auth.model.User;
import com.mobility.auth.model.Role;
import com.mobility.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User appUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé pour l’email : " + email));

        // Construire la liste des authorities à partir de primaryRole + extraRoles
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // rôle principal
        authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getPrimaryRole().name()));
        // rôles additionnels
        appUser.getExtraRoles()
                .forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.name())));

        // Ici on utilise le UserDetails natif de Spring
        return org.springframework.security.core.userdetails.User
                .withUsername(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
