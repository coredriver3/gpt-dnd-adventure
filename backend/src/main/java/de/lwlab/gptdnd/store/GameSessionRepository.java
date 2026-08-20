package de.lwlab.gptdnd.store;

import de.lwlab.gptdnd.domain.GameSession;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class GameSessionRepository {
    private final ConcurrentMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession save(GameSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    public Optional<GameSession> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public Collection<GameSession> findAll() {
        return sessions.values();
    }
}
