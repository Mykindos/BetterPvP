package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.OptionalInt;

@Getter
public class EnergyButton extends AbstractItem {

    private final Clan clan;
    private final Windowed parent;
    private final boolean deposit;

    public EnergyButton(Clan clan, boolean deposit, Windowed parent) {
        this.clan = clan;
        this.parent = parent;
        this.deposit = deposit;
    }

    @Override
    public ItemProvider getItemProvider() {
        final int energy = clan.getEnergy();
        final String currentEnergy = NumberFormat.getInstance().format(energy);

        final Component progressText = Component.text("Current Energy:", NamedTextColor.GRAY)
                .appendSpace()
                .append(Component.text(currentEnergy, NamedTextColor.YELLOW));

        final TextColor highlight = TextColor.color(227, 156, 255);
        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.PAPER)
                .customModelData(4)
                .displayName(Component.text("Energy", TextColor.color(179, 79, 255), TextDecoration.BOLD))
                .frameLore(true)
                .lore(Component.text("Energy is required to upkeep your", NamedTextColor.GRAY))
                .lore(Component.text("clan core and territory. Without it", NamedTextColor.GRAY))
                .lore(Component.text("your clan will disband.", NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(progressText)
                .lore(Component.text("Disbands in: ", NamedTextColor.GRAY).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.YELLOW)))
                .lore(Component.empty())
                .lore(Component.text("To get more energy, you can:", NamedTextColor.GRAY))
                .lore(Component.text("\u25AA ").append(Component.text("Kill other players", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Complete dungeons and raids", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Mine it in the world or at Fields", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Participate in world events", highlight)));

        if (deposit) {
            builder.action(ClickActions.ALL, Component.text("Deposit Energy Item on Cursor"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!deposit) {
            return;
        }

        final OptionalInt energyOpt = EnergyItem.getEnergyAmount(event.getCursor(), false);
        if (energyOpt.isEmpty()) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        //noinspection deprecation
        event.setCursor(null);
        final int energy = energyOpt.getAsInt();
        clan.grantEnergy(energy);
        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 2f).play(player);
        notifyWindows();
    }
}
