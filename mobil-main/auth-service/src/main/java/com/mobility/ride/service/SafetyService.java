// ─────────────────────────────────────────────────────────────────────────────
//  SERVICE : SafetyService
//  Gestion SOS, incidents, PIN de prise en charge
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.dto.SOSRequest;
import com.mobility.ride.event.SafetyIncidentEvent;
import com.mobility.ride.model.*;
import com.mobility.ride.repository.RideRepository;
import com.mobility.ride.repository.SafetyIncidentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyService {

    private final RideRepository              rideRepo;
    private final SafetyIncidentRepository    incidentRepo;
    private final ApplicationEventPublisher   events;

    /* ═══════════ SOS ═══════════ */

    /**
     * Surcharge utilitaire pour le controller : accepte directement le DTO.
     */
    @Transactional
    public void triggerSos(SOSRequest req) {
        triggerSos(req.rideId(), req.userId(), req.location());
    }

    /**
     * Version « bas niveau » : utile si vous déclenchez le SOS ailleurs.
     */
    @Transactional
    public void triggerSos(Long rideId, Long userId, LatLng location) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        SafetyIncident incident = SafetyIncident.builder()
                .ride(ride)
                .userId(userId)
                .type(SafetyIncidentType.SOS)
                .location(location)
                .build();

        incidentRepo.save(incident);
        log.warn("🆘  SOS triggered for ride={} by user={}", rideId, userId);

        // Propagation interne (notifications push, centre d’appel, etc.)
        events.publishEvent(new SafetyIncidentEvent(incident));
    }

    /* ═══════════ Vérification PIN ═══════════ */

    @Transactional(readOnly = true)
    public void verifyPin(Long rideId, String pin) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        if (!pin.equals(ride.getSafetyPin())) {
            log.error("❌ PIN mismatch ride={} expected={} given={}",
                    rideId, ride.getSafetyPin(), pin);

            // Journalise l’incident
            SafetyIncident incident = SafetyIncident.builder()
                    .ride(ride)
                    .userId(null)                       // inconnu / n/a
                    .type(SafetyIncidentType.PIN_MISMATCH)
                    .build();
            incidentRepo.save(incident);
            events.publishEvent(new SafetyIncidentEvent(incident));

            throw new IllegalArgumentException("PIN_INVALID");
        }

        log.info("✅ PIN verified ride={}", rideId);
    }
}
