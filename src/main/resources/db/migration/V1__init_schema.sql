-- =========================================================
-- INIT : schéma consolidé (équivalent à l'ancien V1 -> V13)
-- =========================================================

-- USERS
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL,
                       password      VARCHAR(255),
                       first_name    VARCHAR(100),
                       last_name     VARCHAR(100),
                       phone_number  VARCHAR(50),
                       avatar_url    VARCHAR(255),
                       role          VARCHAR(255),
                       status        VARCHAR(255) NOT NULL,
                       deleted_at    TIMESTAMP,
                       created_at    TIMESTAMP NOT NULL,
                       updated_at    TIMESTAMP NOT NULL,
    -- Compte admin bootstrap géré via ADMIN_EMAIL / ADMIN_PASSWORD
                       env_managed   BOOLEAN NOT NULL DEFAULT FALSE,
                       avatar_id     BIGINT,

                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
                       CONSTRAINT users_status_check CHECK (
                           status IN ('PENDING_VERIFICATION', 'ACTIVE', 'BLOCKED', 'DELETED')
                           )
);

-- MEDIA
CREATE TABLE media (
                       id          BIGSERIAL PRIMARY KEY,
                       public_id   VARCHAR(200) NOT NULL,
                       url         VARCHAR(500) NOT NULL,
                       type        VARCHAR(10)  NOT NULL,
                       owner_id    BIGINT       NOT NULL,
                       created_at  TIMESTAMP    NOT NULL,

                       CONSTRAINT uk_media_public_id UNIQUE (public_id),
                       CONSTRAINT fk_media_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_media_owner ON media(owner_id);
CREATE INDEX idx_media_created_at ON media(created_at);

-- users.avatar_id -> media (ajouté après media pour éviter la dépendance circulaire)
ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar FOREIGN KEY (avatar_id) REFERENCES media(id);

CREATE INDEX idx_users_avatar ON users(avatar_id);

-- ADDRESSES
CREATE TABLE addresses (
                           id           BIGSERIAL PRIMARY KEY,
                           street       VARCHAR(255) NOT NULL,
                           postal_code  VARCHAR(20)  NOT NULL,
                           city         VARCHAR(255) NOT NULL,
                           country      VARCHAR(255) NOT NULL,
                           is_default   BOOLEAN      NOT NULL,
                           user_id      BIGINT       NOT NULL,
                           created_at   TIMESTAMP    NOT NULL,
                           updated_at   TIMESTAMP    NOT NULL,
                           latitude     DOUBLE PRECISION,
                           longitude    DOUBLE PRECISION,

                           CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- POSTS
CREATE TABLE posts (
                       id          BIGSERIAL PRIMARY KEY,
                       content     TEXT,
                       image_url   VARCHAR(200),
                       author_id   BIGINT    NOT NULL,
                       image_id    BIGINT,
                       video_id    BIGINT,
                       created_at  TIMESTAMP NOT NULL,
                       updated_at  TIMESTAMP NOT NULL,

                       CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id),
                       CONSTRAINT fk_posts_image  FOREIGN KEY (image_id)  REFERENCES media(id),
                       CONSTRAINT fk_posts_video  FOREIGN KEY (video_id)  REFERENCES media(id)
);

CREATE INDEX idx_posts_image ON posts(image_id);
CREATE INDEX idx_posts_video ON posts(video_id);

-- COMMENTS
CREATE TABLE comments (
                          id          BIGSERIAL PRIMARY KEY,
                          content     VARCHAR(2000) NOT NULL,
                          author_id   BIGINT        NOT NULL,
                          post_id     BIGINT        NOT NULL,
                          created_at  TIMESTAMP     NOT NULL,
                          updated_at  TIMESTAMP     NOT NULL,

                          CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
                          CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)   REFERENCES posts(id)
);

-- POST LIKES
CREATE TABLE post_likes (
                            id          BIGSERIAL PRIMARY KEY,
                            user_id     BIGINT    NOT NULL,
                            post_id     BIGINT    NOT NULL,
                            created_at  TIMESTAMP NOT NULL,

                            CONSTRAINT uk_post_likes_user_post UNIQUE (user_id, post_id),
                            CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id)
);

-- POST FAVORITES
CREATE TABLE post_favorites (
                                id          BIGSERIAL PRIMARY KEY,
                                user_id     BIGINT    NOT NULL,
                                post_id     BIGINT    NOT NULL,
                                created_at  TIMESTAMP NOT NULL,

                                CONSTRAINT uk_post_favorites_user_post UNIQUE (user_id, post_id),
                                CONSTRAINT fk_post_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
                                CONSTRAINT fk_post_favorites_post FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE INDEX idx_post_favorites_user_id ON post_favorites(user_id);
CREATE INDEX idx_post_favorites_post_id ON post_favorites(post_id);

-- REFRESH TOKENS
CREATE TABLE refresh_tokens (
                                id          VARCHAR(36)  NOT NULL,
                                token_hash  VARCHAR(255) NOT NULL,
                                user_id     VARCHAR(255) NOT NULL,
                                expires_at  TIMESTAMP    NOT NULL,
                                revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at  TIMESTAMP    NOT NULL,
                                user_agent  VARCHAR(512),
                                ip_address  VARCHAR(45),
                                revoked_at  TIMESTAMP,

                                CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
                                CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- EMAIL TOKENS
CREATE TABLE email_tokens (
                              id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                              token_hash  VARCHAR(255) NOT NULL,
                              type        VARCHAR(30)  NOT NULL,
                              user_id     BIGINT       NOT NULL,
                              expires_at  TIMESTAMP    NOT NULL,
                              used_at     TIMESTAMP,
                              created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT uk_email_tokens_token UNIQUE (token_hash),
                              CONSTRAINT fk_email_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_tokens_user_id ON email_tokens (user_id);

-- SPRING SESSION
CREATE TABLE spring_session (
                                primary_id             CHAR(36) NOT NULL,
                                session_id             CHAR(36) NOT NULL,
                                creation_time          BIGINT   NOT NULL,
                                last_access_time       BIGINT   NOT NULL,
                                max_inactive_interval  INT      NOT NULL,
                                expiry_time            BIGINT   NOT NULL,
                                principal_name         VARCHAR(100),

                                CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
                                           session_primary_id  CHAR(36)     NOT NULL,
                                           attribute_name      VARCHAR(200) NOT NULL,
                                           attribute_bytes     BYTEA        NOT NULL,

                                           CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
                                           CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
                                               REFERENCES spring_session(primary_id) ON DELETE CASCADE
);