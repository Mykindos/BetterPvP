package me.mykindos.betterpvp.champions.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

@Singleton
@BPvPListener
public class CraftingListener implements Listener {

    private final Champions champions;

    private final ItemHandler itemHandler;

    private final CraftingManager craftingManager;

    @Inject
    public CraftingListener(Champions champions, ItemHandler itemHandler, CraftingManager craftingManager) {
        this.champions = champions;
        this.itemHandler = itemHandler;
        this.craftingManager = craftingManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        var firstItem = event.getInventory().getFirstItem();
        if (firstItem == null) return;

        var secondItem = event.getInventory().getSecondItem();
        if (secondItem == null) return;

        var imbuement = craftingManager.getImbuements().stream().filter(imb -> imb.getRuneMaterial() == secondItem.getType()).findFirst().orElse(null);
        if (imbuement == null) return;

        var imbuementKey = NamespacedKey.fromString(imbuement.getKey());
        if (imbuementKey == null) return;

        if (firstItem.getItemMeta().getPersistentDataContainer().has(imbuementKey)) return;

        if (UtilItem.isArmour(firstItem.getType()) && !imbuement.isCanImbueArmour()) {
            return;
        }

        if (UtilItem.isWeapon(firstItem.getType()) && !imbuement.isCanImbueWeapons()) {
            if(!UtilItem.isAxe(firstItem.getType()) && !imbuement.isCanImbueTools()) {
                return;
            }
        }

        if (UtilItem.isTool(firstItem.getType()) && !imbuement.isCanImbueTools()) {
            if(!UtilItem.isAxe(firstItem.getType()) && !imbuement.isCanImbueWeapons()) {
                return;
            }
        }

        ItemStack result = firstItem.clone();
        ItemMeta meta = result.getItemMeta();

        var oldLore = meta.lore();
        if (oldLore != null) {
            var newLore = new ArrayList<>(oldLore);
            var affixIndex = UtilItem.indexOf("affixes", newLore);

            if (affixIndex == -1) {
                newLore.add(Component.empty());
                newLore.add(Component.text("Affixes").color(NamedTextColor.BLUE));
                affixIndex = newLore.size() - 1;
            }

            newLore.add(affixIndex + 1, MiniMessage.miniMessage().deserialize(imbuement.getAffixText()));

            meta.lore(newLore);
        }
        meta.getPersistentDataContainer().set(imbuementKey, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(CoreNamespaceKeys.GLOW_KEY, PersistentDataType.STRING, "true");
        result.setItemMeta(meta);

        event.setResult(itemHandler.updateNames(result));

        // Don't ask why, it's just required for some stupid reason
        UtilServer.runTaskLater(champions, () -> event.getInventory().setRepairCost(0), 1);

    }

    @EventHandler
    public void onLoreUpdate(ItemUpdateLoreEvent event) {

        craftingManager.getImbuements().forEach(imbuement -> {
            var namespacedKey = NamespacedKey.fromString(imbuement.getKey());
            if (namespacedKey == null) return;

            if (event.getItemMeta().getPersistentDataContainer().has(namespacedKey)) {

                var affixIndex = UtilItem.indexOf("affixes", event.getItemLore());
                if (affixIndex == -1) {
                    event.getItemLore().add(Component.empty());
                    event.getItemLore().add(UtilMessage.getMiniMessage("<blue>Affixes"));
                    affixIndex = event.getItemLore().size() - 1;
                }

                event.getItemLore().add(affixIndex + 1, UtilMessage.getMiniMessage(imbuement.getAffixText()));
            }
        });
    }

    @EventHandler
    public void onNameUpdate(ItemUpdateNameEvent event) {

        var hasImbuement = craftingManager.getImbuements().stream().anyMatch(imbuement -> {
            var namespacedKey = NamespacedKey.fromString(imbuement.getKey());
            if (namespacedKey != null) {
                return event.getItemMeta().getPersistentDataContainer().has(namespacedKey);
            }
            return false;
        });

        if (hasImbuement) {
            event.setItemName(
                    Component.text("Imbued ").color(NamedTextColor.AQUA)
                            .append(event.getItemName().color(NamedTextColor.AQUA))
                            .decoration(TextDecoration.ITALIC, false)
            );
        }


    }
}
