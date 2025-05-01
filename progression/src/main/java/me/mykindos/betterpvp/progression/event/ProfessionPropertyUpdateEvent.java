package me.mykindos.betterpvp.progression.event;

import java.util.UUID;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@Getter
public class ProfessionPropertyUpdateEvent extends CustomCancellableEvent {

    private final UUID uuid;
    private final String profession;
    private final String property;
    private final Object value;

    public ProfessionPropertyUpdateEvent(UUID uuid, String profession, String property, Object value) {
        this.uuid = uuid;
        this.profession = profession;
        this.property = property;
        this.value = value;
    }
}
