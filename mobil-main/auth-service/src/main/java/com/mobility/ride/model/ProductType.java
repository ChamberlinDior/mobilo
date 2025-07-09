// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/model/ProductType.java
//  v2025-09-04 – libellé + icône pour l’écran « match »
//               (méthodes getLabel / getIconUrl utilisées par RideFlowService)
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.model;

/**
 * Catalogue des « produits » (types de trajets) proposés par l’app.
 *
 * <p>Chaque constante transporte désormais&nbsp;:</p>
 * <ul>
 *   <li><strong>label&nbsp;:</strong> intitulé lisible par l’humain
 *       (affiché sous l’avatar du produit) ;</li>
 *   <li><strong>iconUrl&nbsp;:</strong> chemin statique de l’icône PNG/SVG
 *       embarquée dans les apps.</li>
 * </ul>
 *
 * <p>
 *  Ces deux champs sont requis par <code>RideFlowService</code> pour remplir
 *  le <code>ProductSnippet</code> envoyé à l’écran « match » lorsque le
 *  chauffeur accepte une course.
 * </p>
 */
public enum ProductType {

    /*            label                icône (resources/static/…) */
    X       ("Berline éco",            "/assets/products/x.png"),
    XL      ("Van 6 places",           "/assets/products/xl.png"),
    MOTO    ("Moto-taxi",              "/assets/products/moto.png"),
    BLACK   ("Berline business",       "/assets/products/black.png"),
    COMFORT ("Confort +",              "/assets/products/comfort.png"),
    LUX     ("Luxe",                   "/assets/products/lux.png"),
    POOL    ("Course partagée",        "/assets/products/pool.png"),
    DELIVERY("Livraison colis",        "/assets/products/delivery.png");

    /* ─────────────────── Champs internes ─────────────────── */
    private final String label;
    private final String iconUrl;

    /* ─────────────────── Constructeur ─────────────────── */
    ProductType(String label, String iconUrl) {
        this.label    = label;
        this.iconUrl  = iconUrl;
    }

    /* ─────────────────── Accesseurs publics ─────────────────── */
    /** Libellé marketing (localisable). */
    public String getLabel()   { return label;   }

    /** Chemin/URL de l’icône (PNG ou SVG). */
    public String getIconUrl() { return iconUrl; }
}
