package com.mobility.ride.scheduler;

import com.mobility.ride.model.SurgeMultiplier;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.repository.SurgeMultiplierRepository;
import com.mobility.ride.service.SurgePricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>SurgeRefreshJob – préchauffage périodique du cache “surge”</h2>
 *
 * <p>
 *   À intervalles réguliers, parcourt toutes les combinaisons de
 *   (cityId, productType) pour appeler {@link SurgePricingService#warmUpCache},
 *   garantissant que les clés du cache Caffeine restent chaudes.
 * </p>
 *
 * <p><strong>Contexte :</strong>
 *   – TTL du cache = 30 s (expireAfterWrite)<br>
 *   – Cette tâche s’exécute toutes les 5 minutes pour rafraîchir le cache.
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SurgeRefreshJob {

    /**
     * Service gérant l’appel à {@code warmUpCache(cityId, productType)}.
     */
    private final SurgePricingService surgeService;

    /**
     * Repository permettant de récupérer les multiplicateurs de tous les
     * cityId existants en base (au moins un enreg. historique pour chaque ville).
     */
    private final SurgeMultiplierRepository surgeRepo;

    /**
     * Intervalle : toutes les 5 minutes (300 000 ms).
     *
     * <p>
     *   – La tâche parcourt d’abord tous les cityId distincts trouvés dans {@code surge_multipliers} ;
     *   – Pour chaque cityId, elle itère sur l’ensemble des {@link ProductType#values()} ;
     *   – Appelle {@link SurgePricingService#warmUpCache(Long, ProductType)} pour forcer
     *     la mise en cache des dernières données.
     * </p>
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 10_000)
    public void refreshAllSurgeCaches() {
        // 1) Récupérer la liste de tous les cityId distincts pour lesquels on a au moins un multiplicateur en base
        List<Long> cityIds = fetchDistinctCityIds();

        if (cityIds.isEmpty()) {
            log.debug("⚡ SurgeRefreshJob – aucun cityId trouvé dans surge_multipliers, arrêt du rafraîchissement");
            return;
        }

        // 2) Pour chaque ville, itérer sur tous les types de produit
        for (Long cityId : cityIds) {
            for (ProductType productType : ProductType.values()) {
                try {
                    surgeService.warmUpCache(cityId, productType);
                } catch (Exception ex) {
                    log.error("⚡ SurgeRefreshJob – erreur lors du warmUpCache pour cityId={} / productType={} : {}",
                            cityId, productType, ex.getMessage(), ex);
                }
            }
        }

        log.info("⚡ SurgeRefreshJob – cache warm-up terminé pour {} villes et {} produits",
                cityIds.size(), ProductType.values().length);
    }

    /**
     * Extrait tous les cityId distincts à partir de la table {@code surge_multipliers}.
     *
     * <p>
     *   Pour éviter un chargement complet inutile, on extrait d’abord toutes les entités,
     *   puis on déduit la liste de cityId distincts via un flux Java 8.
     *   Idéalement, on aurait un finder dédié {@code findDistinctCityIds()} dans le repository.
     * </p>
     *
     * @return liste non triée de tous les cityId distincts ayant au moins un multiplicateur enregistré
     */
    private List<Long> fetchDistinctCityIds() {
        // Chargement de l’ensemble des multiplicateurs (taille raisonnable : max ≈ 500 villes × 4 produits)
        List<SurgeMultiplier> allMultipliers = surgeRepo.findAll();

        // Extraction et déduplication des cityId
        Set<Long> distinctIds = allMultipliers.stream()
                .map(SurgeMultiplier::getCityId)
                .collect(Collectors.toSet());

        return List.copyOf(distinctIds);
    }
}
