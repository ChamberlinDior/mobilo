/* ----------------------------------------------------------- */
/*  src/main/java/com/mobility/ride/mapper/PricingMapper.java  */
/* ----------------------------------------------------------- */
package com.mobility.ride.mapper;

import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.dto.SurgeInfoResponse;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.SurgeMultiplier;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PricingMapper {

    /* ─── 1) SurgeMultiplier ➜ SurgeInfoResponse ─── */
    @Mapping(target = "surgeActive",
            expression = "java(src.getFactor()!=null && src.getFactor().doubleValue() > 1.0)")
    SurgeInfoResponse toSurgeInfoDto(SurgeMultiplier src);

    /* ─── 2) Factory PriceQuoteResponse ─── */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    // champs existants
    @Mapping(target = "cityId",        source = "cityId")
    @Mapping(target = "productType",   source = "productType")
    @Mapping(target = "distanceKm",    source = "distanceKm")
    @Mapping(target = "durationSec",   source = "durationSec")
    @Mapping(target = "baseFare",      source = "baseFare")
    @Mapping(target = "surgeFactor",   source = "surgeFactor")
    @Mapping(target = "totalFare",     source = "totalFare")
    @Mapping(target = "currency",      source = "currency")
    @Mapping(target = "expiresAt",     source = "expiresAt")
    @Mapping(target = "upfront",       constant = "true")

    // nouveaux champs pour livraison
    @Mapping(target = "weightKg",      source = "weightKg")
    @Mapping(target = "deliveryZone",  source = "deliveryZone")

    // champs non encore gérés
    @Mapping(target = "ratePerKmUsed", ignore = true)
    @Mapping(target = "flatFareLabel", ignore = true)
    @Mapping(target = "platformShare", ignore = true)

    PriceQuoteResponse toPriceQuoteDto(
            Long          cityId,
            ProductType   productType,
            double        distanceKm,
            long          durationSec,
            BigDecimal    baseFare,
            BigDecimal    surgeFactor,
            BigDecimal    totalFare,
            String        currency,
            OffsetDateTime expiresAt,
            BigDecimal    weightKg,
            DeliveryZone  deliveryZone
    );
}
