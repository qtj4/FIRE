package kz.enki.fire.ticket_intake_service.repository;

import kz.enki.fire.ticket_intake_service.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByEndpointAndIdempotencyKey(String endpoint, String idempotencyKey);
}
