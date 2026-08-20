# GPT Dice Adventure

Angular + Spring Boot monorepo for a dice-driven GPT-style adventure prototype.

## Structure

- `frontend/` - Angular app with componentized game UI and API service layer.
- `backend/` - Spring Boot REST API with in-memory sessions and game rules.
- `legacy-static/` - original static prototype kept as reference.

## Run locally

Terminal 1:

```bash
npm run dev:backend
```

Terminal 2:

```bash
npm run dev:frontend
```

Open `http://127.0.0.1:4200/`. Angular proxies `/api` to Spring Boot on port `8080`.

## API

- `GET /api/settings`
- `POST /api/sessions`
- `GET /api/sessions/{id}`
- `POST /api/sessions/{id}/actions`
- `PATCH /api/sessions/{id}/preferences`

The backend currently uses `LocalStoryProvider` for deterministic local narration. `ExternalStoryProvider` is prepared for a future API integration via:

```bash
ADVENTURE_AI_ENDPOINT=...
ADVENTURE_AI_API_KEY=...
```

## Verify

```bash
npm run build
```
