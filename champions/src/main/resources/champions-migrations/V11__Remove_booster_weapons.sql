-- Delete boosters
DELETE
FROM itemlore
WHERE Text = '<gray>+1 Level to Sword Skills';
DELETE
FROM itemlore
WHERE Text = '<gray>+1 Level to Axe Skills';

-- Change ancient weapon damage
UPDATE champions_damagevalues
SET Damage = 7.5
WHERE Material = 'NETHERITE_SWORD'
   OR Material = 'NETHERITE_AXE';

UPDATE
    itemlore
SET Text = '<gray>Damage: <green>7.5'
WHERE Item = (SELECT id FROM items WHERE Material = 'NETHERITE_AXE' AND Namespace = 'champions')
   OR Item = (SELECT id FROM items WHERE Material = 'NETHERITE_SWORD' AND Namespace = 'champions');

-- Change booster weapon damage
UPDATE champions_damagevalues
SET Damage = 6.5
WHERE Material = 'GOLDEN_SWORD'
   OR Material = 'GOLDEN_AXE';

UPDATE
    itemlore
SET Text = '<gray>Damage: <green>6.5'
WHERE Item = (SELECT id FROM items WHERE Material = 'GOLDEN_AXE' AND Namespace = 'champions')
   OR Item = (SELECT id FROM items WHERE Material = 'GOLDEN_SWORD' AND Namespace = 'champions');