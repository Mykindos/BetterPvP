package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.loot.item.DroppedItemLoot;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.loot.FishLoot;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@CustomLog
@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class ClansFishingListener implements Listener {

    private final ClanManager clanManager;
    private final ItemFactory itemFactory;

    @Inject
    public ClansFishingListener(ClanManager clanManager, ItemFactory itemFactory) {
        this.clanManager = clanManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        final FishLoot fishLoot = event.getFishLoot();
        if (fishLoot == null || fishLoot.getCurrentFish() == null) return;

        final Fish fish = fishLoot.getCurrentFish();
        final boolean inFields = clanManager.getClanByLocation(event.getHook().getLocation().getBlock().getLocation())
                .map(c -> c.getName().equalsIgnoreCase("Fields"))
                .orElse(false);

        if (!inFields) {
            if (event.isBaseFishingUnlocked()) {
                return;
            }

            fish.setWeight((int) (fish.getWeight() * 0.50));
            if (UtilMath.randomInt(20) < 2) {
                UtilMessage.message(event.getPlayer(), "core.prefix.fishing", "clans.fishing.outside-fields");
            }
        } else {
            UtilServer.callEvent(new ClanAddExperienceEvent(event.getPlayer(), 0.1));
        }
    }

    /**
     * Substitutes important-rarity fishing-treasure drops with a {@link Material#DRAGON_HEAD} when
     * the player is not in Fields, mirroring the legacy gate that previously sat on
     * {@code PlayerCaughtFishEvent} before the LootTable migration.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTreasureAwarded(LootAwardedEvent event) {
        if(true) {
            return;
        }
        if (!event.getContext().getSource().getId().startsWith("fishing:")) return;
        if (!(event.getLoot() instanceof DroppedItemLoot droppedLoot)) return;

        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;

        final ItemInstance reward = droppedLoot.getReward();
        if (!reward.getRarity().isImportant()) return;

        final Location location = event.getContext().getLocation();
        final boolean inFields = clanManager.getClanByLocation(location.getBlock().getLocation())
                .map(c -> c.getName().equalsIgnoreCase("Fields"))
                .orElse(false);
        if (inFields) return;

        // The dropped Item entity exists at the context location after award; find and replace it.
        final Material expectedType = reward.getItemStack().getType();
        final Optional<Item> dropped = location.getNearbyEntitiesByType(Item.class, 1.0).stream()
                .filter(item -> item.getItemStack().getType() == expectedType)
                .findFirst();

        if (dropped.isEmpty()) {
            log.warn("Could not locate awarded fishing treasure ({}) at {} to substitute for non-Fields catch.",
                    expectedType, location).submit();
            return;
        }

        dropped.get().setItemStack(new ItemStack(Material.DRAGON_HEAD));

        final Component name = reward.getBaseItem().getItemNameRenderer().createName(reward);
        UtilMessage.message(player, "core.prefix.fishing", "clans.fishing.legendary-denied", name);
        UtilMessage.message(player, "core.prefix.fishing", "clans.fishing.replacement");

        log.info("{} ({}) would have caught a legendary while fishing, but they were not at fields!",
                player.getName(), player.getUniqueId()).submit();
    }
}
