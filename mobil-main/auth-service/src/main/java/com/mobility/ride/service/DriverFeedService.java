/* ──────────────────────────────────────────────────────────────
 *  FILE : src/main/java/com/mobility/ride/service/DriverFeedService.java
 *  v2025-08-10 – interface
 * ────────────────────────────────────────────────────────────── */
package com.mobility.ride.service;

import com.mobility.ride.dto.RideOfferDto;
import java.util.List;

public interface DriverFeedService {

    List<RideOfferDto> findOpenRides      (Double lat, Double lng, Double radiusKm);
    List<RideOfferDto> findOpenParcels    (Double lat, Double lng, Double radiusKm);
    List<RideOfferDto> findScheduledRides (Double lat, Double lng, Double radiusKm);
}
