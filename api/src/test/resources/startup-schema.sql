CREATE TABLE IF NOT EXISTS t_order (
    id VARCHAR(64) PRIMARY KEY, uid VARCHAR(64), phone VARCHAR(32), aid VARCHAR(64),
    seats VARCHAR(255), price DOUBLE, status INT, create_at VARCHAR(32), pay_at VARCHAR(32)
);
CREATE TABLE IF NOT EXISTS t_film (
    id VARCHAR(64) PRIMARY KEY, name VARCHAR(64), release_time VARCHAR(32), type VARCHAR(32),
    status BOOLEAN, region VARCHAR(32), introduction VARCHAR(255), hot INT,
    cover VARCHAR(255), duration INT
);
