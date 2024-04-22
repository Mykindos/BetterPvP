package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.tree.fishing.fish.Fish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Singleton
@PluginAdapter("Progression")
public class FortuneRuneListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public FortuneRuneListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatch(PlayerCaughtFishEvent event) {
        ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer runePdc = pdc.get(RuneNamespacedKeys.FORTUNE, PersistentDataType.TAG_CONTAINER);
        if (runePdc == null) return;

        String runeName = runePdc.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
        if (runeName == null) return;

        Rune rune = (Rune) itemHandler.getItem(runeName);
        if(rune == null) return;

        double increasedWeight = rune.getRollFromItem(runePdc, rune.getAppliedNamespacedKey(), PersistentDataType.DOUBLE);
        increasedWeight = 1 + (increasedWeight / 100);

        event.setIgnoresWeight(true);
        if (event.getLoot() instanceof Fish fish) {
            event.setLoot(new Fish(fish.getType(), (int) (fish.getWeight() * increasedWeight)));
        }
    }
}
