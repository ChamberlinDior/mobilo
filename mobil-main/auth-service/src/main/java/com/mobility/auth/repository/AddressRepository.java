package com.mobility.auth.repository;

import com.mobility.auth.model.Address;
import com.mobility.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByUser(User user);
    void deleteByIdAndUser(Long id, User user);
}
