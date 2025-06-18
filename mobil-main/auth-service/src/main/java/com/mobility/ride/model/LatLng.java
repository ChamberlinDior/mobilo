// ------------------------------------------------------------------
// LatLng.java – embeddable latitude/longitude WGS-84  (corrigé)
// ------------------------------------------------------------------
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatLng {

    @Column(nullable = false)   // ❌ plus de precision/scale sur DOUBLE
    private Double lat;         // -90 … +90

    @Column(nullable = false)
    private Double lng;         // -180 … +180
}
