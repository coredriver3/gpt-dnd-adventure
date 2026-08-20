package de.lwlab.gptdnd.api;

import de.lwlab.gptdnd.service.GameService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/settings")
    public List<SettingResponse> settings() {
        return gameService.settings().stream().map(GameMapper::setting).toList();
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GameSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        return GameMapper.session(gameService.create(request));
    }

    @GetMapping("/sessions/{id}")
    public GameSessionResponse get(@PathVariable String id) {
        return GameMapper.session(gameService.get(id));
    }

    @PostMapping("/sessions/{id}/actions")
    public GameSessionResponse act(@PathVariable String id, @Valid @RequestBody ActionRequest request) {
        return GameMapper.session(gameService.act(id, request));
    }

    @PatchMapping("/sessions/{id}/preferences")
    public GameSessionResponse preferences(@PathVariable String id, @RequestBody PreferenceRequest request) {
        return GameMapper.session(gameService.applyPreferences(id, request));
    }
}
