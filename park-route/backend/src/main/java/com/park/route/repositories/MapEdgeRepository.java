package com.park.route.repositories;

import com.park.route.model.MapEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MapEdgeRepository extends JpaRepository<MapEdge, Long> {
    Optional<MapEdge> findByCode(String code);
    boolean existsByCode(String code);
}
