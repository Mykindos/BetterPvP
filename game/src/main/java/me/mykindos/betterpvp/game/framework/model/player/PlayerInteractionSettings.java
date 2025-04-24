package me.mykindos.betterpvp.game.framework.model.player;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PlayerInteractionSettings {

    @Builder.Default
    boolean blockPlace = false;

    @Builder.Default
    boolean blockBreak = false;

    @Builder.Default
    boolean blockInteract = false;

    @Builder.Default
    boolean inventoryClick = false;

    @Builder.Default
    boolean itemDrop = false;

    @Builder.Default
    boolean itemDurability = false;

}
