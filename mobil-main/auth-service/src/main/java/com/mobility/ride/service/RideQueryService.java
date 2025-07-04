package com.mobility.ride.service;

import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.model.Ride;
import com.mobility.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>RideQueryService</h2>
 * <p>
 *     Service utilitaire destiné au back‑office chauffeur : il renvoie en temps
 *     réel la liste des courses encore <strong>REQUESTED</strong> dans un rayon donné
 *     autour d’un point (souvent le drop‑off du conducteur).
 * </p>
 * <ul>
 *     <li>Repose sur {@link RideRepository#findOpenNear(double, double, double)}
 *         introduit dans la v2025‑07‑25.</li>
 *     <li>Filtre en mémoire les demandes plus vieilles que <em>createdAt &gt; now‑3 min</em>
 *         (évite les vestiges si le scheduler de nettoyage tarde).</li>
 *     <li>Convertit chaque entité {@link Ride} en DTO {@link RideResponse}
 *         enrichi d’adresses lisibles via {@link GeoService#reverse(Double, Double)}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RideQueryService {

    private final RideRepository rideRepo;
    private final GeoService     geo;          // reverse‑geocoding déjà existant

    /**
     * Liste les rides encore en statut <code>REQUESTED</code> dans un rayon
     * de <code>radiusKm</code> autour du point <code>(lat,lng)</code>.
     */
    public List<RideResponse> listOpen(double lat, double lng, double radiusKm) {
        return rideRepo.findOpenNear(lat, lng, radiusKm)
                .stream()
                // facultatif : ignorer les demandes trop anciennes (> 3 min)
                .filter(r -> r.getCreatedAt()
                        .isAfter(OffsetDateTime.now().minusMinutes(3)))
                .map(this::toDto)
                .toList();
    }

    /* -------------------------------------------------------- */
    /*    Mapping interne  ➜  DTO exposé au front / driver      */
    /* -------------------------------------------------------- */
    private RideResponse toDto(Ride r) {
        return RideResponse.builder()
                .rideId        (r.getId())
                .status        (r.getStatus().name())
                .productType   (r.getProductType().name())
                .pickupLat     (r.getPickupLat())
                .pickupLng     (r.getPickupLng())
                .dropoffLat    (r.getDropoffLat())
                .dropoffLng    (r.getDropoffLng())
                .pickupAddress (geo.reverse(r.getPickupLat(),  r.getPickupLng()))
                .dropoffAddress(geo.reverse(r.getDropoffLat(), r.getDropoffLng()))
                .totalFare     (r.getTotalFare())
                .currency      (r.getCurrency())
                .createdAt     (r.getCreatedAt())
                .build();
    }
}
