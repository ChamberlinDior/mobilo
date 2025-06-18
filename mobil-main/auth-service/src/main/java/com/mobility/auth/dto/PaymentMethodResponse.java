// src/main/java/com/mobility/auth/dto/PaymentMethodResponse.java
package com.mobility.auth.dto;

import com.mobility.auth.model.enums.PaymentProvider;
import lombok.*;

/**
 * DTO renvoyé au frontend après création, lecture ou mise à jour
 * d’un moyen de paiement.
 *
 * <p><strong>NOUVEAUTÉ :</strong> le champ {@code type} correspond
 * à la nouvelle colonne NOT&nbsp;NULL de la table <em>payment_methods</em>.
 * Ses valeurs possibles sont :</p>
 * <ul>
 *   <li><code>card</code>   – carte bancaire / Apple Pay via Stripe</li>
 *   <li><code>wallet</code> – PayPal, Airtel Money, etc.</li>
 *   <li><code>cash</code>   – règlement en espèces au chauffeur</li>
 * </ul>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethodResponse {

    /* ─────────── Identifiant ─────────── */
    private Long id;

    /* ─────────── Famille (NOUVEAU) ───── */
    private String type;               // card | wallet | cash

    /* ─────────── Prestataire ─────────── */
    private PaymentProvider provider;  // STRIPE, PAYPAL, CASH…

    /* ─────────── Détails carte ───────── */
    private String  brand;             // VISA / PAYPAL / AIRTEL…
    private String  last4;             // 4 chiffres ou numéro masqué
    private Integer expMonth;          // null si N/A
    private Integer expYear;           // null si N/A

    /* ─────────── Statut ──────────────── */
    private Boolean isDefault;         // true = moyen par défaut
}
