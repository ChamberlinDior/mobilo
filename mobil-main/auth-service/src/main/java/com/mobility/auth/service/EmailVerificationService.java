// com.mobility.auth.service.EmailVerificationService.java
package com.mobility.auth.service;

import com.mobility.auth.model.EmailVerificationToken;
import com.mobility.auth.model.User;
import com.mobility.auth.repository.EmailVerificationTokenRepository;
import com.mobility.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    /**
     * Génère un token, le stocke et envoie le mail de vérification.
     */
    @Transactional
    public void createAndSendToken(User user) {
        // 1) supprime l'ancien éventuel
        tokenRepo.deleteByUserId(user.getId());

        // 2) crée et persiste un nouveau token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken ev = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .build();
        tokenRepo.save(ev);

        // 3) envoie le mail
        String link = String.format(
                "https://api.mobility.example.com/api/v1/auth/verify-email?token=%s",
                token
        );
        String body = String.join("\n",
                "Bonjour " + user.getFirstName() + ",",
                "",
                "Merci de vous être inscrit sur Mobility.",
                "Cliquez ici pour confirmer votre adresse e-mail :",
                link,
                "",
                "Ce lien expire dans 24 h.",
                "",
                "— L’équipe Mobility"
        );
        emailService.sendSimpleMessage(
                user.getEmail(),
                "Vérification de votre adresse e-mail",
                body
        );
    }

    /**
     * Vérifie le token, marque emailVerified=true et supprime le token.
     */
    @Transactional
    public void verifyToken(String token) {
        EmailVerificationToken ev = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_VERIFICATION_TOKEN"));

        if (ev.isExpired()) {
            throw new IllegalArgumentException("VERIFICATION_TOKEN_EXPIRED");
        }

        User user = ev.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);

        tokenRepo.delete(ev);
    }
}
