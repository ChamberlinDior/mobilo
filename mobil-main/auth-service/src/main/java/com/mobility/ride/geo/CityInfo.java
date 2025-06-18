// ============================================================================
//  FILE    : CityInfo.java
//  PACKAGE : com.mobility.ride.geo
// ----------------------------------------------------------------------------
package com.mobility.ride.geo;

/**
 * DTO renvoyé par GeoLocationService, contenant
 * - l'ID interne de la ville/marché (pour PricingProperties)
 * - la devise ISO associée.
 */
public record CityInfo(Long cityId, String currency) {}
