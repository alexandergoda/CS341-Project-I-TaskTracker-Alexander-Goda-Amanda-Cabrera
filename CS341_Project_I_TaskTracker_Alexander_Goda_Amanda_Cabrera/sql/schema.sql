PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    task_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    creator_id INTEGER NOT NULL,
    assignee_id INTEGER,
    due_date TEXT,
    priority TEXT NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    completed INTEGER NOT NULL DEFAULT 0
        CHECK (completed IN (0,1)),
    FOREIGN KEY (creator_id) REFERENCES users(user_id),
    FOREIGN KEY (assignee_id) REFERENCES users(user_id)
);
