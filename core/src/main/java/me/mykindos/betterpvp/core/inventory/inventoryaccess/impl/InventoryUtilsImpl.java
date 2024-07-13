package me.mykindos.betterpvp.core.inventory.inventoryaccess.impl;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.InventoryUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftContainer;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
public class InventoryUtilsImpl implements InventoryUtils {

    public static Component createNMSComponent(ComponentWrapper component) {
        if (component == null) return null;
        return CraftChatMessage.fromJSON(component.serializeToJson());
    }

    public static int getActiveWindowId(ServerPlayer player) {
        AbstractContainerMenu container = player.containerMenu;
        return container == null ? -1 : container.containerId;
    }

    @Override
    public void openCustomInventory(@NotNull Player player, @NotNull Inventory inventory) {
        openCustomInventory(player, inventory, null);
    }

    @Override
    public void openCustomInventory(@NotNull Player player, @NotNull Inventory inventory, @Nullable ComponentWrapper title) {

        try {
            if (!Bukkit.isPrimaryThread()) {
                log.error("Cannot open a menu on a secondary thread! This can lead to very bad things happening!");
                throw new RuntimeException("Cannot open a menu on a secondary thread!");
            }

            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            MenuType<?> menuType = CraftContainer.getNotchInventoryType(inventory);

            if (serverPlayer.connection != null) {
                AbstractContainerMenu menu = new CraftContainer(inventory, serverPlayer, serverPlayer.nextContainerCounter());
                menu = CraftEventFactory.callInventoryOpenEvent(serverPlayer, menu);
                if (menu != null) {
                    Container container = ((CraftInventory) inventory).getInventory();
                    Component titleComponent;
                    if (title == null) {
                        if (container instanceof MenuProvider)
                            titleComponent = ((MenuProvider) container).getDisplayName();
                        else titleComponent = CraftChatMessage.fromString(menu.getBukkitView().getTitle())[0];
                    } else titleComponent = createNMSComponent(title);

                    menu.checkReachable = false;
                    serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menuType, titleComponent));
                    serverPlayer.containerMenu = menu;
                    serverPlayer.initMenu(menu);
                }
            }
        } catch (Exception ex) {
            player.closeInventory();
            log.error("Failed to open inventory for player " + player.getName(), ex);
        }

    }

    @Override
    public void updateOpenInventoryTitle(@NotNull Player player, @NotNull ComponentWrapper title) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        AbstractContainerMenu menu = serverPlayer.containerMenu;
        serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menu.getType(), createNMSComponent(title)));
        serverPlayer.initMenu(menu);
    }

    @Override
    public @Nullable ItemStack getItemStackFromView(@NotNull InventoryView view, int slot) {
        return view.getItem(slot);
    }

}
