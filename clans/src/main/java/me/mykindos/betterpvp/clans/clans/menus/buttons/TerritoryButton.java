package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class TerritoryButton extends AbstractItem {

    private final boolean admin;
    private final Clan clan;

    public TerritoryButton(boolean admin, Clan clan) {
        this.admin = admin;
        this.clan = clan;
    }

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder builder = ItemView.builder().material(Material.PAPER).customModelData(3)
                .displayName(Component.text("Territory", NamedTextColor.DARK_GREEN))
                .lore(UtilMessage.deserialize("<white>%d</white>/%d claimed", clan.getTerritory().size(), Math.min(clan.getMembers().size() + 3, 9)))
                .frameLore(true);

        if (admin) {
            builder.action(ClickActions.LEFT, Component.text("Claim Territory"));
            builder.action(ClickActions.RIGHT, Component.text("Unclaim Territory"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!admin) {
            return;
        }

        if (ClickActions.LEFT.accepts(clickType)) {
            player.chat("/clan claim");
            notifyWindows();
        } else if (ClickActions.RIGHT.accepts(clickType)) {
            player.chat("/clan unclaim");
            notifyWindows();
        }

    }
}
