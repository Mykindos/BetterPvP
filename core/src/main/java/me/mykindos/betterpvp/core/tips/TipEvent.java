package me.mykindos.betterpvp.core.tips;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class TipEvent extends CustomCancellableEvent {

    private final Player player;
    private final Gamer gamer;
    private final WeighedList<Tip> tipList = new WeighedList<>();

    public TipEvent(Player player, Gamer gamer) {
        super(true);
        this.gamer = gamer;
        this.player = player;
    }

}
