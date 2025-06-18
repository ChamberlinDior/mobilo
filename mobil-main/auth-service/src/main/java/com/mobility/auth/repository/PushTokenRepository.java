package com.mobility.auth.repository;

import com.mobility.auth.model.PushToken;
import com.mobility.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    List<PushToken> findAllByUser(User user);
    void deleteByTokenAndUser(String token, User user);
}
