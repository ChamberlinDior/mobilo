// ============================
// src/main/java/com/mobility/ride/model/RideStatus.java
// ============================
package com.mobility.ride.model;

/**
 * 🔄  Cycle de vie complet d’une course / livraison.
 *
 * <pre>
 * REQUESTED   ─┐
 *             ▼
 *  ACCEPTED   →  EN_ROUTE  →  ARRIVED  →  WAITING  →  IN_PROGRESS  →  COMPLETED
 *                    │                           ↘
 *                    └─────────────>  CANCELLED / NO_SHOW
 * </pre>
 *
 * • {@code SCHEDULED} est un état « hors flux temps réel » réservé aux
 *   réservations futures (T > now).
 * • {@code WAITING} démarre quand le chauffeur déclenche « Arrived » ;
 *   la grâce (ex. 2 min) est gérée par WaitTimeService.
 * • {@code NO_SHOW} est posé automatiquement si le rider ne se présente
 *   pas avant la fin de la grâce.
 */
public enum RideStatus {

    /** Demande créée, aucun chauffeur encore attribué. */
    REQUESTED,

    /** Chauffeur a accepté la demande (verrouillé). */
    ACCEPTED,

    /** Chauffeur en route vers le point de prise en charge. */
    EN_ROUTE,

    /** Chauffeur arrivé au pickup (pas encore de passager). */
    ARRIVED,

    /** Attente payante après la période de grâce. */
    WAITING,

    /** Course planifiée pour une date future. */
    SCHEDULED,

    /** Passager à bord / colis pris en charge. */
    IN_PROGRESS,

    /** Course terminée et payée. */
    COMPLETED,

    /** Annulée (par rider, driver ou système) avant complétion. */
    CANCELLED,

    /** Annulation automatique pour « no-show ». */
    NO_SHOW
}
