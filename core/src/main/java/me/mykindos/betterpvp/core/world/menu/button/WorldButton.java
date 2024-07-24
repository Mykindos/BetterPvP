package me.mykindos.betterpvp.core.world.menu.button;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.chat.ChatCallbacks;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.button.DescriptionButton;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.menu.GuiWorldManager;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Getter
public final class WorldButton extends DescriptionButton {

    private final BPvPWorld world;
    private final ChatCallbacks chatCallbacks;
    private final WorldHandler worldHandler;

    public WorldButton(BPvPWorld world, ChatCallbacks chatCallbacks, WorldHandler worldHandler) {
        super(world::getDescription);
        this.world = world;
        this.chatCallbacks = chatCallbacks;
        this.worldHandler = worldHandler;
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder builder = ItemView.of(super.getItemProvider().get()).toBuilder();
        if (world.isLoaded()) {
            builder.action(ClickActions.LEFT, Component.text("Teleport"));
        }

        return builder.action(ClickActions.RIGHT, Component.text(world.isLoaded() ? "Unload" : "Load"))
                .action(ClickActions.RIGHT_SHIFT, Component.text("Delete"))
                .action(ClickActions.LEFT_SHIFT, Component.text("Duplicate"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (clickType.isLeftClick()) {
            handleLeftClick(clickType, player);
        } else if (clickType.isRightClick()) {
            handleRightClick(clickType, player);
        }
    }

    private void handleLeftClick(ClickType clickType, Player player) {
        if (clickType.isShiftClick()) {
            duplicateWorld(player);
        } else if (world.isLoaded()) {
            teleportPlayer(player);
        }
    }

    private void handleRightClick(ClickType clickType, Player player) {
        if (clickType.isShiftClick()) {
            deleteWorldConfirmation(player);
        } else {
            toggleWorldLoad(player);
        }
    }

    private void duplicateWorld(Player player) {
        player.closeInventory();
        UtilMessage.message(player, "World", "Enter the name of the new world:");
        this.chatCallbacks.listen(player, message -> UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
            final String worldName = (message instanceof TextComponent textComponent) ? textComponent.content() : message.toString();
            if (isWorldNameValid(worldName, player)) {
                createDuplicateWorld(worldName, player);
            }
        }));
    }

    private boolean isWorldNameValid(String worldName, Player player) {
        final NamespacedKey namespace = NamespacedKey.fromString(worldName.toLowerCase().replace(" ", "_"));
        if (namespace == null) {
            UtilMessage.message(player, "World", "Invalid world name.");
            return false;
        }

        if (Bukkit.getWorld(worldName) != null || Bukkit.getWorld(Objects.requireNonNull(namespace)) != null) {
            UtilMessage.message(player, "World", "A world with that name already exists.");
            return false;
        }

        return true;
    }

    private void createDuplicateWorld(String worldName, Player player) {
        UtilMessage.message(player, "World", "Duplicating world...");
        final BPvPWorld newWorld = world.duplicate(worldName);
        newWorld.createWorld();
        UtilMessage.message(player, "World", "World duplicated!");
        SoundEffect.HIGH_PITCH_PLING.play(player);
        new GuiWorldManager(worldHandler, chatCallbacks, null).show(player);
    }

    private void teleportPlayer(Player player) {
        player.closeInventory();
        player.teleport(Objects.requireNonNull(world.getWorld()).getSpawnLocation());
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    private void deleteWorldConfirmation(Player player) {
        new ConfirmationMenu("Are you sure you want to delete " + world.getName() + "?", success -> {
            player.closeInventory();
            if (Boolean.TRUE.equals(success)) {
                deleteWorld(player);
            } else {
                SoundEffect.LOW_PITCH_PLING.play(player);
            }
        }).show(player);
    }

    private void deleteWorld(Player player) {
        worldHandler.deleteWorld(world);
        SoundEffect.HIGH_PITCH_PLING.play(player);
        UtilMessage.message(player, "World", "Deleted <alt2>" + world.getName() + "</alt2>.");
    }

    private void toggleWorldLoad(Player player) {
        if (world.isLoaded()) {
            world.unloadWorld();
            SoundEffect.HIGH_PITCH_PLING.play(player);
            UtilMessage.message(player, "World", "Unloaded <alt2>" + world.getName() + "</alt2>.");
            notifyWindows();
        } else {
            world.createWorld();
            SoundEffect.HIGH_PITCH_PLING.play(player);
            UtilMessage.message(player, "World", "Loaded <alt2>" + world.getName() + "</alt2>.");
            notifyWindows();
        }
    }
}
