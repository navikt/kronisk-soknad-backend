CREATE TABLE referanse (
    referansenummer serial PRIMARY KEY,
    uuid uuid UNIQUE NOT NULL
);
