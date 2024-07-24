package me.mykindos.betterpvp.clans.clans.menus.buttons.banner;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.events.ClanBannerUpdateEvent;
import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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

public class SaveItem extends ControlItem<BannerMenu> implements CooldownButton {

    private final Clan clan;

    public SaveItem(Clan clan) {
        this.clan = clan;
    }

    @Override
    public ItemProvider getItemProvider(BannerMenu gui) {
        return ItemView.builder()
                .material(Material.EMERALD)
                .displayName(Component.text("Save", NamedTextColor.GREEN, TextDecoration.BOLD))
                .action(ClickActions.ALL, Component.text("Save"))
                .build();
    }

    @Override
    public double getCooldown() {
        return 1;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        BannerWrapper banner = getGui().getBuilder().build();
        clan.setBanner(banner);
        UtilServer.callEvent(new ClanBannerUpdateEvent(clan));
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
