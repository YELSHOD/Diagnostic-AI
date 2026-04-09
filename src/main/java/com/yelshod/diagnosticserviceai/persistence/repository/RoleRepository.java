package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);
}
