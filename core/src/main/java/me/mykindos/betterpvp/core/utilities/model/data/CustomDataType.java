package me.mykindos.betterpvp.core.utilities.model.data;

import lombok.experimental.UtilityClass;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

@UtilityClass
public class CustomDataType {

    public static final PersistentDataType<String, UUID> UUID = new PersistentUUIDType();

}
