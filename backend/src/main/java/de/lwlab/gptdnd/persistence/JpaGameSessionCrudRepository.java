package de.lwlab.gptdnd.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaGameSessionCrudRepository extends JpaRepository<GameSessionEntity, String> {
}
