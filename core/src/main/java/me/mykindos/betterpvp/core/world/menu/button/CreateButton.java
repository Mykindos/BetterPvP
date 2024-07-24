package me.mykindos.betterpvp.core.world.menu.button;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@AllArgsConstructor
public class CreateButton extends AbstractItem {

    private final @NotNull WorldHandler worldHandler;
    private final @NotNull WorldCreator creator;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.GREEN_CONCRETE)
                .displayName(Component.text("Create World", NamedTextColor.GREEN, TextDecoration.BOLD))
                .glow(true)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        new ConfirmationMenu("Are you sure you want to create this world?", confirmed -> {
            if (Boolean.TRUE.equals(confirmed)) {
                player.closeInventory();
                UtilMessage.message(player, "World", "Creating world...");
                final BPvPWorld world = new BPvPWorld(creator.name());
                world.createWorld(creator);
                UtilMessage.message(player, "World", "World created!");
                player.teleport(Objects.requireNonNull(world.getWorld()).getSpawnLocation());
                SoundEffect.HIGH_PITCH_PLING.play(player);
            } else {
                SoundEffect.LOW_PITCH_PLING.play(player);
            }
            player.closeInventory();
        }).show(player);
    }
}
