package me.mykindos.betterpvp.clans.clans.tips;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import net.minecraft.world.entity.player.Player;

@Getter
public class TipEvent extends CustomCancellableEvent {

    private final Gamer gamer;
    TipEvent(Gamer gamer){
        super(true);
        this.gamer = gamer;
    }
}
