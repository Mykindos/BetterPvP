package me.mykindos.betterpvp.clans.clans.menus.buttons;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.Optional;

@Getter
public class ClanInformationButton extends AbstractItem {

    private final ItemProvider itemProvider;

    public ClanInformationButton(Clan clan, ClanRelation viewerRelation) {
        final double netDominance = clan.getEnemies().stream().mapToDouble(ClanEnemy::getDominance).sum();
        String netDominanceText = String.format("%.1f%%", netDominance);

        Component tntProtectionCmpt;
        Optional<Long> tntProtectionOptional = clan.getProperty(ClanProperty.TNT_PROTECTION);
        if (tntProtectionOptional.isPresent()) {
            long tntProtection = tntProtectionOptional.get();
            if (tntProtection > System.currentTimeMillis()) {
                final String time = UtilTime.getTime2(tntProtection - System.currentTimeMillis(), UtilTime.TimeUnit.MINUTES, 1);
                tntProtectionCmpt = Component.text("Active in " + time, NamedTextColor.RED);
            } else {
                tntProtectionCmpt = Component.text("Yes", NamedTextColor.GREEN);
            }
        } else {
            tntProtectionCmpt = Component.text("No - Clan is online", NamedTextColor.RED);
        }

        this.itemProvider = ItemView.of(clan.getBanner()).toBuilder()
                .frameLore(true)
                .displayName(Component.text(clan.getName(), viewerRelation.getSecondary()))
                .lore(Component.text("Net Dominance: ", NamedTextColor.GRAY).append(Component.text(netDominanceText, netDominance >= 0 ? NamedTextColor.GREEN : NamedTextColor.DARK_PURPLE)))
                .lore(Component.text("TNT Protection: ", NamedTextColor.GRAY).append(tntProtectionCmpt))
                .lore(UtilMessage.deserialize("<gray>Online: <white>%,d</white>/<white>%,d</white>", clan.getOnlineMemberCount(), clan.getMembers().size()))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Ignored
    }
}
