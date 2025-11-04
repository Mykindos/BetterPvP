CREATE OR REPLACE FUNCTION set_updated_time_millis()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_time = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS shopitems
(
    id          SERIAL PRIMARY KEY,
    shopkeeper  VARCHAR(255) NOT NULL,
    material    VARCHAR(255) NOT NULL,
    item_name   VARCHAR(255) NULL,
    model_data  INT NULL,
    menu_slot   INT NOT NULL,
    menu_page   INT NULL,
    amount      INT NOT NULL,
    buy_price   INT NOT NULL,
    sell_price  INT NOT NULL,
    CONSTRAINT shopitems_shopkeeper_material_itemname_uk
        UNIQUE (shopkeeper, material, item_name)
);

CREATE TABLE IF NOT EXISTS shopitems_dynamic_pricing
(
    shop_item_id    INTEGER NOT NULL REFERENCES shopitems (id) ON DELETE CASCADE,
    realm           SMALLINT NOT NULL,
    min_sell_price  INTEGER NOT NULL,
    base_sell_price INTEGER NOT NULL,
    max_sell_price  INTEGER NOT NULL,
    min_buy_price   INTEGER NOT NULL,
    base_buy_price  INTEGER NOT NULL,
    max_buy_price   INTEGER NOT NULL,
    base_stock      INTEGER NOT NULL,
    max_stock       INTEGER NOT NULL,
    current_stock   INTEGER NOT NULL,
    created_time    BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_time    BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT shopitems_dynamic_pricing_uk
        UNIQUE (shop_item_id, realm),
    CHECK (min_sell_price <= base_sell_price),
    CHECK (base_sell_price <= max_sell_price),
    CHECK (min_buy_price <= base_buy_price),
    CHECK (base_buy_price <= max_buy_price)
);

CREATE TRIGGER shopitems_dynamic_pricing_update_trigger
    BEFORE UPDATE ON shopitems_dynamic_pricing
    FOR EACH ROW
EXECUTE FUNCTION set_updated_time_millis();

CREATE TABLE IF NOT EXISTS shopitems_flags
(
    id               SERIAL PRIMARY KEY,
    shop_item_id     INT NOT NULL REFERENCES shopitems (id) ON DELETE CASCADE,
    persistent_key   VARCHAR(255) NOT NULL,
    persistent_value VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS auctions
(
    id           BIGINT PRIMARY KEY,
    realm        INTEGER NOT NULL,
    client       BIGINT NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    item_name    TEXT NOT NULL,
    item         TEXT NOT NULL,
    price        INTEGER NOT NULL,
    expiry       BIGINT NOT NULL,
    sold         BOOLEAN DEFAULT FALSE NOT NULL,
    cancelled    BOOLEAN DEFAULT FALSE NOT NULL,
    delivered    BOOLEAN DEFAULT FALSE NOT NULL,
    created_time BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_time BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TRIGGER auctions_update_trigger
    BEFORE UPDATE ON auctions
    FOR EACH ROW
EXECUTE FUNCTION set_updated_time_millis();

CREATE INDEX auctions_server_season_gamer_index ON auctions (realm, client);

CREATE TABLE IF NOT EXISTS auction_transaction_history
(
    auction_id  BIGINT PRIMARY KEY REFERENCES auctions (id) ON DELETE CASCADE,
    buyer       BIGINT NOT NULL REFERENCES clients (id),
    time_sold   BIGINT NOT NULL
);