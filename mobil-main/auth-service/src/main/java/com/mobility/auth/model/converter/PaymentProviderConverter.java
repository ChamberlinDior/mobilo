// src/main/java/com/mobility/auth/model/converter/PaymentProviderConverter.java
package com.mobility.auth.model.converter;

import com.mobility.auth.model.enums.PaymentProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convertisseur JPA pour {@link PaymentProvider}.
 * <p>
 *  • En base : on stocke la constante enum en MAJUSCULE (VARCHAR(32)).<br>
 *  • À la lecture : on tolère la casse, les espaces, ainsi que les anciennes
 *    lignes où le provider était <code>null</code> ou vide (retourne STRIPE).
 */
@Converter(autoApply = true)                     // ← conversion globale
public class PaymentProviderConverter
        implements AttributeConverter<PaymentProvider, String> {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentProviderConverter.class);

    /* ═════════════ vers la base ═════════════ */

    @Override
    public String convertToDatabaseColumn(PaymentProvider attribute) {
        return attribute == null ? null : attribute.name(); // STRIPE, CASH, …
    }

    /* ═════════════ depuis la base ═════════════ */

    @Override
    public PaymentProvider convertToEntityAttribute(String dbData) {

        // Back-compat : anciennes lignes sans provider => STRIPE
        if (dbData == null || dbData.trim().isEmpty()) {
            return PaymentProvider.STRIPE;
        }

        try {
            return PaymentProvider.from(dbData);           // tolère la casse/espaces
        } catch (IllegalArgumentException ex) {
            // Provider inconnu en base -> on logue et on relaie l’erreur
            log.error("Provider inconnu dans la table payment_methods : «{}»", dbData);
            throw ex;
        }
    }
}
