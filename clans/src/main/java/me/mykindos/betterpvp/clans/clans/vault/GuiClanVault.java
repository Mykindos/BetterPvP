package me.mykindos.betterpvp.clans.clans.vault;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.vault.restriction.ClanVaultRestrictions;
import me.mykindos.betterpvp.clans.clans.vault.restriction.VaultRestriction;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class GuiClanVault extends AbstractGui implements Windowed {

    private static final Item DISABLED_SLOT_ITEM = new SimpleItem(ItemView.builder()
            .material(Material.RED_STAINED_GLASS_PANE)
            .displayName(Component.text("LOCKED SLOT", NamedTextColor.RED, TextDecoration.BOLD))
            .build(), click -> SoundEffect.WRONG_ACTION.play(click.getPlayer()));

    private final ClanVault vault;

    GuiClanVault(@NotNull Player viewer, @NotNull ClanVault vault, @Nullable Windowed previousMenu) {
        super(9, 6);
        this.vault = vault;

        // Middle of last row
        setItem(4, 5, new BackButton(previousMenu));
        fillBorders(Menu.BACKGROUND_GUI_ITEM, false);

        // Virtual inventory
        final Clans clans = JavaPlugin.getPlugin(Clans.class);
        final ClanVaultRestrictions restrictions = clans.getInjector().getInstance(ClanVaultRestrictions.class);
        final int maxSize = clans.getConfig().getOrSaveInt("clans.clan.vault.max-size", 28);
        final int size = vault.getSize();

        // Initialize ItemStack[] array with size of 'size' and contents of vault.getContents(). Empty slots are filled with null.
        final ItemStack[] contents = new ItemStack[size];
        vault.getContents().forEach((slot, item) -> {
            if (slot >= size) return;
            contents[slot] = item;
        });
        final VirtualInventory virtualInventory = new VirtualInventory(UUID.randomUUID(), contents);

        // Save handler
        virtualInventory.setPostUpdateHandler(event -> {
            vault.getContents().clear();
            final @Nullable ItemStack[] items = virtualInventory.getItems();
            for (int i = 0; i < items.length; i++) {
                final ItemStack item = items[i];
                if (item != null) {
                    vault.getContents().put(i, item);
                }
            }
        });

        // Update handler
        virtualInventory.setPreUpdateHandler(event -> {
            final ItemStack item = event.getNewItem();
            if (item == null) {
                return; // Empty slot
            }

            final Optional<VaultRestriction> restrictionOpt = restrictions.getAvailable(vault, item);
            if (restrictionOpt.isEmpty()) {
                return; // No restriction, item is allowed
            }

            final VaultRestriction restriction = restrictionOpt.get();
            final OptionalInt remainingOpt = restriction.getRemainingCount(vault);
            if (remainingOpt.isEmpty()) {
                return; // Infinite count
            }

            final int remainingCount = remainingOpt.getAsInt();
            if (remainingCount > 0 && event.getAddedAmount() <= remainingCount) {
                return; // Item is allowed
            }

            SoundEffect.WRONG_ACTION.play(viewer);
            event.setCancelled(true);
        });

        // Populate this AbstractGui with the contents of the virtual inventory
        for (int i = 0; i < size; i++) {
            addSlotElements(new SlotElement.InventorySlotElement(virtualInventory, i));
        }

        // Add disabled slots
        for (int i = size; i < maxSize; i++) {
            addItems(DISABLED_SLOT_ITEM);
        }
    }

    GuiClanVault(@NotNull Player viewer, @NotNull ClanVault vault) {
        this(viewer, vault, null);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(vault.getClan().getName() + ": Clan Vault");
    }
}
