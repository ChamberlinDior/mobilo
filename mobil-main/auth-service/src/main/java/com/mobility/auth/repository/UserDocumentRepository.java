package com.mobility.auth.repository;

import com.mobility.auth.model.UserDocument;
import com.mobility.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findAllByUser(User user);
}
