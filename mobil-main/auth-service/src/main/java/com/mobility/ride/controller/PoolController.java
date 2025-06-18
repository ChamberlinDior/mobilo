// ============================================================================
//  FILE    : PoolController.java
// ----------------------------------------------------------------------------
package com.mobility.ride.controller;

import com.mobility.ride.dto.*;
import com.mobility.ride.service.PoolMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints liés au produit <em>Pool / Shared rides</em>.
 */
@Tag(name = "Pool", description = "Shared‑ride availability & booking")
@RestController
@RequestMapping("/api/v1/pool")
@RequiredArgsConstructor
public class PoolController {

    private final PoolMatchService poolMatchService;

    /* ═══════════ Disponibilité / éligibilité ═══════════ */
    @Operation(summary = "Check Pool eligibility",
            responses = @ApiResponse(responseCode = "200", description = "Eligibility",
                    content = @Content(schema = @Schema(implementation = PoolEligibilityResponse.class))))
    @GetMapping("/availability")
    public ResponseEntity<PoolEligibilityResponse> eligibility(@Valid PoolEligibilityRequest req) {
        return ResponseEntity.ok(poolMatchService.checkEligibility(req));
    }

    /* ═══════════ Création d'une ride Pool ═══════════ */
    @Operation(summary = "Request a Pool ride",
            responses = @ApiResponse(responseCode = "201", description = "Ride created",
                    content = @Content(schema = @Schema(implementation = PriceQuoteResponse.class))))
    @PostMapping(path = "/rides", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PriceQuoteResponse> createPoolRide(@RequestBody @Valid PriceQuoteRequest body) {
        PriceQuoteResponse quote = poolMatchService.createPoolRide(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(quote);
    }
}
