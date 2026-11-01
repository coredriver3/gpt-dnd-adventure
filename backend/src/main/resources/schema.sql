-- Hibernate's ddl-auto=update never drops columns, and these two are NOT NULL in games from before resolve was removed.
ALTER TABLE IF EXISTS game_sessions DROP COLUMN IF EXISTS resolve;
ALTER TABLE IF EXISTS game_sessions DROP COLUMN IF EXISTS max_resolve;
