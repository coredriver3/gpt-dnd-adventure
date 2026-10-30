# GPT Dice Adventure

Angular + Spring Boot monorepo for a dice-driven GPT-style adventure prototype.

## Structure

- `frontend/` - Angular app with componentized game UI and API service layer.
- `backend/` - Spring Boot REST API with MariaDB persistence and game rules.
- `legacy-static/` - original static prototype kept as reference.

## Run locally

The database is PostgreSQL in Docker, defined in `backend/compose.yaml`. Docker must be running;
Spring Boot's Docker Compose support starts the container with the backend, connects to it, and stops
it on shutdown. Data persists in the `postgres-data` volume.

| Setting | Value |
| --- | --- |
| Host | `localhost:5432` |
| Database | `gpt_dnd_adventure` |
| User / password | `gpt_dnd` / `gpt_dnd_dev` |

Machine-specific overrides go into `backend/application-local.properties` (git-ignored,
loaded automatically when present), for example API keys. To use an external database instead of the
container, set `spring.docker.compose.enabled=false` and the `spring.datasource.*` properties there.

Terminal 1:

```bash
npm run dev:backend
```

Terminal 2:

```bash
npm run dev:frontend
```

Open `http://127.0.0.1:4200/`. Angular proxies `/api` to Spring Boot on port `8080`.

Do not run `frontend/src/index.html` with the IDE's built-in web server. Angular's
development server compiles the application and provides the `/api` proxy; the raw
HTML entry point does neither.

## API

- `GET /api/settings`
- `POST /api/sessions`
- `GET /api/sessions/{id}`
- `POST /api/sessions/{id}/actions`
- `PATCH /api/sessions/{id}/preferences`

## Game master

Claude (`ClaudeStoryProvider`) runs the game as narrator. Each action is resolved in two steps:

1. **Plan** – Claude checks whether the action fits the scene, whether it needs a roll, and picks
   skill, difficulty (DC) and a modifier.
2. **Resolve** – the backend rolls the d20 and determines the degree of success. Claude then narrates
   the outcome and proposes state changes (health, resolve, resources, items, effects).

The backend stays the rules authority: it rolls the dice and clamps every proposed change to what the
rolled degree allows, so the model cannot hand out free wins or kill a character after a success.

Set an API key to enable Claude, either as an environment variable or in `backend/application-local.properties`
(`adventure.ai.api-key=...`):

```bash
ANTHROPIC_API_KEY=sk-ant-...
```

Alternatively, any model on [OpenRouter](https://openrouter.ai) can narrate (`OpenRouterStoryProvider`),
including free ones. Set `adventure.openrouter.api-key=...` in `backend/application-local.properties`
or export `OPENROUTER_API_KEY`. Free models have tight daily limits, and each action uses two requests.

| Property | Default | Purpose |
| --- | --- | --- |
| `adventure.story.provider` | `auto` | `claude`, `openrouter`, `local`, or `auto` (first provider with a key) |
| `adventure.ai.model` | `claude-opus-5-5` | Claude model id |
| `adventure.openrouter.model` | `nvidia/nemotron-3.5-super-vl:free` | OpenRouter model id |
| `adventure.ai.history-nodes` | `40` | story entries sent as context |

Without any key the backend falls back to `LocalStoryProvider`, a rule-based offline narrator.

## Verify

```bash
npm run build
```
