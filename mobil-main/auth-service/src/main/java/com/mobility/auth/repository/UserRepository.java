// src/main/java/com/mobility/auth/repository/UserRepository.java
package com.mobility.auth.repository;

import com.mobility.auth.model.Role;
import com.mobility.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

/**
 * Accès base pour l’entité {@link User}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /* ─────── Recherche par email / phone ─────── */

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /* ─────── Recherche par externalUid (alias « uid » logique) ─────── */

    /**
     * Trouve un utilisateur à partir de son identifiant fonctionnel (uid).
     * <p>
     *  On mappe {@code uid} → champ {@code externalUid} dans l’entité.
     */
    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByUid(String uid);

    /* ─────── Autres méthodes existantes ─────── */

    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByExternalUid(String uid);   // ← peut être supprimée si doublon

    @Query("select u from User u where u.primaryRole = :role")
    Set<User> findAllByPrimaryRole(Role role);
}
