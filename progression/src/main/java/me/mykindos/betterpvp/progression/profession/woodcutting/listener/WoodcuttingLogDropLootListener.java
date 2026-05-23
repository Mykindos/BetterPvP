package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.loot.item.DroppedItemLoot;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Reserves dropped log items for the chopping player when they have the Protection effect.
 * Observes the {@link LootAwardedEvent} fired by the woodcutting log-drop replacement flow.
 */
@BPvPListener
@Singleton
public class WoodcuttingLogDropLootListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public WoodcuttingLogDropLootListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogDropAwarded(LootAwardedEvent event) {
        if (!"woodcutting:log_drop".equals(event.getContext().getSource().getId())) return;
        if (!(event.getLoot() instanceof DroppedItemLoot)) return;
        if (!(event.getResult() instanceof Item item)) return;

        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;

        UtilItem.reserveItem(item, player, 10);
    }
}
