// ─────────────────────────────────────────────────────────────────────────────
//  PACKAGE : com.mobility.ride.event
//  FILE    : SafetyIncidentEvent.java
// ----------------------------------------------------------------------------
package com.mobility.ride.event;

import com.mobility.ride.model.SafetyIncident;
import org.springframework.context.ApplicationEvent;

/**
 * Émis lorsqu’un incident sécurité est enregistré (SOS, PIN mismatch, etc.).
 */
public class SafetyIncidentEvent extends ApplicationEvent {

    public SafetyIncidentEvent(SafetyIncident incident) {
        super(incident);
    }

    @Override
    public SafetyIncident getSource() {          // typage plus pratique
        return (SafetyIncident) super.getSource();
    }
}
