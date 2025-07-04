package com.mobility.ride.service;

import com.mobility.ride.service.DriverRideActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverRideActionServiceImpl implements DriverRideActionService {

    // private final RideLifecycleService rideLifecycler; // votre service métier

    @Override
    public void accept(Long rideId) {
        // rideLifecycler.driverAccept(rideId);
    }

    @Override
    public void decline(Long rideId) {
        // rideLifecycler.driverDecline(rideId);
    }
}
