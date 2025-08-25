package com.mobility.ride.service;
public interface CurrencyResolver {
    String resolve(Double pickupLat, Double pickupLng, Long fallbackCityId);
}
