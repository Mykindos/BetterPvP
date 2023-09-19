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

INSERT IGNORE INTO ${tablePrefix}imbuement_data (ImbuementName, NamespaceKey, AffixText, Material, Value, OnArmour, OnWeapons, OnTools) VALUES
    ("Fortune", "champions:imbuement-fortune", "<green>10% <gray>chance to get double resources", "DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", 10, 0, 0, 1);

-- Add runes to items & item lore
INSERT IGNORE INTO ${coreTablePrefix}items (Material, Module, Name, Glow) VALUES ('SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Skill Haste', 0);
SELECT id INTO @itemId FROM ${coreTablePrefix}items WHERE Material = 'SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE';
INSERT IGNORE INTO ${coreTablePrefix}itemlore VALUES (@itemId, 0, '<gray>Can be applied to any equippable item'), (@itemId, 1, ''), (@itemId, 2, '<blue>Affix'), (@itemId, 3, '<green>15% <gray>cooldown reduction');

INSERT IGNORE INTO ${coreTablePrefix}items (Material, Module, Name, Glow) VALUES ('RAISER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Vigor', 0);
SELECT id INTO @itemId FROM ${coreTablePrefix}items WHERE Material = 'RAISER_ARMOR_TRIM_SMITHING_TEMPLATE';
INSERT IGNORE INTO ${coreTablePrefix}itemlore VALUES (@itemId, 0, '<gray>Can only be applied to weapons'), (@itemId, 1, ''), (@itemId, 2, '<blue>Affix'), (@itemId, 3, '<green>20% <gray>increased damage');

INSERT IGNORE INTO ${coreTablePrefix}items (Material, Module, Name, Glow) VALUES ( 'WARD_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Fortification', 0);
SELECT id INTO @itemId FROM ${coreTablePrefix}items WHERE Material = 'WARD_ARMOR_TRIM_SMITHING_TEMPLATE';
INSERT IGNORE INTO ${coreTablePrefix}itemlore VALUES (@itemId, 0, '<gray>Can only be applied to armour'), (@itemId, 1, ''), (@itemId, 2, '<blue>Affix'), (@itemId, 3, '<green>10% <gray>damage reduction');

INSERT IGNORE INTO ${coreTablePrefix}items (Material, Module, Name, Glow) VALUES ('WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Insight', 0);
SELECT id INTO @itemId FROM ${coreTablePrefix}items WHERE Material = 'WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE';
INSERT IGNORE INTO ${coreTablePrefix}itemlore VALUES (@itemId, 0, '<gray>Can be applied to any equippable item'), (@itemId, 1, ''), (@itemId, 2, '<blue>Affix'), (@itemId, 3, '<green>10% <gray>energy cost reduction');

INSERT IGNORE INTO ${coreTablePrefix}items (Material, Module, Name, Glow) VALUES ('DUNE_ARMOR_TRIM_SMITHING_TEMPLATE', 'Champions', '<yellow>Rune of Fortune', 0);
SELECT id INTO @itemId FROM ${coreTablePrefix}items WHERE Material = 'DUNE_ARMOR_TRIM_SMITHING_TEMPLATE';
INSERT IGNORE INTO ${coreTablePrefix}itemlore VALUES (@itemId, 0, '<gray>Can only be applied to tools'),(@itemId, 1, ''), (@itemId, 2, '<blue>Affix'), (@itemId, 3, '<green>10% <gray>chance to get double resources');