package me.mykindos.betterpvp.clans.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@EqualsAndHashCode(callSuper = true)
@Data
public class ClanDisbandEvent extends CustomEvent {

    private final Player player;
    private final Clan clan;

}