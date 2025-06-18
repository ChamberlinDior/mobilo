// ─────────────────────────────────────────────────────────────────────────────
//  PACKAGE : com.mobility.ride.dto
//  FILE    : SOSRequest.java
// ----------------------------------------------------------------------------
package com.mobility.ride.dto;

import com.mobility.ride.model.LatLng;
import jakarta.validation.constraints.NotNull;

/**
 * Payload JSON « POST /safety/sos ».
 *
 * Exemple :
 * ```json
 * {
 *   "rideId" : 123,
 *   "userId" : 987,
 *   "location" : { "lat": 0.391, "lng": 9.453 }
 * }
 * ```
 */
public record SOSRequest(
        @NotNull Long   rideId,
        @NotNull Long   userId,
        @NotNull LatLng location
) {}
