// ============================================================================
//  SERVICE : SurgePricingService
//  Gestion et cache du multiplicateur « surge »
// ============================================================================

package com.mobility.ride.service;

import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.dto.SurgeInfoResponse;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.SurgeMultiplier;
import com.mobility.ride.repository.SurgeMultiplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurgePricingService {

    private final SurgeMultiplierRepository repo;
    private final PricingProperties      pricingProperties;  // ← Injection pour évolution future

    /**
     * Info complète – utilisée par PricingController.
     * Le cityId est passé en paramètre, mais on le retrouve aussi dans pricingProperties.
     */
    @Cacheable(
            cacheNames = "surge::info",
            key = "'info:' + #cityId + ':' + #productType.name()"
    )
    @Transactional(readOnly = true)
    public SurgeInfoResponse getSurgeInfo(Long cityId, ProductType productType) {
        // Exemple d’accès futur aux règles de configuration tarifaire :
        // PricingProperties.Country cfg = pricingProperties.getCountry().get("gabon");

        OffsetDateTime now = OffsetDateTime.now();
        Optional<SurgeMultiplier> opt = repo.findActive(cityId, productType, now);

        if (opt.isEmpty()) {
            log.trace("No active surge – city={} product={} ⇒ 1.0", cityId, productType);
            return new SurgeInfoResponse(
                    cityId, productType, 1.0, false, null, null
            );
        }

        SurgeMultiplier sm = opt.get();
        double factor = sm.getFactor() == null
                ? 1.0
                : sm.getFactor().doubleValue();

        return new SurgeInfoResponse(
                cityId,
                productType,
                factor,
                factor > 1.0,
                sm.getWindowStart(),
                sm.getWindowEnd()
        );
    }

    /**
     * Facteur brut (utilisé ailleurs).
     */
    @Cacheable(
            cacheNames = "surge::factor",
            key = "'factor:' + #cityId + ':' + #productType.name()"
    )
    @Transactional(readOnly = true)
    public double getSurgeFactor(Long cityId, ProductType productType) {
        return getSurgeInfo(cityId, productType).factor();
    }

    /**
     * Pré-chauffage du cache (Scheduler).
     */
    @Transactional(readOnly = true)
    public void warmUpCache(Long cityId, ProductType productType) {
        repo.findTopByCityIdAndProductTypeOrderByWindowEndDesc(cityId, productType)
                .ifPresent(sm -> log.debug(
                        "⚡ Warm-up surge cache city={} product={} factor={}",
                        cityId, productType, sm.getFactor()
                ));
    }
}
