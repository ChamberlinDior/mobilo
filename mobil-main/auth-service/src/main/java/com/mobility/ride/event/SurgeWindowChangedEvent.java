// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE : com.mobility.ride.event
// ----------------------------------------------------------------------------
package com.mobility.ride.event;

import com.mobility.ride.model.ProductType;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * <h2>Domain event – SurgeWindowChanged</h2>
 *
 * <p>Publié lorsqu’un multiplicateur « surge » est créé, modifié ou expiré
 * pour un produit et une ville donnés.
 * Écouté typiquement par&nbsp;:</p>
 * <ul>
 *   <li>les services de <strong>pricing cache</strong> (rafraîchir Redis),</li>
 *   <li>le <strong>front-office</strong> (WebSocket ↓),</li>
 *   <li>l’<strong>engine ML</strong> (historisation, forecast).</li>
 * </ul>
 *
 * <p>⚠️  L’instance est <em>immutable</em> ; expose uniquement des getters.</p>
 */
public class SurgeWindowChangedEvent extends ApplicationEvent {

    private final Long        cityId;
    private final ProductType productType;
    private final BigDecimal  newFactor;
    private final OffsetDateTime windowStart;
    private final OffsetDateTime windowEnd;

    public SurgeWindowChangedEvent(Object source,
                                   Long cityId,
                                   ProductType productType,
                                   BigDecimal newFactor,
                                   OffsetDateTime windowStart,
                                   OffsetDateTime windowEnd) {
        super(source);
        this.cityId      = cityId;
        this.productType = productType;
        this.newFactor   = newFactor;
        this.windowStart = windowStart;
        this.windowEnd   = windowEnd;
    }

    // ═══════════ Getters (no setters) ═══════════
    public Long getCityId()               { return cityId; }
    public ProductType getProductType()   { return productType; }
    public BigDecimal  getNewFactor()     { return newFactor; }
    public OffsetDateTime getWindowStart(){ return windowStart; }
    public OffsetDateTime getWindowEnd()  { return windowEnd; }
}
