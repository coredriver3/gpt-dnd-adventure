package de.lwlab.gptdnd.store;

import de.lwlab.gptdnd.domain.GameSession;
import de.lwlab.gptdnd.persistence.GameSessionEntity;
import de.lwlab.gptdnd.persistence.JpaGameSessionCrudRepository;
import java.util.Collection;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GameSessionRepository {
    private final JpaGameSessionCrudRepository repository;

    public GameSessionRepository(JpaGameSessionCrudRepository repository) {
        this.repository = repository;
    }

    public GameSession save(GameSession session) {
        return repository.save(GameSessionEntity.fromDomain(session)).toDomain();
    }

    public Optional<GameSession> findById(String id) {
        return repository.findById(id).map(GameSessionEntity::toDomain);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public Collection<GameSession> findAll() {
        return repository.findAll().stream().map(GameSessionEntity::toDomain).toList();
    }
}
