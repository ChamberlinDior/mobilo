package com.mobility.ride.service;

public interface DriverRideActionService {
    void accept (Long rideId);
    void decline(Long rideId);
}
