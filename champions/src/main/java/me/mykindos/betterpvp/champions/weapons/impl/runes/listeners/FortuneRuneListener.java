package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Singleton
@PluginAdapter("Progression")
@BPvPListener
public class FortuneRuneListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public FortuneRuneListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    private KeyValue<Rune, PersistentDataContainer> getPlayerFortuneRuneData(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return null;

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer runePdc = pdc.get(RuneNamespacedKeys.FORTUNE, PersistentDataType.TAG_CONTAINER);
        if (runePdc == null) return null;

        String runeName = runePdc.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
        if (runeName == null) return null;

        Rune rune = (Rune) itemHandler.getItem(runeName);
        if (rune == null) return null;

        return new KeyValue<>(rune, runePdc);
    }

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatch(PlayerCaughtFishEvent event) {
        KeyValue<Rune, PersistentDataContainer> keyValueOfRuneData = getPlayerFortuneRuneData(event.getPlayer());
        if (keyValueOfRuneData == null) return;

        Rune rune = keyValueOfRuneData.getKey();
        PersistentDataContainer runePdc = keyValueOfRuneData.getValue();

        double increasedWeight = rune.getRollFromItem(runePdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
        increasedWeight = 1 + (increasedWeight / 100);

        event.setIgnoresWeight(true);
        if (event.getLoot() instanceof Fish fish) {
            fish.setWeight((int) (fish.getWeight() * increasedWeight));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        KeyValue<Rune, PersistentDataContainer> keyValueOfRuneData = getPlayerFortuneRuneData(event.getPlayer());
        if (keyValueOfRuneData == null) return;

        Rune rune = keyValueOfRuneData.getKey();
        PersistentDataContainer runePdc = keyValueOfRuneData.getValue();

        double doubleLogsRoll = rune.getRollFromItem(runePdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
        double doubleLogsChance = doubleLogsRoll / 100;

        if (Math.random() < doubleLogsChance) {
            event.setAdditionalLogsDropped(1);
        }
    }
}
