/* ------------------------------------------------------------------ */
/* DriverController.java – nouveau                                     */
/* ------------------------------------------------------------------ */
package com.mobility.ride.controller;

import com.mobility.ride.dto.DriverLocationDto;
import com.mobility.ride.service.DriverLocationService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverLocationService driverLocationService;

    /**
     * Liste les chauffeurs proches du drop-off.
     *
     * Ex.  GET /api/v1/drivers?dropoffLat=48.85&dropoffLng=2.35&radiusKm=3
     */
    @GetMapping
    public ResponseEntity<List<DriverLocationDto>> listNearDestination(
            @NotNull  @DecimalMin("-90.0")  @DecimalMax("90.0")   Double dropoffLat,
            @NotNull  @DecimalMin("-180.0") @DecimalMax("180.0")  Double dropoffLng,
            @DecimalMin("0.1")              @DecimalMax("20.0")   Double radiusKm
    ) {
        List<DriverLocationDto> list = driverLocationService
                .findNearby(dropoffLat, dropoffLng, radiusKm == null ? 3.0 : radiusKm);
        return ResponseEntity.ok(list);
    }
}
