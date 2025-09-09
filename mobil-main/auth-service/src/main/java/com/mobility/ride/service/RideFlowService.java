// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/RideFlowService.java
//  v2025-10-11 – ouvre automatiquement la room de chat après ACCEPT
//               + déclenche les notifications push via NotificationService
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.auth.model.WalletTransaction;
import com.mobility.auth.model.enums.PaymentProvider;
import com.mobility.auth.model.enums.WalletTxnType;
import com.mobility.auth.repository.PaymentMethodRepository;
import com.mobility.auth.repository.UserRepository;
import com.mobility.auth.repository.WalletTransactionRepository;
import com.mobility.ride.dto.*;
import com.mobility.ride.mapper.DtoMapper;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
public class RideFlowService {

    /* ─────────────── Dépendances “hard” ─────────────── */
    private final RideRepository              rideRepo;
    private final PaymentService              paymentSvc;
    private final PaymentMethodRepository     pmRepo;
    private final WalletTransactionRepository txnRepo;
    private final UserRepository              userRepo;
    private final SimpMessagingTemplate       ws;

    /* ─────────────── Dépendance “lazy” ─────────────── */
    private final WaitTimeService waitTimeSvc;

    /* ─────────────── Notifications push ─────────────── */
    private final NotificationService notificationService;

    @Autowired
    public RideFlowService(RideRepository              rideRepo,
                           PaymentService              paymentSvc,
                           PaymentMethodRepository     pmRepo,
                           WalletTransactionRepository txnRepo,
                           UserRepository              userRepo,
                           SimpMessagingTemplate       ws,
                           @Lazy WaitTimeService       waitTimeSvc,
                           NotificationService         notificationService) {
        this.rideRepo            = rideRepo;
        this.paymentSvc          = paymentSvc;
        this.pmRepo              = pmRepo;
        this.txnRepo             = txnRepo;
        this.userRepo            = userRepo;
        this.ws                  = ws;
        this.waitTimeSvc         = waitTimeSvc;
        this.notificationService = notificationService;
    }

    // ═════════════════ 1) ACCEPT ═══════════════════════════════════════════
    @Transactional
    public void accept(Long rideId, Long driverId) {

        /* 1. Validation + mise à jour */
        Ride r = fetchRide(rideId);
        if (r.getStatus() != RideStatus.REQUESTED)
            throw new IllegalStateException("Ride already accepted or cancelled");

        r.setStatus    (RideStatus.ACCEPTED);
        r.setDriverId  (driverId);
        r.setAcceptedAt(OffsetDateTime.now());

        /* 2. “Snippets” ultra-légers (Driver + Rider) */
        DriverSnippet driver = userRepo.findDriverSnippetById(driverId)
                .map(DtoMapper::toDriverSnippet)
                .orElseThrow(() ->
                        new EntityNotFoundException("DRIVER_NOT_FOUND id=" + driverId));

        RiderSnippet rider = userRepo.findRiderSnippetById(r.getRiderId())
                .map(DtoMapper::toRiderSnippet)
                .orElseThrow(() ->
                        new EntityNotFoundException("RIDER_NOT_FOUND id=" + r.getRiderId()));

        /* 3. Produit sélectionné (label + icône) */
        ProductSnippet product = new ProductSnippet(
                r.getProductType().name(),
                r.getProductType().getLabel(),
                r.getProductType().getIconUrl()
        );

        /* 4. Construction du payload “match” */
        RideAcceptedPayload payload = new RideAcceptedPayload(
                r.getId(), driver, rider, product, r.getAcceptedAt());

        /* 5-a. Diffusion temps-réel “match” */
        ws.convertAndSend("/topic/ride/" + r.getId(), payload);

        /* 5-b. Ouvre la room de chat (notif front) */
        ws.convertAndSend("/topic/ride/" + r.getId() + "/chat/open",
                Map.of("rideId", r.getId()));

        publishOps(r);  // monitoring interne

        /* 6. Notifications push (rider & driver) */
        notificationService.notifyRideStatus(r);

        log.info("[FLOW] Ride #{} ACCEPTED by driver #{}", r.getId(), driverId);
    }

    // ═════════════════ 2) EN-ROUTE ═════════════════════════════════════════
    @Transactional
    public void startEnRoute(Long rideId) {
        Ride r = fetchRide(rideId);
        if (r.getStatus() != RideStatus.ACCEPTED)
            throw new IllegalStateException("Ride must be ACCEPTED to start en-route");

        r.setStatus   (RideStatus.EN_ROUTE);
        r.setEnRouteAt(OffsetDateTime.now());

        publishOps(r);

        /* Notifications push */
        notificationService.notifyRideStatus(r);
    }

    // ═════════════════ 3) ARRIVED / WAITING ════════════════════════════════
    @Transactional
    public void arrived(Long rideId) {
        Ride r = fetchRide(rideId);
        if (r.getStatus() != RideStatus.EN_ROUTE)
            throw new IllegalStateException("Ride must be EN_ROUTE to mark ARRIVED");

        r.setStatus   (RideStatus.ARRIVED);
        r.setArrivedAt(OffsetDateTime.now());

        waitTimeSvc.startWaitingCountdown(r.getId());
        publishOps(r);

        /* Notifications push */
        notificationService.notifyRideStatus(r);
    }

    // ═════════════════ 4) START RIDE ═══════════════════════════════════════
    @Transactional
    public void startRide(Long rideId) {
        Ride r = fetchRide(rideId);
        if (r.getStatus() != RideStatus.ARRIVED && r.getStatus() != RideStatus.WAITING)
            throw new IllegalStateException("Ride must be ARRIVED / WAITING to start");

        r.setStatus      (RideStatus.IN_PROGRESS);
        r.setPickupRealAt(OffsetDateTime.now());

        waitTimeSvc.stopWaitingCountdown(rideId);
        publishOps(r);

        /* Notifications push */
        notificationService.notifyRideStatus(r);
    }

    // ═════════════════ 5) COMPLETE ═════════════════════════════════════════
    @Transactional
    public void completeRide(Long rideId, double distanceKm, long durationSec) {
        Ride r = fetchRide(rideId);
        if (r.getStatus() != RideStatus.IN_PROGRESS)
            throw new IllegalStateException("Ride must be IN_PROGRESS to complete");

        r.setStatus         (RideStatus.COMPLETED);
        r.setDropoffRealAt  (OffsetDateTime.now());
        r.setDistanceKmReal (distanceKm);
        r.setDurationSecReal(durationSec);

        capturePayment(r);
        publishOps(r);

        /* Notifications push */
        notificationService.notifyRideStatus(r);
    }

    // ═════════════════ 6) CANCEL / NO_SHOW ═════════════════════════════════
    @Transactional
    public void cancelRide(Long rideId, CancellationReason reason) {
        Ride r = fetchRide(rideId);
        if (r.getStatus() == RideStatus.COMPLETED)
            throw new IllegalStateException("Completed ride cannot be cancelled");

        r.setStatus(reason == CancellationReason.NO_SHOW ? RideStatus.NO_SHOW
                : RideStatus.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        r.setCancelReason(reason.name());

        if (reason == CancellationReason.NO_SHOW)
            paymentSvc.captureCancellationFee(r, WaitTimeService.NO_SHOW_FEE, r.getCurrency());

        publishOps(r);

        /* Notifications push */
        notificationService.notifyRideStatus(r);
    }

    /* ───────────────────── Helpers ───────────────────── */
    private Ride fetchRide(Long id) {
        return rideRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));
    }

    /** Flux “minimal” vers la console dispatch / monitoring. */
    private void publishOps(Ride r) {
        ws.convertAndSend("/topic/driver/ops",
                new RideFlowEvent(r.getId(), r.getStatus(), r.getDriverId()));
    }

    /* ───────────────────── Paiement final ───────────────────── */
    private void capturePayment(Ride r) {
        try {
            paymentSvc.captureRideCharge(r);
        } catch (Exception ex) {
            log.error("[PAYMENT] Capture failed ride #{} : {}", r.getId(), ex.getMessage());
        }

        /* Paiement cash → écriture négative dans le wallet du rider */
        if (resolveProvider(r) == PaymentProvider.CASH) {
            User rider = findRider(r);
            txnRepo.save(WalletTransaction.builder()
                    .user(rider)
                    .type(WalletTxnType.CASH_PAYMENT)
                    .amount(r.getTotalFare().negate())
                    .currency(r.getCurrency())
                    .reference("RIDE-" + r.getId())
                    .build());
            log.info("[WALLET] CASH_PAYMENT logged – ride #{}", r.getId());
        }
    }

    private PaymentProvider resolveProvider(Ride r) {
        return r.getPaymentMethodId() == null
                ? PaymentProvider.CASH
                : pmRepo.findById(r.getPaymentMethodId())
                .map(pm -> pm.getProvider())
                .orElse(PaymentProvider.STRIPE);
    }

    private User findRider(Ride r) {
        return userRepo.findByUid(r.getRiderUid())
                .orElseGet(() -> userRepo.findById(r.getRiderId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "RIDER_NOT_FOUND id=" + r.getRiderId())));
    }

    /* ───────────────────── Types internes ───────────────────── */
    public record RideFlowEvent(Long rideId, RideStatus status, Long driverId) {}
    public enum CancellationReason { RIDER, DRIVER, SYSTEM, NO_SHOW }
}
