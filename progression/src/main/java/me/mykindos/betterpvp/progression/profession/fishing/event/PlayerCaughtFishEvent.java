package me.mykindos.betterpvp.progression.profession.fishing.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.progression.profession.fishing.loot.FishLoot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlayerCaughtFishEvent extends ProgressionFishingEvent {

    private final LootBundle bundle;

    /**
     * The specific {@link FishLoot} entry being processed in this catch event, if any.
     * Null when the bundle entry being processed is not a fish (e.g. an entity spawn or item).
     */
    @Nullable
    @Setter
    private FishLoot fishLoot;

    final FishHook hook;
    final Entity caught;

    @Setter
    private boolean ignoresWeight;
    @Setter
    private boolean baseFishingUnlocked;

    public PlayerCaughtFishEvent(Player player, LootBundle bundle, @Nullable FishLoot fishLoot,
                                  FishHook hook, Entity caught) {
        super(player);
        this.bundle = bundle;
        this.fishLoot = fishLoot;
        this.hook = hook;
        this.caught = caught;
    }
}
