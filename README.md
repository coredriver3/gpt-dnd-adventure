# GPT Dice Adventure

Angular + Spring Boot monorepo for a dice-driven GPT-style adventure prototype.

## Structure

- `frontend/` - Angular app with componentized game UI and API service layer.
- `backend/` - Spring Boot REST API with MariaDB persistence and game rules.
- `legacy-static/` - original static prototype kept as reference.

## Run locally

MariaDB is used by default. Local dev defaults:

```bash
DB_URL=jdbc:mariadb://localhost:3306/gpt_dnd_adventure
DB_USERNAME=gpt_dnd
DB_PASSWORD=gpt_dnd_dev
```

Create the local database once:

```bash
sudo mariadb -e "CREATE DATABASE IF NOT EXISTS gpt_dnd_adventure CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mariadb -e "CREATE USER IF NOT EXISTS 'gpt_dnd'@'localhost' IDENTIFIED BY 'gpt_dnd_dev';"
sudo mariadb -e "GRANT ALL PRIVILEGES ON gpt_dnd_adventure.* TO 'gpt_dnd'@'localhost'; FLUSH PRIVILEGES;"
```

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
