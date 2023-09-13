create table if not exists ${tablePrefix}imbuement_data
(
    ImbuementName       varchar(255)  not null primary key,
    NamespaceKey        varchar(255)  not null,
    AffixText           varchar(255)  not null,
    Material            varchar(255)  not null,
    OnArmour            tinyint       not null default 0,
    OnWeapons           tinyint       not null default 0,
    OnTools             tinyint       not null default 0
    );

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, OnArmour, OnWeapons, OnTools) VALUES
    ("Haste", "champions:haste-rune", "<green>15% <gray>cooldown reduction", "SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 1, 1);

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, OnArmour, OnWeapons, OnTools) VALUES
    ("Vigor", "champions:vigor-rune", "<green>10% <gray>increased damage", "RAISER_ARMOR_TRIM_SMITHING_TEMPLATE", 0, 1, 0);