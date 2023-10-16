package me.mykindos.betterpvp.clans.fields.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.fields.Fields;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public class FieldsInteractableUseEvent extends CustomEvent {

    private final Fields fields;
    private final FieldsInteractable type;
    private final FieldsBlock block;
    private final Player player;

    public FieldsInteractableUseEvent(Fields fields, FieldsInteractable type, FieldsBlock block, Player player) {
        this.fields = fields;
        this.type = type;
        this.block = block;
        this.player = player;
    }
}
