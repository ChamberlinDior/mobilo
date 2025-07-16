// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/repository/PushTokenRepository.java
//  v2025-10-11 – ajout findAllByUserIdIn(...)
// ─────────────────────────────────────────────────────────────
package com.mobility.auth.repository;

import com.mobility.auth.model.PushToken;
import com.mobility.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    /* jetons d’un seul utilisateur */
    List<PushToken> findAllByUser(User user);

    /* jetons d’un ensemble d’utilisateurs – utilisé par ChatService */
    List<PushToken> findAllByUserIdIn(List<Long> userIds);

    /* révocation d’un token précis */
    void deleteByTokenAndUser(String token, User user);
}
