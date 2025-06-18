// ─────────────────────────────────────────────────────────────────────────────
// SERVICE : TipService
// Gestion des pourboires + split-fare
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.dto.*;
import com.mobility.ride.model.*;
import com.mobility.ride.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository          tipRepo;
    private final PaymentSplitRepository splitRepo;
    private final RideRepository         rideRepo;

    /* ═══════════ Pourboires ═══════════ */
    @Transactional
    public Tip addTip(long rideId, TipRequest req) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        Tip tip = Tip.builder()
                .ride(ride)
                .payerId(req.payerId())
                .amount(req.amount())
                .currency(req.currency())
                .createdAt(OffsetDateTime.now())
                .build();

        tipRepo.save(tip);
        log.info("💸 Tip {} {} added by {} on ride {}", req.amount(), req.currency(),
                req.payerId(), rideId);
        return tip;
    }

    /* ═══════════ Split-fare ═══════════ */
    @Transactional
    public PaymentSplit splitBill(long rideId, SplitBillRequest req) {

        if (req.sharePct() <= 0 || req.sharePct() >= 100) {
            throw new IllegalArgumentException("SHARE_PCT_OUT_OF_RANGE");
        }

        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        PaymentSplit split = PaymentSplit.builder()
                .ride(ride)
                .payerId(req.payerId())
                .sharePct(req.sharePct())
                .createdAt(OffsetDateTime.now())
                .build();

        splitRepo.save(split);
        log.info("🧾 Split-bill {}% for payer {} on ride {}", req.sharePct(),
                req.payerId(), rideId);
        return split;
    }
}
