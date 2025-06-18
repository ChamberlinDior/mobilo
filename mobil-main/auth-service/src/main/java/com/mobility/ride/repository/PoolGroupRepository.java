// ─────────────────────────────────────────────────────────────────────────────
// PoolGroupRepository.java
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoolGroupRepository extends JpaRepository<PoolGroup, Long> {

    /* Groupes en cours de formation                                      */
    List<PoolGroup> findAllByStatus(PoolGroupStatus status);

    /* Verrou pessimiste : réserve un pool pour assignation atomique      */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pg from PoolGroup pg where pg.id = :id")
    Optional<PoolGroup> lockById(@Param("id") Long id);
}
