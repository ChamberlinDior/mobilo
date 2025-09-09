// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/repository/UserRepository.java
//  v2025-09-05 – ré-expose findByExternalUid (compat RideUserService)
//               + projections « snippet » Driver/Rider
//               + verrou PESSIMISTIC_WRITE pour MAJ de solde
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.auth.repository;

import com.mobility.auth.model.Role;
import com.mobility.auth.model.User;
import com.mobility.auth.repository.view.UserSnippetView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    /* ══════════════ Recherche “classique” ══════════════ */
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Identifiant fonctionnel : externalUid ←→ uid logique (champ « sub » du JWT).
     * NB : `findByExternalUid` est conservé pour compatibilité.
     */
    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByUid(@Param("uid") String uid);

    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByExternalUid(@Param("uid") String uid);     // compat

    /**
     * Verrou d'écriture pour MAJ atomique du solde (TOP_UP / WITHDRAW).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByExternalUidForUpdate(@Param("uid") String uid);

    /* Recherche par rôle principal (drivers, admins, …) */
    @Query("select u from User u where u.primaryRole = :role")
    Set<User> findAllByPrimaryRole(@Param("role") Role role);

    /* ══════════════ Projections « snippet » ══════════════ */
    @Query("""
           select u.id                     as id,
                  u.firstName              as firstName,
                  u.lastName               as lastName,
                  u.profilePicture         as profilePicture,
                  u.profilePictureMimeType as profilePictureMimeType,
                  u.rating                 as rating
             from User u
            where u.id = :id
           """)
    Optional<UserSnippetView> findDriverSnippetById(@Param("id") Long id);

    @Query("""
           select u.id                     as id,
                  u.firstName              as firstName,
                  u.lastName               as lastName,
                  u.profilePicture         as profilePicture,
                  u.profilePictureMimeType as profilePictureMimeType,
                  u.rating                 as rating
             from User u
            where u.id = :id
           """)
    Optional<UserSnippetView> findRiderSnippetById(@Param("id") Long id);
}
