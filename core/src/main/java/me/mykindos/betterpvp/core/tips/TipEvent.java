package me.mykindos.betterpvp.core.tips;

import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.core.tips.Tip;
import org.bukkit.entity.Player;

@Getter
@Data
public class TipEvent extends CustomCancellableEvent {

    private final Player player;
    private final WeighedList<Tip> tipList = new WeighedList<Tip>();
    TipEvent(Player player) {
        super(true);
        this.player = player;
    }
}
