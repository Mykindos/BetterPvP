UPDATE shopitems_dynamic_pricing SET Server = 1, Season = 1 WHERE Server != 'TEMPLATE';
UPDATE shopitems_dynamic_pricing SET Server = 0, Season = 0 WHERE Server = 'TEMPLATE';

UPDATE auctions SET Server = 1, Season = 1;

ALTER TABLE shopitems_dynamic_pricing MODIFY COLUMN `Server` TINYINT UNSIGNED;
ALTER TABLE shopitems_dynamic_pricing MODIFY COLUMN `Season` TINYINT UNSIGNED;
ALTER TABLE auctions MODIFY COLUMN `Server` TINYINT UNSIGNED;
ALTER TABLE auctions MODIFY COLUMN `Season` TINYINT UNSIGNED;
