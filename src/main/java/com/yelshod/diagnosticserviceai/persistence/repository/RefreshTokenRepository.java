package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.RefreshTokenEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findAllByUser(UserEntity user);
}
