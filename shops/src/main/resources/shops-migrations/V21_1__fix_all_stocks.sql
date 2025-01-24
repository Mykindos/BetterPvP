SELECT id INTO @mushroomId FROM shopitems WHERE Shopkeeper = 'Weapons / Tools' AND ItemName = 'Mushroom Stew';
SELECT id INTO @honeycombId FROM shopitems WHERE Shopkeeper = 'Farming' AND ItemName = 'Honeycomb';
UPDATE shopitems_dynamic_pricing SET BaseStock = 25000, MaxStock = 50000, CurrentStock = 25000  WHERE shopItemId NOT IN(@mushroomId, @honeycombId);
UPDATE shopitems_dynamic_pricing SET BaseStock = 5000, MaxStock = 10000, CurrentStock = 5000 WHERE shopItemId = @honeycombId;