INSERT IGNORE INTO property_map VALUES ("SHOW_TAG", "string");

UPDATE client_properties
    SET Value = "SHORT"
    WHERE Property = "SHOW_TAG";
