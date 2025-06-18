package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>RideParticipationService</h2>
 *
 * <p>
 * Fournit une méthode pour savoir si un utilisateur (passager ou conducteur)
 * est bien lié à une course identifiée par rideId. Utilisé notamment dans les
 * contrôleurs pour sécuriser l’accès aux opérations ride-spécifiques.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RideParticipationService {

    private final RideRepository rideRepository;

    /**
     * Indique si l’utilisateur d’ID interne {@code userId} est participant
     * (soit passenger, soit driver) de la course {@code rideId}.
     *
     * @param rideId ID de la course
     * @param userId ID interne de l’utilisateur
     * @return true si l’utilisateur est le rider ou le driver de la course
     * @throws EntityNotFoundException si la course {@code rideId} n’existe pas
     */
    public boolean isParticipantOfRide(Long rideId, Long userId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        // Vérifie si userId correspond au riderId ou au driverId
        return userId.equals(ride.getRiderId()) || userId.equals(ride.getDriverId());
    }
}
