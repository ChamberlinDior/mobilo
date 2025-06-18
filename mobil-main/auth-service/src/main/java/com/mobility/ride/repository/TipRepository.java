// ─────────────────────────────────────────────────────────────────────────────
// TipRepository.java
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {

    List<Tip> findAllByRideId(Long rideId);
}
