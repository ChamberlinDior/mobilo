/* ──────────────────────────────────────────────────────────────
 *  FILE : src/main/java/com/mobility/ride/service/DriverFeedServiceImpl.java
 *  v2025-10-11 – fenêtre planifiées : ≤ 25 min
 * ────────────────────────────────────────────────────────────── */
package com.mobility.ride.service;

import com.mobility.ride.dto.RideOfferDto;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.Ride;
import com.mobility.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverFeedServiceImpl implements DriverFeedService {

    private final RideRepository rideRepository;
    private final GeoService     geoService;

    private static final double EARTH_RADIUS_KM   = 6_371;
    private static final int    ACCEPT_WINDOW_MIN = 25;   // ← fenêtre d’acceptation

    /* ───────── 1) Courses immédiates ───────── */
    @Override
    public List<RideOfferDto> findOpenRides(Double lat, Double lng, Double radiusKm) {
        return rideRepository.findOpenNear(lat, lng, radiusKm)
                .stream()
                .map(r -> mapToDto(r, lat, lng))
                .toList();
    }

    /* ───────── 2) Colis immédiats ───────── */
    @Override
    public List<RideOfferDto> findOpenParcels(Double lat, Double lng, Double radiusKm) {
        return rideRepository.findOpenNear(lat, lng, radiusKm)
                .stream()
                .filter(r -> r.getProductType() == ProductType.DELIVERY)
                .map(r -> mapToDto(r, lat, lng))
                .toList();
    }

    /* ───────── 3) Planifiées : ≤ 25 min avant scheduledAt ───────── */
    @Override
    public List<RideOfferDto> findScheduledRides(Double lat, Double lng, Double radiusKm) {
        OffsetDateTime now   = OffsetDateTime.now();
        OffsetDateTime limit = now.plusMinutes(ACCEPT_WINDOW_MIN);

        return rideRepository.findScheduledBetween(now, limit)   // ← borne haute
                .stream()
                .filter(r -> distance(lat, lng,
                        r.getPickupLat(), r.getPickupLng()) <= radiusKm)
                .map(r -> mapToDto(r, lat, lng))
                .toList();
    }

    /* ───────── Mapping entité ➜ DTO ───────── */
    private RideOfferDto mapToDto(Ride r, double originLat, double originLng) {
        double dist = distance(originLat, originLng,
                r.getPickupLat(), r.getPickupLng());
        return RideOfferDto.builder()
                .rideId        (r.getId())
                .productType   (r.getProductType().name())
                .pickupLat     (r.getPickupLat())
                .pickupLng     (r.getPickupLng())
                .dropoffLat    (r.getDropoffLat())
                .dropoffLng    (r.getDropoffLng())
                .pickupAddress (geoService.reverse(r.getPickupLat(), r.getPickupLng()))
                .dropoffAddress(geoService.reverse(r.getDropoffLat(), r.getDropoffLng()))
                .totalFare     (r.getTotalFare().doubleValue())
                .currency      (r.getCurrency())
                .createdAt     (r.getCreatedAt().toString())
                .scheduledAt   (r.getScheduledAt() != null
                        ? r.getScheduledAt().toString()
                        : null)
                .distanceKm    (dist)
                .weightKg      (r.getWeightKg() != null
                        ? r.getWeightKg().doubleValue()
                        : null)
                .build();
    }

    /* ───────── Haversine (km) ───────── */
    private double distance(double lat1, double lon1,
                            double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM
                * 2
                * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
