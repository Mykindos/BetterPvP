package me.mykindos.betterpvp.clans.logging.button;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.KillClanLog;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClanKillLogButton extends AbstractItem {

    private final KillClanLog killClanLog;
    private final ClanRelation killerRelation;
    private final ClanRelation victimRelation;

    public ClanKillLogButton(Clan clan, KillClanLog killClanLog, ClanManager clanManager) {
        this.killClanLog = killClanLog;

        Clan killerClan = clanManager.getClanById(killClanLog.getKillerClan()).orElse(null);
        Clan victimClan = clanManager.getClanById(killClanLog.getVictimClan()).orElse(null);

        killerRelation = clan.getRelation(killerClan);
        victimRelation = clan.getRelation(victimClan);
    }

    @Override
    public ItemProvider getItemProvider() {
        Component name = Component.text(killClanLog.getKillerName(), killerRelation.getPrimary())
                            .append(Component.text(" killed ", NamedTextColor.GRAY))
                            .append(Component.text(killClanLog.getVictimName(), victimRelation.getPrimary()));
        List<Component> lore = getLore();
        ItemStack itemStack = ItemStack.of(Material.TIPPED_ARROW);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(Color.fromRGB(killerRelation.getSecondary().value()));
        }
        return ItemView.builder()
                .material(Material.TIPPED_ARROW)
                .baseMeta(itemMeta)
                .displayName(name)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .lore(lore)
                .build();
    }

    @NotNull
    private List<Component> getLore() {
        double dominance = killClanLog.getDominance();
        NamedTextColor dominanceColor = dominance > 0 ? NamedTextColor.RED : NamedTextColor.GREEN;

        return List.of(
                Component.text(killClanLog.getKillerClanName(), killerRelation.getSecondary())
                        .appendSpace()
                        .append(Component.text(killClanLog.getKillerName(), killerRelation.getPrimary())),
                Component.text("killed", NamedTextColor.GRAY),
                Component.text(killClanLog.getVictimClanName(), victimRelation.getSecondary())
                        .appendSpace()
                        .append(Component.text(killClanLog.getVictimName(), victimRelation.getPrimary())),
                Component.text("for ", NamedTextColor.GRAY).append(Component.text(dominance, dominanceColor))
                        .append(Component.text(" dominance", NamedTextColor.GRAY))
        );
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        //unused
    }
}
