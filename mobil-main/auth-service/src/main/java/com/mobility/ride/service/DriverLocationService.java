/* ------------------------------------------------------------------ */
/* DriverLocationService.java – nouveau                                */
/* ------------------------------------------------------------------ */
package com.mobility.ride.service;

import com.mobility.ride.dto.DriverLocationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class DriverLocationService {

    /** Demo : renvoie 5 voitures ‘‘placées’’ aléatoirement à ±300 m. */
    public List<DriverLocationDto> findNearby(double lat, double lng, double radiusKm) {
        return ThreadLocalRandom.current()
                .doubles(5, -radiusKm / 111.0, radiusKm / 111.0)  // ~°→km
                .mapToObj(dLat -> DriverLocationDto.of(
                        String.valueOf(ThreadLocalRandom.current().nextInt(1_000_000)),
                        lat + dLat,
                        lng + ThreadLocalRandom.current().nextDouble(-radiusKm / 111.0, radiusKm / 111.0),
                        ThreadLocalRandom.current().nextInt(0, 360)
                ))
                .toList();
    }
}
