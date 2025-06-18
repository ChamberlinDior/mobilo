// src/main/java/com/mobility/auth/repository/PaymentMethodRepository.java
package com.mobility.auth.repository;

import com.mobility.auth.model.PaymentMethod;
import com.mobility.auth.model.User;
import com.mobility.auth.model.enums.PaymentProvider;   // ← import enum
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /* Toutes les cartes d’un utilisateur */
    List<PaymentMethod> findAllByUser(User user);

    /* Suppression sécurisée */
    void deleteByIdAndUser(Long id, User user);

    /* Existe-t-il déjà un moyen de même provider (ex. CASH) ? */
    boolean existsByUserAndProvider(User user, PaymentProvider provider);      // ← TYPE CHANGÉ

    /* Unicité du token (Stripe / PayPal…) */
    boolean existsByUserAndToken(User user, String token);

    /* Retrouver la carte par id + user */
    Optional<PaymentMethod> findByIdAndUser(Long id, User user);
}
