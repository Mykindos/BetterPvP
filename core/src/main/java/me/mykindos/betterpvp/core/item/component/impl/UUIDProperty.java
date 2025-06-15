package me.mykindos.betterpvp.core.item.component.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;

import java.util.UUID;

/**
 * Represents Items that are unique and are not stackable.
 * Serialization is handled by UUIDPropertySerializer.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UUIDProperty extends AbstractItemComponent {

    private final UUID uniqueId;

    public UUIDProperty(UUID uniqueId) {
        super("uuid");
        this.uniqueId = uniqueId;
    }

    public UUIDProperty() {
        this(UUID.randomUUID());
    }

    @Override
    public UUIDProperty copy() {
        return new UUIDProperty(uniqueId);
    }
}
