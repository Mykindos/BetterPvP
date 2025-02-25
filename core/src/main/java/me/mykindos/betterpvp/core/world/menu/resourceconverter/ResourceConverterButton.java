package me.mykindos.betterpvp.core.world.menu.resourceconverter;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ResourceConverterButton extends AbstractItem {

    private final Player player;
    private final ItemStack itemStack;
    private final ItemStack targetResource;

    public ResourceConverterButton(Player player, ItemStack itemStack, ItemStack targetResource) {
        this.player = player;
        this.itemStack = itemStack.clone();
        this.targetResource = targetResource.clone();
    }

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder().material(targetResource.getType())
                .displayName(Objects.requireNonNull(targetResource.getItemMeta().displayName())
                        .color(NamedTextColor.YELLOW)).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (UtilInventory.remove(player, itemStack.getType(), itemStack.getAmount())) {
            targetResource.setAmount(itemStack.getAmount());
            UtilItem.insert(player, targetResource);
            UtilMessage.simpleMessage(player, "Resource Converter", "Successfully converted resources.");
            UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1f);
        }

        player.closeInventory();
    }
}
