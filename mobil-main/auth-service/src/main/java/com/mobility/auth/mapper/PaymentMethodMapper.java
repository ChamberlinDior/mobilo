// src/main/java/com/mobility/auth/mapper/PaymentMethodMapper.java
package com.mobility.auth.mapper;

import com.mobility.auth.dto.PaymentMethodRequest;
import com.mobility.auth.dto.PaymentMethodResponse;
import com.mobility.auth.model.PaymentMethod;
import com.mobility.auth.model.User;
import com.mobility.auth.model.enums.PaymentProvider;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct – conversions PaymentMethod ⇆ DTO.
 *
 *  • String côté API  ⇄  PaymentProvider côté entité
 *  • Helpers => évite les erreurs “incompatible types”
 */
@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    /*────────────────── Helpers de conversion pour `provider` ──────────────────*/
    default PaymentProvider toEnum(String raw) { return PaymentProvider.from(raw); }
    default String          toString(PaymentProvider p) { return p.name().toLowerCase(); }

    /*────────────────── Helper de normalisation pour `type` ────────────────────*/
    @Named("normType")
    default String normType(String v) {
        return v == null ? null : v.trim().toLowerCase();   // "CARD" → "card", etc.
    }

    /*────────────────── Request  ➜  Entity ─────────────────────────────────────*/
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mappings({
            @Mapping(target = "id",            ignore = true),
            @Mapping(target = "defaultMethod", ignore = true),
            @Mapping(target = "user",          expression = "java(user)"),

            /* nouveau : copie le champ type (card / cash) */
            @Mapping(target = "type", source = "type", qualifiedByName = "normType")
            /* les autres champs (provider, token, brand…) sont mappés auto.      */
    })
    PaymentMethod toEntity(PaymentMethodRequest dto, @Context User user);

    /*────────────────── Entity  ➜  Response DTO ────────────────────────────────*/
    @Mapping(target = "isDefault", source = "defaultMethod")
    PaymentMethodResponse toDto(PaymentMethod entity);

    List<PaymentMethodResponse> toDto(List<PaymentMethod> entities);
}
