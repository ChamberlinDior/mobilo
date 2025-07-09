// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/repository/UserRepository.java
//  v2025-09-05 – ré-expose findByExternalUid (compatibilité RideUserService)
//               + projections « snippet » Driver / Rider
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.auth.repository;

import com.mobility.auth.model.Role;
import com.mobility.auth.model.User;
import com.mobility.auth.repository.view.UserSnippetView;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * **NB :** `findByExternalUid` est conservé pour ne rien casser côté
     * `RideUserService`.  `findByUid` reste la méthode “préférée”.
     */
    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByUid(@Param("uid") String uid);

    @Query("select u from User u where u.externalUid = :uid")
    Optional<User> findByExternalUid(@Param("uid") String uid);     // ← compat

    /* Recherche par rôle principal (drivers, admins, …) */
    @Query("select u from User u where u.primaryRole = :role")
    Set<User> findAllByPrimaryRole(@Param("role") Role role);

    /* ══════════════ Projections « snippet » ══════════════ */
    /**
     * Mini-fiche utilisateur (id, nom, avatar, rating)
     * – utilisée pour l’écran « match » afin d’éviter un `SELECT *`.
     */
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

    /** Rider : même projection (la note n’est pas forcément affichée). */
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
