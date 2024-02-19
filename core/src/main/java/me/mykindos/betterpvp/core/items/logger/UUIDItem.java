package me.mykindos.betterpvp.core.items.logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * Represents an item with an uuid
 */
@EqualsAndHashCode
@Getter
public class UUIDItem {
    public UUID uuid;
    public String namespace;
    private String key;
    private NamespacedKey namespacedKey;
    public UUIDItem (UUID uuid, String namespace, String key) {
        this.uuid = uuid;
        this.namespace = namespace;
        this.key = key;
        this.namespacedKey = new NamespacedKey(namespace, key);
    }

    public String getIdentifier() {
        return namespacedKey.asString();
    }

}
