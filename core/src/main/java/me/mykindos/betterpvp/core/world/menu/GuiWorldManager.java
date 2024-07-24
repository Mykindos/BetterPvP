package me.mykindos.betterpvp.core.world.menu;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.chat.ChatCallbacks;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.menu.button.WorldButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GuiWorldManager extends ViewCollectionMenu {

    private final WorldHandler worldHandler;
    private final ChatCallbacks chatCallbacks;

    public GuiWorldManager(@NotNull WorldHandler worldHandler, @NotNull ChatCallbacks chatCallbacks, @Nullable Windowed previous) {
        super("World Manager", worldHandler.getWorlds().stream()
                .map(world -> new WorldButton(world, chatCallbacks, worldHandler))
                .map(Item.class::cast)
                .toList(), previous);
        this.chatCallbacks = chatCallbacks;
        this.worldHandler = worldHandler;

        setItem(getSize() - 1, new SimpleItem(ItemView.builder()
                .displayName(Component.text("Create World", NamedTextColor.GREEN, TextDecoration.BOLD))
                .material(Material.GREEN_CONCRETE)
                .build(), click -> createWorld(click.getPlayer())));
    }

    private void createWorld(Player player) {
        player.closeInventory();
        UtilMessage.message(player, "World", "Enter the name of the new world:");
        this.chatCallbacks.listen(player, message -> UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
            final String worldName = (message instanceof TextComponent textComponent) ? textComponent.content() : message.toString();
            final NamespacedKey namespace = NamespacedKey.fromString(worldName.toLowerCase().replace(" ", "_"));
            if (namespace == null) {
                UtilMessage.message(player, "World", "Invalid world name.");
                return;
            }

            if (Bukkit.getWorld(worldName) != null || Bukkit.getWorld(Objects.requireNonNull(namespace)) != null) {
                UtilMessage.message(player, "World", "A world with that name already exists.");
                return;
            }

            new GuiCreateWorld(worldHandler, worldName, this).show(player);
        }));
    }

}
