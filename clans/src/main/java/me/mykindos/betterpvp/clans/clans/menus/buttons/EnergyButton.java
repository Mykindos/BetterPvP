package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
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

        final Component progressText = Translations.component("clans.menu.clan.button.energy.lore.current").color(NamedTextColor.GRAY)
                .appendSpace()
                .append(Component.text(currentEnergy, NamedTextColor.YELLOW));

        final TextColor highlight = TextColor.color(227, 156, 255);
        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/sun_icon"))
                .displayName(Translations.component("clans.menu.clan.button.energy.name").color(TextColor.color(179, 79, 255)).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.menu.clan.button.energy.lore.description.1").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.clan.button.energy.lore.description.2").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.clan.button.energy.lore.description.3").color(NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(progressText)
                .lore(Translations.component("clans.menu.clan.button.energy.lore.disbands").color(NamedTextColor.GRAY)
                        .appendSpace().append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.YELLOW)))
                .lore(Component.empty())
                .lore(Translations.component("clans.menu.clan.button.energy.lore.get-more").color(NamedTextColor.GRAY))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.clan.button.energy.lore.method.kill").color(highlight)))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.clan.button.energy.lore.method.dungeons").color(highlight)))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.clan.button.energy.lore.method.mine").color(highlight)))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.clan.button.energy.lore.method.events").color(highlight)));

        if (deposit) {
            builder.action(ClickActions.ALL, Translations.component("clans.menu.clan.button.energy.action.deposit"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!deposit) {
            return;
        }

        ItemStack energyItem = event.getCursor();
        if(energyItem.getType() != Material.AMETHYST_SHARD) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        int energyAmount = 0;
        final OptionalInt energyOpt = EnergyItem.getEnergyAmount(energyItem, false);
        if (energyOpt.isEmpty()) {
            energyAmount = 30 * energyItem.getAmount();
        }else {
            energyAmount = energyOpt.getAsInt();
        }

        //noinspection deprecation
        event.setCursor(null);
        final int energy = energyAmount;
        clan.grantEnergy(player, energy, "Deposit");

        final Client client = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClientManager.class).search().online(player);
        client.getStatContainer().incrementStat(ClientStat.CLANS_ENERGY_COLLECTED, energy);

        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 2f).play(player);
        notifyWindows();
    }
}
