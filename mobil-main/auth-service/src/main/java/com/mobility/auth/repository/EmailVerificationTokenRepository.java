// com.mobility.auth.repository.EmailVerificationTokenRepository.java
package com.mobility.auth.repository;

import com.mobility.auth.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUserId(Long userId);
}
