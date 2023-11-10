package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.EnergyMenu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.NumberFormat;

@Getter
public class EnergyButton extends AbstractItem {

    private final Clan clan;
    private final boolean allowShop;
    private final ItemProvider itemProvider;
    private final Windowed parent;

    public EnergyButton(boolean allowShop, Clan clan, Windowed parent) {
        this.parent = parent;
        final TextComponent currentEnergy = Component.text("Current Energy: ", NamedTextColor.GRAY)
                .append(Component.text(NumberFormat.getInstance().format(clan.getEnergy()), NamedTextColor.YELLOW));

        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.NETHER_STAR)
                .displayName(Component.text("Energy", NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(currentEnergy);

        if (!clan.getTerritory().isEmpty()) {
            builder.lore(Component.text("Disbands in: ", NamedTextColor.GRAY).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.YELLOW)));
        }

        if (allowShop) {
            builder.action(ClickActions.ALL, Component.text("Open Energy Shop"));
        }

        this.allowShop = allowShop;
        this.clan = clan;
        this.itemProvider = builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!allowShop) {
            return;
        }

        new EnergyMenu(clan, parent).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
