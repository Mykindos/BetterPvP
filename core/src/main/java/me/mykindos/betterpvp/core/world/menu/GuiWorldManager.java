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
import me.mykindos.betterpvp.core.world.menu.button.FolderButton;
import me.mykindos.betterpvp.core.world.menu.button.WorldButton;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class GuiWorldManager extends ViewCollectionMenu {

    private final WorldHandler worldHandler;
    private final ChatCallbacks chatCallbacks;
    private String currentPath = "";

    public GuiWorldManager(@NotNull WorldHandler worldHandler, @NotNull ChatCallbacks chatCallbacks, @Nullable Windowed previous) {
        super("World Manager", new ArrayList<>(), previous);
        this.chatCallbacks = chatCallbacks;
        this.worldHandler = worldHandler;

        refresh();
    }

    private void refresh() {
        final List<Item> items = new ArrayList<>();
        final Set<BPvPWorld> allWorlds = worldHandler.getWorlds();

        final Set<String> foldersInPath = new HashSet<>();
        final List<BPvPWorld> worldsInPath = new ArrayList<>();

        for (BPvPWorld world : allWorlds) {
            String name = world.getName();
            if (currentPath.isEmpty()) {
                if (name.contains("/")) {
                    foldersInPath.add(name.split("/")[0]);
                } else {
                    worldsInPath.add(world);
                }
            } else {
                if (name.startsWith(currentPath + "/")) {
                    String relative = name.substring(currentPath.length() + 1);
                    if (relative.contains("/")) {
                        foldersInPath.add(relative.split("/")[0]);
                    } else {
                        worldsInPath.add(world);
                    }
                }
            }
        }

        for (String folder : foldersInPath.stream().sorted().toList()) {
            items.add(new FolderButton(folder, player -> {
                this.currentPath = currentPath.isEmpty() ? folder : currentPath + "/" + folder;
                refresh();
            }));
        }

        for (BPvPWorld world : worldsInPath) {
            items.add(new WorldButton(world, chatCallbacks, worldHandler) {
                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
                    super.handleClick(clickType, player, inventoryClickEvent);
                    if (!clickType.isLeftClick() || clickType.isShiftClick()) {
                        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                            refresh();
                            notifyWindows();
                        }, 1L);
                    }
                }
            });
        }

        setContent(items);

        setItem(getSize() - 1, new SimpleItem(ItemView.builder()
                .displayName(Component.text("Create World", NamedTextColor.GREEN, TextDecoration.BOLD))
                .material(Material.GREEN_CONCRETE)
                .build(), click -> createWorld(click.getPlayer())));
    }

    @Override
    public void handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType, @NotNull InventoryClickEvent event) {
        if (slot == 31 && !currentPath.isEmpty()) { // Back button slot in ViewCollectionMenu
            if (currentPath.contains("/")) {
                currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
            } else {
                currentPath = "";
            }
            refresh();
            return;
        }
        super.handleClick(slot, player, clickType, event);
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
