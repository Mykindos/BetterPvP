package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class ClanDetailsButton extends ControlItem<ClanMenu> {

    private final Clan clan;
    private final boolean admin;
    private final Clan viewerClan;
    private final ClanRelation viewerRelation;

    public ClanDetailsButton(final boolean admin, final Clan clan, final Clan viewerClan, final ClanRelation viewerRelation) {
        this.viewerClan = viewerClan;
        this.viewerRelation = viewerRelation;
        this.clan = clan;
        this.admin = admin;
    }

    @Override
    public ItemProvider getItemProvider(final ClanMenu gui) {
        double netDominance = this.clan.getEnemies().stream().mapToDouble(ClanEnemy::getDominance).sum();
        for (final ClanEnemy enemy : this.clan.getEnemies()) {
            final Optional<ClanEnemy> theirEnemyOptional = enemy.getClan().getEnemy(this.clan);
            if (theirEnemyOptional.isPresent()) {
                netDominance -= theirEnemyOptional.get().getDominance();
            }
        }

        final String netDominanceText = String.format("%.1f%%", netDominance);

        final ItemView.ItemViewBuilder builder = ItemView.of(this.clan.getBanner().get()).toBuilder()
                .frameLore(true)
                .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                .displayName(Component.text(this.clan.getName(), this.viewerRelation.getSecondary()))
                .lore(Component.text("Net Dominance: ", NamedTextColor.GRAY).append(Component.text(netDominanceText, netDominance >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .lore(UtilMessage.deserialize("<gray>Online: <white>%,d</white>/<white>%,d</white>", this.clan.getOnlineMemberCount(), this.clan.getMembers().size()))
                .lore(UtilMessage.deserialize("<gray>Squad Size: <white>%d</white>", this.clan.getSquadCount()));

        if (this.viewerRelation == ClanRelation.ENEMY) {
            double dominance;
            final Optional<ClanEnemy> enemyOptional = this.viewerClan.getEnemy(this.clan);
            if(enemyOptional.isPresent()) {
                dominance = enemyOptional.get().getDominance();

                if(dominance == 0) {
                    final Optional<ClanEnemy> theirEnemyOptional = this.clan.getEnemy(this.viewerClan);
                    if(theirEnemyOptional.isPresent()) {
                        dominance = theirEnemyOptional.get().getDominance() * -1;
                    }
                }

                builder.lore(UtilMessage.deserialize("<gray>Dominance: %s%.1f", dominance >= 0 ? "<green>" : "<red>", dominance));
            }

        }

        if (this.admin) {
            builder.action(ClickActions.ALL, Component.text("Edit Banner"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull final ClickType clickType, @NotNull final Player player, @NotNull final InventoryClickEvent event) {
        if (!this.admin) {
            return;
        }

        new BannerMenu(this.clan, this.getGui(), this::notifyWindows).show(player);
    }
}
