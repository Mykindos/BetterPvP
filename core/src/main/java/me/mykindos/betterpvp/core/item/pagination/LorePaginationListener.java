package me.mykindos.betterpvp.core.item.pagination;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.renderer.LorePages;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Lets a player flip an item's lore page by pressing the offhand/swap key (F) while hovering it
 * in their own inventory. The vanilla {@code PlayerSwapHandItemsEvent} only fires with no screen
 * open (where lore cannot be read), so we instead handle the {@code SWAP_OFFHAND} inventory click.
 * <p>
 * Only the player's own inventory slots are handled, so this never fights InvUI's own offhand-key
 * handling for menu (top) inventories, and only multi-page items are intercepted.
 */
@BPvPListener
@Singleton
public class LorePaginationListener implements Listener {

    private final ItemFactory itemFactory;
    private final LorePageService lorePageService;

    @Inject
    public LorePaginationListener(ItemFactory itemFactory, LorePageService lorePageService) {
        this.itemFactory = itemFactory;
        this.lorePageService = lorePageService;
    }

    @EventHandler
    public void onSwapOffhand(InventoryClickEvent event) {
        // Version-safe check (SWAP_OFFHAND absent on older APIs), matching the codebase convention.
        if (event.getClick() != ClickType.SWAP_OFFHAND) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.getType() != InventoryType.PLAYER) {
            return;
        }

        final ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }

        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(current);
        if (instanceOpt.isEmpty()) {
            return;
        }

        final ItemInstance instance = instanceOpt.get();
        if (LorePages.visiblePages(instance).size() <= 1) {
            return; // nothing to page through - let vanilla handle the swap
        }

        event.setCancelled(true);
        lorePageService.advance(player.getUniqueId(), instance);
        player.updateInventory(); // re-sent packets re-flow through ItemPacketRemapper at the new page
        new SoundEffect(Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON, 2f, 1f).play(player);
    }

}
