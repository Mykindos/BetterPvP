create table if not exists ${tablePrefix}imbuement_data
(
    ImbuementName       varchar(255)  not null primary key,
    NamespaceKey        varchar(255)  not null,
    AffixText           varchar(255)  not null,
    Material            varchar(255)  not null,
    Value               double        not null default 0,
    OnArmour            tinyint       not null default 0,
    OnWeapons           tinyint       not null default 0,
    OnTools             tinyint       not null default 0
    );

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, Value, OnArmour, OnWeapons, OnTools) VALUES
    ("Haste", "champions:imbuement-haste", "<green>15% <gray>cooldown reduction", "SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE", 15, 1, 1, 1);

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, Value, OnArmour, OnWeapons, OnTools) VALUES
    ("Vigor", "champions:imbuement-vigor", "<green>10% <gray>increased damage", "RAISER_ARMOR_TRIM_SMITHING_TEMPLATE", 10, 0, 1, 0);

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, Value, OnArmour, OnWeapons, OnTools) VALUES
    ("Fortification", "champions:imbuement-fortification", "<green>10% <gray>damage reduction", "WARD_ARMOR_TRIM_SMITHING_TEMPLATE", 10, 1, 0, 0);

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, Value, OnArmour, OnWeapons, OnTools) VALUES
    ("Insight", "champions:imbuement-insight", "<green>10% <gray>energy cost reduction", "WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE", 10, 1, 1, 1);

-- Add runes to items & itemlore
INSERT IGNORE INTO items (id, Material, Module, Name, Glow) VALUES (33, 'SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Skill Haste', 0);
INSERT IGNORE INTO itemlore VALUES (33, 0, ''), (33, 1, '<blue>Affix'), (33, 2, '<green>15% <gray>cooldown reduction');

INSERT IGNORE INTO items (id, Material, Module, Name, Glow) VALUES (34, 'RAISER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Vigor', 0);
INSERT IGNORE INTO itemlore VALUES (34, 0, ''), (34, 1, '<blue>Affix'), (34, 2, '<green>20% <gray>increased damage');

INSERT IGNORE INTO items (id, Material, Module, Name, Glow) VALUES (35, 'WARD_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Fortification', 0);
INSERT IGNORE INTO itemlore VALUES (35, 0, ''), (35, 1, '<blue>Affix'), (35, 2, '<green>10% <gray>damage reduction');

INSERT IGNORE INTO items (id, Material, Module, Name, Glow) VALUES (36, 'WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Insight', 0);
INSERT IGNORE INTO itemlore VALUES (36, 0, ''), (36, 1, '<blue>Affix'), (36, 2, '<green>10% <gray>energy cost reduction');
