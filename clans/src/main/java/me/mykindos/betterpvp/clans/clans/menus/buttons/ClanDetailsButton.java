package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

import java.util.Optional;

@Getter
public class ClanDetailsButton extends ControlItem<ClanMenu> {

    private final Clan clan;
    private final boolean admin;
    private final Clan viewerClan;
    private final ClanRelation viewerRelation;

    public ClanDetailsButton(boolean admin, Clan clan, Clan viewerClan, ClanRelation viewerRelation) {
        this.viewerClan = viewerClan;
        this.viewerRelation = viewerRelation;
        this.clan = clan;
        this.admin = admin;
    }

    @Override
    public ItemProvider getItemProvider(ClanMenu gui) {
        double netDominance = clan.getEnemies().stream().mapToDouble(ClanEnemy::getDominance).sum();
        for (ClanEnemy enemy : clan.getEnemies()) {
            Optional<ClanEnemy> theirEnemyOptional = enemy.getClan().getEnemy(clan);
            if (theirEnemyOptional.isPresent()) {
                netDominance -= theirEnemyOptional.get().getDominance();
            }
        }

        String netDominanceText = String.format("%.1f%%", netDominance);

        Component tntProtectionCmpt;
        Optional<Long> tntProtectionOptional = clan.getProperty(ClanProperty.TNT_PROTECTION);
        if (tntProtectionOptional.isPresent()) {
            long tntProtection = tntProtectionOptional.get();
            if (tntProtection > System.currentTimeMillis()) {
                final String time = UtilTime.getTime2((tntProtection - System.currentTimeMillis()), UtilTime.TimeUnit.MINUTES, 1);
                tntProtectionCmpt = Component.text("Active in " + time, NamedTextColor.RED);
            } else {
                tntProtectionCmpt = Component.text("Yes", NamedTextColor.GREEN);
            }
        } else {
            tntProtectionCmpt = Component.text("No - Clan is online", NamedTextColor.RED);
        }

        final ItemView.ItemViewBuilder builder = ItemView.of(clan.getBanner().get()).toBuilder()
                .frameLore(true)
                .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                .displayName(Component.text(clan.getName(), viewerRelation.getSecondary()))
                .lore(Component.text("Net Dominance: ", NamedTextColor.GRAY).append(Component.text(netDominanceText, netDominance >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .lore(Component.text("TNT Protection: ", NamedTextColor.GRAY).append(tntProtectionCmpt))
                .lore(UtilMessage.deserialize("<gray>Online: <white>%,d</white>/<white>%,d</white>", clan.getOnlineMemberCount(), clan.getMembers().size()))
                .lore(UtilMessage.deserialize("<gray>Squad Size: <white>%d</white>", clan.getSquadCount()));

        if(viewerRelation == ClanRelation.ENEMY) {
            double dominance;
            Optional<ClanEnemy> enemyOptional = viewerClan.getEnemy(clan);
            if(enemyOptional.isPresent()) {
                dominance = enemyOptional.get().getDominance();

                if(dominance == 0) {
                    Optional<ClanEnemy> theirEnemyOptional = clan.getEnemy(viewerClan);
                    if(theirEnemyOptional.isPresent()) {
                        dominance = theirEnemyOptional.get().getDominance() * -1;
                    }
                }

                builder.lore(UtilMessage.deserialize("<gray>Dominance: %s%.1f", dominance >= 0 ? "<green>" : "<red>", dominance));
            }

        }

        if (admin) {
            builder.action(ClickActions.ALL, Component.text("Edit Banner"));
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!admin) {
            return;
        }

        new BannerMenu(clan, getGui(), this::notifyWindows).show(player);
    }
}
