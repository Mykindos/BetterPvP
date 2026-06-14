-- Replace the boolean SIDEBAR_ENABLED property with the three-state SIDEBAR_MODE (DISABLED/SIDEBAR/HUD).
INSERT INTO property_map VALUES ('SIDEBAR_MODE', 'string') ON CONFLICT DO NOTHING;

-- Migrate stored values: enabled -> HUD (the new default "on" experience), disabled -> DISABLED.
UPDATE client_properties
   SET property = 'SIDEBAR_MODE',
       value = CASE WHEN value = 'true' THEN 'HUD' ELSE 'DISABLED' END
 WHERE property = 'SIDEBAR_ENABLED';

DELETE FROM property_map WHERE property = 'SIDEBAR_ENABLED';
