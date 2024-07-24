package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@RequiredArgsConstructor
public class ClanMemberButton extends AbstractItem {

    private final Clan clan;
    private final ClanMember member;
    private final OfflinePlayer player;
    private final boolean detailed;
    private final boolean canEdit;
    private final ClientManager clientManager;
    private String name;

    @SneakyThrows
    @Override
    public ItemProvider getItemProvider() {
        if (name == null) {
            name = player.getName();
            if (name == null) {
                this.clientManager.search().offline(player.getUniqueId(), opt -> {
                    name = opt.map(Client::getName).orElseThrow();
                    notifyWindows();
                });
            }
        }

        final String name = Objects.requireNonNullElse(this.name, "Loading Player...");
        final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        itemStack.setItemMeta(meta);
        ItemView.ItemViewBuilder builder = ItemView.of(itemStack).toBuilder();

        final TextComponent role = Component.text("Role: ", NamedTextColor.WHITE).append(Component.text(member.getRank().getName(), NamedTextColor.GRAY));
        if (canEdit) {
            builder.action(ClickActions.LEFT, Component.text("Promote"));
            builder.action(ClickActions.RIGHT, Component.text("Demote"));
            builder.action(ClickActions.SHIFT, Component.text("Kick"));
        }

        if (!player.isOnline()) {
            return builder
                    .material(Material.SKELETON_SKULL)
                    .displayName(Component.text(name, NamedTextColor.RED, TextDecoration.BOLD))
                    .lore(Component.empty())
                    .lore(role)
                    .lore(Component.empty())
                    .build();
        }

        final Player online = player.getPlayer();
        builder.displayName(Component.text(name, NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(role);

        if (detailed) {
            final Location loc = online.getLocation();
            final String locText = String.format("%,d %,d", loc.getBlockX(), loc.getBlockZ());
            final TextComponent location = Component.text("Location: ", NamedTextColor.WHITE).append(Component.text(locText, NamedTextColor.GRAY));
            builder.lore(location);
        }

        builder.lore(Component.empty());

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!canEdit && name != null) {
            return;
        }

        if (clickType == ClickType.LEFT) {
            player.chat("/c promote " + this.name);
        } else if (clickType == ClickType.RIGHT) {
            player.chat("/c demote " + this.name);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            player.chat("/c kick " + this.name);
            player.closeInventory();
        }

    }
}
