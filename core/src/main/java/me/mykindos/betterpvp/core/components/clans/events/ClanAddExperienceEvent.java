package me.mykindos.betterpvp.core.components.clans.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ClanAddExperienceEvent extends CustomEvent {

    private final Player player;
    private final double experience;
    /** Human-readable label shown in XP gain notifications (e.g. "Killing Enemy", "Mining Ore"). */
    private final String reason;
    /** UUID of the player whose contribution ledger should be credited. Defaults to player's UUID. */
    @Nullable
    private final UUID contributor;

    public ClanAddExperienceEvent(Player player, double experience, String reason) {
        this.player = player;
        this.experience = experience;
        this.reason = reason;
        this.contributor = player != null ? player.getUniqueId() : null;
    }

    public ClanAddExperienceEvent(Player player, double experience, String reason, @Nullable UUID contributor) {
        this.player = player;
        this.experience = experience;
        this.reason = reason;
        this.contributor = contributor;
    }

}
