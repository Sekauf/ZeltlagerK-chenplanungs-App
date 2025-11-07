PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS categories (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL UNIQUE CHECK (trim(name) <> ''),
    description   TEXT
);

CREATE TABLE IF NOT EXISTS recipes (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT    NOT NULL CHECK (trim(name) <> ''),
    category_id        INTEGER,
    base_servings      INTEGER NOT NULL DEFAULT 1 CHECK (base_servings > 0),
    instructions       TEXT    NOT NULL CHECK (trim(instructions) <> ''),
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
        CHECK (created_at LIKE '%Z'),
    updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
        CHECK (updated_at LIKE '%Z'),
    FOREIGN KEY (category_id) REFERENCES categories(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TRIGGER IF NOT EXISTS recipes_updated_at
AFTER UPDATE ON recipes
FOR EACH ROW
WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE recipes
    SET updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now')
    WHERE id = NEW.id AND updated_at = OLD.updated_at;
END;

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id           INTEGER NOT NULL,
    name                TEXT    NOT NULL CHECK (trim(name) <> ''),
    unit                TEXT    NOT NULL CHECK (trim(unit) <> ''),
    amount_per_serving  REAL    NOT NULL CHECK (amount_per_serving > 0),
    notes               TEXT,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe
    ON recipe_ingredients (recipe_id);

CREATE INDEX IF NOT EXISTS idx_recipes_category
    ON recipes (category_id);
