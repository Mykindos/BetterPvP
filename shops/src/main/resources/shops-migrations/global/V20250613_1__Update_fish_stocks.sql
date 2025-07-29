UPDATE shopitems_dynamic_pricing SET BaseStock = 25000, MaxStock = 50000, CurrentStock = 25000
WHERE shopItemId IN (SELECT id FROM shopitems WHERE Shopkeeper = 'Fisherman' AND Material = "COD")