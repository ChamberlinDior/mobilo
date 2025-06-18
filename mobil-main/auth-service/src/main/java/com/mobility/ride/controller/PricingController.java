// ============================================================================
//  PACKAGE : com.mobility.ride.controller
//  FILE    : PricingController.java
// ----------------------------------------------------------------------------
package com.mobility.ride.controller;

import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.dto.SurgeInfoResponse;
import com.mobility.ride.geo.CityInfo;
import com.mobility.ride.geo.GeoLocationService;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.service.SurgePricingService;
import com.mobility.ride.service.UpfrontPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>REST PricingController – v1</h2>
 *
 * <p>Expose :</p>
 * <ul>
 *   <li><strong>POST /pricing/quote</strong> – prix fixe (up-front)</li>
 *   <li><strong>GET  /pricing/surge</strong> – multiplicateur temps-réel</li>
 * </ul>
 */
@Tag(name = "Pricing", description = "Up-front fares & surge multipliers")
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final UpfrontPriceService upfrontPriceService;
    private final SurgePricingService surgePricingService;
    private final GeoLocationService  geoLocationService;

    /* ═══════════ Up-front fare ═══════════ */
    @Operation(
            summary   = "Get upfront price quote",
            responses = @ApiResponse(
                    responseCode = "200",
                    description  = "Quote OK",
                    content      = @Content(schema = @Schema(implementation = PriceQuoteResponse.class))
            )
    )
    @PostMapping("/quote")
    public ResponseEntity<PriceQuoteResponse> quote(
            @Valid @RequestBody PriceQuoteRequest req
    ) {
        /* 1) Résoudre cityId + currency :
              • on tente la géoloc Google
              • en cas d’échec → on garde les valeurs fournies par le client              */
        CityInfo info;
        try {
            info = geoLocationService.resolve(req.pickup());
        } catch (Exception ex) {
            info = new CityInfo(req.cityId(), req.currency());
        }

        /* 2) Reconstituer une requête interne normalisée */
        PriceQuoteRequest internalReq = new PriceQuoteRequest(
                info.cityId(),
                req.productType(),
                req.pickup(),
                req.dropoff(),
                req.distanceKm(),
                req.durationMin(),
                info.currency(),
                null,                 // surgeFactor : calculé par le service
                req.options()
        );

        /* 3) Calcul du devis */
        PriceQuoteResponse resp = upfrontPriceService.quote(internalReq);
        return ResponseEntity.ok(resp);
    }

    /* ═══════════ Surge multiplier ═══════════ */
    @Operation(
            summary   = "Get surge multiplier",
            responses = @ApiResponse(
                    responseCode = "200",
                    description  = "Surge info",
                    content      = @Content(schema = @Schema(implementation = SurgeInfoResponse.class))
            )
    )
    @GetMapping("/surge")
    public ResponseEntity<SurgeInfoResponse> surge(
            @RequestParam @NotNull Long        cityId,
            @RequestParam @NotNull ProductType productType
    ) {
        SurgeInfoResponse info = surgePricingService.getSurgeInfo(cityId, productType);
        return ResponseEntity.ok(info);
    }
}
