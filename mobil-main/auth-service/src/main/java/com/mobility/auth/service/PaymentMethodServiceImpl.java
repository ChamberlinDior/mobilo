// src/main/java/com/mobility/auth/service/PaymentMethodServiceImpl.java
package com.mobility.auth.service;

import com.mobility.auth.dto.PaymentMethodRequest;
import com.mobility.auth.dto.PaymentMethodResponse;
import com.mobility.auth.mapper.PaymentMethodMapper;
import com.mobility.auth.model.PaymentMethod;
import com.mobility.auth.model.enums.PaymentProvider;
import com.mobility.auth.repository.PaymentMethodRepository;
import com.mobility.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final UserRepository          userRepo;
    private final PaymentMethodRepository repo;
    private final PaymentMethodMapper     mapper;

    /* ══════════════ LECTURE ══════════════ */

    @Override
    public List<PaymentMethodResponse> list(String userUid) {
        var user = userRepo.findByExternalUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return repo.findAllByUser(user).stream()
                .map(mapper::toDto)
                .toList();
    }

    /* ══════════════ CRÉATION ═════════════ */

    @Override
    @Transactional
    public PaymentMethodResponse add(String userUid, PaymentMethodRequest req) {

        var user = userRepo.findByExternalUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        /* 1) Provider déjà typé -------------------------------------------------- */
        final PaymentProvider provider = req.getProvider();               // ✱ MOD
        if (provider == null) {
            throw new ResponseStatusException(BAD_REQUEST, "provider manquant");
        }

        /* 2) Règles métier selon le provider ------------------------------------ */
        switch (provider) {

            case CASH -> {
                // Un seul moyen CASH par utilisateur
                if (repo.existsByUserAndProvider(user, PaymentProvider.CASH)) {  // ✱ MOD
                    throw new ResponseStatusException(CONFLICT, "Un moyen CASH existe déjà");
                }
                // On force quelques champs pour la cohérence
                req.setToken(null);
                req.setBrand("CASH");
                req.setLast4(null);
                req.setExpMonth(null);
                req.setExpYear(null);
            }

            case AIRTEL_MONEY_GA -> {
                if (StringUtils.isBlank(req.getToken())) {
                    throw new ResponseStatusException(BAD_REQUEST, "MSISDN Airtel (token) manquant");
                }
                req.setBrand("AIRTEL");
                req.setLast4(StringUtils.right(req.getToken(), 4));
            }

            case APPLE_PAY, PAYPAL, STRIPE -> {
                if (StringUtils.isBlank(req.getToken())) {
                    throw new ResponseStatusException(BAD_REQUEST, "token obligatoire pour " + provider);
                }
            }
        }

        /* 3) Vérification unicité token (hors CASH) ----------------------------- */
        if (provider != PaymentProvider.CASH &&
                repo.existsByUserAndToken(user, req.getToken())) {
            throw new ResponseStatusException(CONFLICT, "Ce moyen de paiement existe déjà");
        }

        /* 4) Mapping / persistance --------------------------------------------- */
        PaymentMethod entity = mapper.toEntity(req, user);

        // makeDefault : basculer les anciens
        if (Boolean.TRUE.equals(req.getMakeDefault())) {
            repo.findAllByUser(user).forEach(pm -> pm.setDefaultMethod(false));
            entity.setDefaultMethod(true);
        }

        repo.save(entity);
        return mapper.toDto(entity);
    }

    /* ══════════════ SUPPRESSION ═════════════ */

    @Override
    @Transactional
    public void delete(String userUid, Long methodId) {
        var user = userRepo.findByExternalUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        repo.deleteByIdAndUser(methodId, user);
    }

    /* ══════ DÉFINITION/CHANGEMENT DU DEFAULT ══════ */

    @Override
    @Transactional
    public PaymentMethodResponse setDefault(String userUid, Long methodId) {
        var user = userRepo.findByExternalUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        repo.findAllByUser(user).forEach(pm ->
                pm.setDefaultMethod(pm.getId().equals(methodId)));

        return repo.findById(methodId)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
    }
}
