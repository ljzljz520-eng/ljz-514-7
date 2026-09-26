package com.park.route.repositories;

import com.park.route.model.MapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MapNodeRepository extends JpaRepository<MapNode, Long> {
    Optional<MapNode> findByCode(String code);
    boolean existsByCode(String code);
}
