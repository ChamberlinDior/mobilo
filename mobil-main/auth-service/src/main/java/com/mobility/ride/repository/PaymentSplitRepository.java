// ─────────────────────────────────────────────────────────────────────────────
// PaymentSplitRepository.java
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.PaymentSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {

    List<PaymentSplit> findAllByRideId(Long rideId);
}
