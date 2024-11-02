DELETE FROM shopitems WHERE Material = 'CROSSBOW';

UPDATE shopitems SET BuyPrice = 50, SellPrice = 25 WHERE Material LIKE "%_LOG%";