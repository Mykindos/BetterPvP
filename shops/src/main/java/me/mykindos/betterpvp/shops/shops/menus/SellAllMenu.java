package me.mykindos.betterpvp.shops.shops.menus;

import lombok.Getter;
import lombok.NonNull;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SellAllSlot;
import me.mykindos.betterpvp.shops.shops.menus.buttons.SellStagedItemsButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.direction.BackToPreviousButton;
import me.mykindos.betterpvp.shops.shops.menus.buttons.direction.DisabledPageButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class SellAllMenu extends AbstractGui implements Windowed {

    @Getter
    private final ShopContext context;
    @Getter
    private final VirtualInventory inventory = new VirtualInventory(45);
    private final Windowed previous;

    public SellAllMenu(ShopContext context, Windowed previous) {
        super(9, 6);
        this.context = context;
        this.previous = previous;

        inventory.setPreUpdateHandler(event -> {
            if (!(event.getUpdateReason() instanceof PlayerUpdateReason)) {
                return;
            }

            ItemStack newItem = event.getNewItem();
            if (newItem == null || newItem.getType().isAir()) {
                return;
            }

            ItemStack pricedItem = newItem.clone();
            event.setNewItem(pricedItem);
        });

        inventory.setPostUpdateHandler(event -> {
            updateControlItems();
        });

        applyStructure(new Structure(
                "v v v v v v v v v",
                "v v v v v v v v v",
                "v v v v v v v v v",
                "v v v v v v v v v",
                "v v v v v v v v v",
                "0 0 < ( 0 ) > 0 a")
                .addIngredient('<', new BackToPreviousButton(previous, false))
                .addIngredient('(', new BackToPreviousButton(previous, true))
                .addIngredient(')', new DisabledPageButton(true, false))
                .addIngredient('>', new DisabledPageButton(true, true))
                .addIngredient('a', new SellStagedItemsButton(this)));

        ItemView placeholder = ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Place items here to sell them!", NamedTextColor.RED))
                .itemModel(Resources.ItemModel.INVISIBLE)
                .build();
        for (int slot = 0; slot < 45; slot++) {
            setSlotElement(slot, new SellAllSlot(this, inventory, slot, placeholder));
        }
    }

    public void sellAll(Player player) {
        final Gamer gamer = context.getClientManager().search().online(player).getGamer();

        // Group stacks by shop item so one event (and one sell message) is fired per unique item type.
        final Map<IShopItem, Integer> shopItemAmounts = new LinkedHashMap<>();
        final @Nullable ItemStack[] items = inventory.getItems();
        for (int i = 0; i < items.length; i++) {
            final ItemStack item = items[i];
            if (item == null) continue;
            IShopItem shopItem = context.getSellService().findMatchingShopItem(item, context.getShopItemsByKey());
            if (shopItem == null) {
                inventory.setItem(null, i, null);
                UtilItem.insert(player, item);
                continue;
            }
            shopItemAmounts.merge(shopItem, item.getAmount(), Integer::sum);
        }

        boolean success = false;
        for (Map.Entry<IShopItem, Integer> entry : shopItemAmounts.entrySet()) {
            ShopCurrency currency = context.getCurrency(entry.getKey());
            PlayerSellItemEvent sellEvent = new PlayerSellItemEvent(player, gamer, entry.getKey(), inventory, currency);
            sellEvent.setRequestedAmount(entry.getValue());
            UtilServer.callEvent(sellEvent);
            if (!sellEvent.isCancelled()) {
                success = true;
            }
        }

        if (success) {
            new SoundEffect("betterpvp", "shop.sell").play(player);
            new SoundEffect("betterpvp", "game.domination.gem_pickup", 2, 0.05f).play(player);
        }
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);
        window.addCloseHandler(() -> {
            sellAll(player);
        });
        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-13><glyph:menu_shop_sell_all>").font(NEXO);
    }

    public int getTotalPrice() {
        int total = 0;
        for (ItemStack item : inventory.getItems()) {
            if (item == null) continue;
            IShopItem shopItem = context.getSellService().findMatchingShopItem(item, context.getShopItemsByKey());
            if (shopItem != null) {
                total += shopItem.getSellPrice() * item.getAmount();
            }
        }
        return total;
    }

    public boolean containsShopItem(IShopItem target) {
        for (ItemStack item : inventory.getItems()) {
            if (item == null) continue;
            IShopItem shopItem = context.getSellService().findMatchingShopItem(item, context.getShopItemsByKey());
            if (shopItem != null && shopItem.getId() == target.getId()) {
                return true;
            }
        }
        return false;
    }

    public void notifyPriceChanged() {
        updateControlItems();
        inventory.notifyWindows();
    }
}
