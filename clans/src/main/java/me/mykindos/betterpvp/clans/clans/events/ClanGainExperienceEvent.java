package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ClanGainExperienceEvent extends CustomCancellableEvent {

    private final Clan clan;
    private final Player player;
    private final long experience;
    /** Human-readable label shown in XP gain notifications (e.g. "Killing Enemy", "Mining Ore"). */
    private final String reason;

    public ClanGainExperienceEvent(Clan clan, Player player, long experience, String reason) {
        this.clan = clan;
        this.player = player;
        this.experience = experience;
        this.reason = reason;
    }
}
