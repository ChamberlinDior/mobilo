// ─────────────────────────────────────────────────────────────────────────────
//  FILE : SurgeMultiplierRepository.java
// ----------------------------------------------------------------------------
package com.mobility.ride.repository;

import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.SurgeMultiplier;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface SurgeMultiplierRepository
        extends JpaRepository<SurgeMultiplier, Long> {

    /* Multiplicateur actif à l’instant T */
    @Query("""
            select s
              from SurgeMultiplier s
             where s.cityId      = :cityId
               and s.productType = :product
               and :ts between s.windowStart and s.windowEnd
           """)
    Optional<SurgeMultiplier> findActive(
            @Param("cityId")  Long        cityId,
            @Param("product") ProductType product,
            @Param("ts")      OffsetDateTime ts
    );

    /* Dernier multiplicateur connu (pour warm-up cache) */
    Optional<SurgeMultiplier>
    findTopByCityIdAndProductTypeOrderByWindowEndDesc(
            Long cityId, ProductType productType);
}
