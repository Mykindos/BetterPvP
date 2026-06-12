package me.mykindos.betterpvp.progression.profession.fishing.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.loot.FishLoot;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Per-entry side effects for the regular fishing catch flow. The {@link FishingListener}
 * generates the bundle and calls {@link me.mykindos.betterpvp.core.loot.LootBundle#award()};
 * this listener observes each awarded entry and applies item reservation, despawn lifetime,
 * and FishLoot bookkeeping.
 */
@BPvPListener
@Singleton
public class FishingCatchLootListener implements Listener {

    private final Progression progression;
    private final FishingHandler fishingHandler;

    @Inject
    public FishingCatchLootListener(Progression progression, FishingHandler fishingHandler) {
        this.progression = progression;
        this.fishingHandler = fishingHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCatchLootAwarded(LootAwardedEvent event) {
        if (!"fishing:catch".equals(event.getContext().getSource().getId())) return;
        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;

        if (event.getResult() instanceof Item item) {
            UtilItem.reserveItem(item, player, 30);
            UtilServer.runTaskLater(progression, () -> {
                if (item.isValid()) {
                    item.remove();
                }
            }, 20L * 60L);
        }

        if (event.getLoot() instanceof FishLoot fishLoot) {
            final Fish fish = fishLoot.getCurrentFish();
            if (fish != null) {
                fishingHandler.addFish(player, fish);
                UtilMessage.message(player, "core.prefix.fishing", "progression.fishing.caught-fish",
                        Component.text(fish.getTypeName(), NamedTextColor.GREEN),
                        Component.text(UtilFormat.formatNumber(fish.getWeight()), NamedTextColor.YELLOW));
            }
        }
    }
}
