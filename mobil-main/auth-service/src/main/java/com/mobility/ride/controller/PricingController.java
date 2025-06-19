// ============================================================================
//  PACKAGE : com.mobility.ride.controller
//  FILE    : PricingController.java               (v2025-07-02)
// ============================================================================
package com.mobility.ride.controller;

import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.dto.SurgeInfoResponse;
import com.mobility.ride.geo.CityInfo;
import com.mobility.ride.geo.GeoLocationService;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.service.SurgePricingService;
import com.mobility.ride.service.UpfrontPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>REST PricingController – v1</h2>
 *
 * <p>Expose :</p>
 * <ul>
 *   <li><strong>POST /pricing/quote</strong> – prix fixe (up-front) pour courses & livraisons</li>
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
    private final GeoLocationService geoLocationService;

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
        // 1) Résolution cityId & currency via géoloc
        CityInfo info;
        try {
            info = geoLocationService.resolve(req.pickup());
        } catch (Exception ex) {
            info = new CityInfo(req.cityId(), req.currency());
        }

        // 2) Défaut de zone à LOCAL si non fourni
        DeliveryZone zone = req.deliveryZone() == null
                ? DeliveryZone.LOCAL
                : req.deliveryZone();

        // 3) Construire la requête interne (le service gère désormais la validation du poids)
        PriceQuoteRequest internalReq = new PriceQuoteRequest(
                info.cityId(),
                req.productType(),
                req.pickup(),
                req.dropoff(),
                req.distanceKm(),
                req.durationMin(),
                info.currency(),
                null,              // surgeFactor géré en interne
                req.options(),
                req.weightKg(),    // null pour LOCAL → traité comme 0 dans le service
                zone
        );

        // 4) Appel du service tarifaire
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
            @RequestParam Long        cityId,
            @RequestParam ProductType productType
    ) {
        SurgeInfoResponse info = surgePricingService.getSurgeInfo(cityId, productType);
        return ResponseEntity.ok(info);
    }
}
