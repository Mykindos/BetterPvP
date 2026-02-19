package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
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

    @EventHandler
    public void onCaughtFish(PlayerCaughtFishEvent event) {
        if (!(event.getLoot().getType() instanceof TreasureType treasureType)) {
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getHook().getLocation());
        if (clanOptional.isPresent() && clanOptional.get().getName().equalsIgnoreCase("Fields")) {
            return;
        }

        final ItemStack itemStack = treasureType.generateItem(1);
        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        if (itemOpt.isEmpty() || !itemOpt.get().getRarity().isImportant()) {
            return;
        }

        event.setLoot(new FishingLoot() {
            @Override
            public FishingLootType getType() {
                return null;
            }

            @Override
            public void processCatch(PlayerCaughtFishEvent event) {
                final ItemStack itemStack = new ItemStack(Material.DRAGON_HEAD);
                Item item = (Item) event.getCaught();
                UtilItem.reserveItem(item, event.getPlayer(), 10);
                item.setItemStack(itemStack);
             }
        });

        final ItemInstance instance = itemOpt.get();
        final Component text = Component.text("You would have caught a ", NamedTextColor.GRAY)
                .append(instance.getBaseItem().getItemNameRenderer().createName(instance))
                .append(Component.text(", but you were not at Fields!", NamedTextColor.GRAY));

        UtilMessage.simpleMessage(event.getPlayer(), "Fishing", text);
        UtilMessage.simpleMessage(event.getPlayer(), "Fishing", "Have this instead...");
        log.info("{} ({}) would have caught a legendary while fishing, but they were not at fields!", event.getPlayer().getName(), event.getPlayer().getUniqueId()).submit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if (!(event.getLoot() instanceof Fish fish)) return;
        if (!clanManager.getClanByLocation(event.getHook().getLocation().getBlock().getLocation())
                .map(c -> c.getName().equalsIgnoreCase("Fields"))
                .orElse(false)) {

            if (event.isBaseFishingUnlocked()) {
                return;
            }

            fish.setWeight((int) (fish.getWeight() * 0.50));
            if (UtilMath.randomInt(20) < 2) {
                UtilMessage.simpleMessage(event.getPlayer(), "Fishing", "Fish caught outside of Fields are half their normal size.");
            }
        }

        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> clan.getExperience().grantXp(event.getPlayer(), 50, "Fishing"));
    }
}
