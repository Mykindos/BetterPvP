UPDATE shopitems
SET shopkeeper = CASE shopkeeper
    WHEN 'Building' THEN 'Block Merchant'
    WHEN 'Farming' THEN 'Farmer'
    WHEN 'Resources' THEN 'Resource Merchant'
    WHEN 'Weapons / Tools' THEN 'Blacksmith'
    ELSE shopkeeper
END
WHERE shopkeeper IN ('Building', 'Farming', 'Resources', 'Weapons / Tools');

DELETE FROM shopitems WHERE shopkeeper = 'Armor';