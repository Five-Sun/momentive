CREATE TABLE pet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    birth_date DATE,
    gender VARCHAR(10),
    weight_kg DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pet_user_id ON pet(user_id);
