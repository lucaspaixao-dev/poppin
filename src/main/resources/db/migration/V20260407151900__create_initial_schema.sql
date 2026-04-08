CREATE TABLE users
(
    id               VARCHAR(36)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    username         VARCHAR(30)  NOT NULL,
    gender           VARCHAR(10)  NOT NULL,
    birthdate        DATE         NOT NULL,
    bio              TEXT,
    social_name      VARCHAR(30),
    profile_photo    TEXT,
    location_city    VARCHAR(100),
    location_country VARCHAR(100),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    registered_at    TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE user_social_medias
(
    user_id  VARCHAR(36) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    url      TEXT        NOT NULL,

    CONSTRAINT fk_user_social_medias_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
