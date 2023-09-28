package me.mykindos.betterpvp.clans.clans.tips;

import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
@Data
public class TipEvent extends CustomCancellableEvent {

    private final Player player;
    TipEvent(Player player){
        super(true);
        this.player = player;
    }
}
