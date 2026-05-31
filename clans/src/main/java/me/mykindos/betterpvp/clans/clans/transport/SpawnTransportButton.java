package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.zone.Zone;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SpawnTransportButton extends ControlItem<ClanTravelHubMenu> {

    private final Zone spawn;
    private final Client client;
    private final Material material;
    private final NamedTextColor namedTextColor;

    public SpawnTransportButton(Zone spawn, Client client, Material material, NamedTextColor namedTextColor) {
        this.spawn = spawn;
        this.client = client;
        this.material = material;
        this.namedTextColor = namedTextColor;
    }

    @Override
    public ItemProvider getItemProvider(ClanTravelHubMenu menu) {
        ItemView.ItemViewBuilder provider = ItemView.builder()
                .material(material)
                .itemModel(Key.key("betterpvp", "menu/gui/waystone/button"))
                .displayName(Component.text("Spawn", namedTextColor, TextDecoration.BOLD))
                .action(ClickActions.LEFT, Component.text("Teleport"));
        if (menu.isPressed(this)) {
            provider.itemModel(Key.key("betterpvp", "menu/gui/waystone/button_selected"));
        }
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (!clickType.isLeftClick()) {
            return;
        }

        getGui().beginTeleport(this, () -> teleportToSpawn(player));
    }

    /**
     * Teleports the player to the spawn {@link #spawn zone}. Wired into the shared 0.5s teleport buffer in
     * {@link ClanTravelHubMenu#beginTeleport}; this method runs once the buffer elapses.
     */
    private void teleportToSpawn(@NotNull Player player) {
        final WorldHandler worldHandler = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(WorldHandler.class);
        final Location spawnLocation = Objects.requireNonNull(worldHandler.getSpawnLocation()).clone();
        spawnLocation.add(0, 0.01, 0); // epsilon to prevent phasing into the ground
        player.teleportAsync(spawnLocation);
        player.closeInventory();
    }
}
