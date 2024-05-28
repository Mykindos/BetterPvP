package me.mykindos.betterpvp.progression.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

import java.util.UUID;

@Getter
public class ProfessionPropertyUpdateEvent extends PropertyUpdateEvent {

    private final UUID uuid;
    private final String profession;

    public ProfessionPropertyUpdateEvent(UUID uuid, String profession, String property, Object value) {
        super(property, value);
        this.uuid = uuid;
        this.profession = profession;
    }
}
