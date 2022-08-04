package me.mykindos.betterpvp.clans.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ClanEvent extends CustomCancellableEvent {

    private final Player player;
    private final Clan clan;
    private final boolean globalScoreboardUpdate;


}
