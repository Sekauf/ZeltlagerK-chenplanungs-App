PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS categories (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL UNIQUE,
    description   TEXT
);

CREATE TABLE IF NOT EXISTS recipes (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT    NOT NULL,
    category_id        INTEGER,
    base_servings      INTEGER NOT NULL DEFAULT 1,
    instructions       TEXT    NOT NULL,
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    FOREIGN KEY (category_id) REFERENCES categories(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TRIGGER IF NOT EXISTS recipes_updated_at
AFTER UPDATE ON recipes
BEGIN
    UPDATE recipes
    SET updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now')
    WHERE id = NEW.id;
END;

CREATE TABLE IF NOT EXISTS ingredients (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id           INTEGER NOT NULL,
    name                TEXT    NOT NULL,
    unit                TEXT    NOT NULL,
    amount_per_serving  REAL    NOT NULL,
    notes               TEXT,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ingredients_recipe
    ON ingredients (recipe_id);

CREATE INDEX IF NOT EXISTS idx_recipes_category
    ON recipes (category_id);
