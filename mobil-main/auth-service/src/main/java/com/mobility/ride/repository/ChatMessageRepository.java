// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE : com.mobility.ride.repository
// ----------------------------------------------------------------------------
package com.mobility.ride.repository;

import com.mobility.ride.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByRideIdOrderByCreatedAtAsc(Long rideId);
}
