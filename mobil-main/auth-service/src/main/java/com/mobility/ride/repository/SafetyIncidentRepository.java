// ─────────────────────────────────────────────────────────────────────────────
// SafetyIncidentRepository.java
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyIncidentRepository
        extends JpaRepository<SafetyIncident, Long> {

    List<SafetyIncident> findAllByRideId(Long rideId);

    long countByRideIdAndType(Long rideId, SafetyIncidentType type);
}
