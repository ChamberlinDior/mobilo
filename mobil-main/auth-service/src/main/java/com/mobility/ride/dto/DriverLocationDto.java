/* ------------------------------------------------------------------ */
/* DriverLocationDto.java – simple DTO utilisé par le front            */
/* ------------------------------------------------------------------ */
package com.mobility.ride.dto;

import lombok.*;

@Getter @AllArgsConstructor(staticName = "of")
public class DriverLocationDto {
    private String  id;
    private Double  latitude;
    private Double  longitude;
    private Integer heading;      // nullable
}
