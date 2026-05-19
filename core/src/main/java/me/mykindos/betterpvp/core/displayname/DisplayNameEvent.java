package me.mykindos.betterpvp.core.displayname;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
public class DisplayNameEvent extends CustomEvent {

    private final Entity entity, targetEntity;
    private String displayName;

    public DisplayNameEvent(final Entity entity, final Entity targetEntity) {
        this.entity = entity;
        this.targetEntity = targetEntity;

        this.displayName = "<yellow>%s</yellow>".formatted(entity.getName());
    }
}