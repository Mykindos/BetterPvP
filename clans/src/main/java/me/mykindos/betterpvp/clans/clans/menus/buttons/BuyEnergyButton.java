package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.events.ClanBuyEnergyEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public class BuyEnergyButton extends SimpleItem {

    @NotNull
    private final Clan clan;
    private final int energy;
    private final int cost;

    public BuyEnergyButton(@NotNull Clan clan, @NotNull String time, @IntRange(from = 1) int energy, @IntRange(from = 1) int cost) {
        super(ItemView.builder()
                .material(Material.BEACON)
                .displayName(Component.text("Buy " + time, NamedTextColor.GREEN))
                .lore(Component.text("Energy: ", NamedTextColor.GRAY).append(Component.text(String.format("%,d", energy), NamedTextColor.YELLOW)))
                .lore(Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%,d", cost), NamedTextColor.YELLOW)))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("Buy " + time))
                .build());
        this.clan = clan;
        this.energy = energy;
        this.cost = cost;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        UtilServer.callEvent(new ClanBuyEnergyEvent(player, clan, energy, cost));
    }

}
