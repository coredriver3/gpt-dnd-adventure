# GPT Dice Adventure

Angular + Spring Boot monorepo for a dice-driven GPT-style adventure prototype.

## Structure

- `frontend/` - Angular app with componentized game UI and API service layer.
- `backend/` - Spring Boot REST API with PostgreSQL persistence and game rules.
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
- `GET /api/sessions`
- `POST /api/sessions`
- `GET /api/sessions/{id}`
- `POST /api/sessions/{id}/actions`
- `PATCH /api/sessions/{id}/preferences`
- `DELETE /api/sessions/{id}`

## Game master

A language model runs the game as narrator (`OpenRouterStoryProvider` by default with a free model, or `ClaudeStoryProvider`).
Without any key a rule-based local narrator is used. Each action is resolved in two steps:

1. **Plan** – the model decides whether the action simply works, cannot work, or is uncertain enough to
   roll. For a roll it picks skill, difficulty (DC), modifier and what is at stake.
2. **Resolve** – the backend rolls the d20 if needed and adds directions for the turn. The model then
   narrates the outcome and proposes state changes and story state updates.

The story director (`StoryDirector`) keeps the pacing varied: it picks the kind of moment each turn delivers,
advances a hidden threat clock that forces the threat to act, requires a named person by turn 3 and new
surroundings every 5 turns, and limits how often coins can be found. Goal, threat, layout, people and open threads
are stored per game and sent along with every turn. The layout (size of the place, exits, where everyone stands) is
shown in the sidebar and keeps the narration spatially consistent.

A character has health, coins and an inventory. A fatal turn is rewritten by one extra request so that the text tells
the death, and a "success with cost" without a cost from the model costs 1 health.

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
cd backend && ./mvnw test     # uses H2, no Docker needed
cd frontend && npx ng build
```
