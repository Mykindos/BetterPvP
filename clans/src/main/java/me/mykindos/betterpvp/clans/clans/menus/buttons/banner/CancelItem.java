package me.mykindos.betterpvp.clans.clans.menus.buttons.banner;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class CancelItem extends ControlItem<BannerMenu> {

    private final Clan clan;

    public CancelItem(Clan clan) {
        this.clan = clan;
    }

    @Override
    public ItemProvider getItemProvider(BannerMenu gui) {
        return ItemView.builder()
                .material(Material.TNT)
                .displayName(Component.text("Cancel", NamedTextColor.RED, TextDecoration.BOLD))
                .action(ClickActions.LEFT, Component.text("Reset Changes"))
                .action(ClickActions.SHIFT, Component.text("Wipe Banner"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (ClickActions.LEFT.accepts(clickType)) {
            // Reset
            getGui().setBuilder(clan.getBanner().toBuilder());
            getGui().update();
            SoundEffect.HIGH_PITCH_PLING.play(player);
        } else if (ClickActions.SHIFT.accepts(clickType)) {
            getGui().setBuilder(BannerWrapper.builder());
            getGui().update();
            SoundEffect.HIGH_PITCH_PLING.play(player);
        } else {
            SoundEffect.WRONG_ACTION.play(player);
        }
    }
}
