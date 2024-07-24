package me.mykindos.betterpvp.clans.clans.menus;


import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ClanDetailsButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ClanHomeButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ClanMemberButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ClanProgressionButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.EnergyButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.LeaveClanButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.TerritoryButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ViewAlliancesButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.ViewEnemiesButton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClanMenu extends AbstractGui implements Windowed {

    private static final int[] MEMBER_SLOTS = { 19, 20, 21, 22, 23, 24, 25, 26 };
    private static final SimpleItem EMPTY_MEMBER_SLOT = new SimpleItem(ItemView.builder()
            .material(Material.RED_STAINED_GLASS_PANE)
            .displayName(Component.empty())
            .lore(Component.text("EMPTY SLOT!", NamedTextColor.RED))
            .lore(Component.empty())
            .build());

    private final Player viewer;
    private final Clan viewerClan;
    private final Clan clan;
    private final ClientManager clientManager;

    public ClanMenu(Player viewer, Clan viewerClan, Clan clan) {
        super(9, 5);
        this.viewerClan = viewerClan;
        this.clan = clan;
        this.viewer = viewer;
        this.clientManager = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClientManager.class);

        populate();
    }

    public void populate() {
        boolean ownClan = viewerClan != null && viewerClan.getId().equals(clan.getId());
        boolean admin = ownClan && viewerClan.getMemberByUUID(viewer.getUniqueId()).map(member -> member.getRank().hasRank(ClanMember.MemberRank.ADMIN)).orElse(false);
        boolean leader = ownClan && viewerClan.getMemberByUUID(viewer.getUniqueId()).map(member -> member.getRank().hasRank(ClanMember.MemberRank.LEADER)).orElse(false);

        // Top row global buttons
        setItem(0, new ViewEnemiesButton(clan, this, viewerClan));
        setItem(2, new ClanProgressionButton(clan));
        setItem(4, new TerritoryButton(admin, clan));
        setItem(6, new EnergyButton(clan, false, this));
        setItem(8, new ViewAlliancesButton(clan, this, viewerClan));

        // Middle row - member and clan information
        setItem(18, new ClanDetailsButton(admin, clan, viewerClan, clan.getRelation(viewerClan)));
        addMemberButtons(ownClan);

        // Bottom row buttons (only viewable by clan members)
        if (ownClan) {
            setItem(38, new ClanHomeButton(admin));
            setItem(42, new LeaveClanButton(leader));
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    private void addMemberButtons(boolean detailed) {
        final Optional<ClanMember.MemberRank> optRank = clan.getMemberByUUID(viewer.getUniqueId()).map(ClanMember::getRank);
        final boolean admin = optRank.isPresent() && optRank.map(rank -> rank.hasRank(ClanMember.MemberRank.ADMIN)).orElse(false);
        final Map<ClanMember, OfflinePlayer> members = clan.getMembers().stream().collect(Collectors.toMap(
                Function.identity(), member -> Bukkit.getOfflinePlayer(UUID.fromString(member.getUuid()))));

        final ArrayList<ClanMember> sorted = new ArrayList<>(members.keySet());
        sorted.sort((m1, m2) -> {
            final OfflinePlayer p1 = members.get(m1);
            final OfflinePlayer p2 = members.get(m2);
            if (p1.isOnline() != p2.isOnline()) {
                return Boolean.compare(p2.isOnline(), p1.isOnline());
            }

            return Integer.compare(m2.getRank().getPrivilege(), m1.getRank().getPrivilege());
        });

        final Iterator<ClanMember> iterator = sorted.iterator();
        for (final int slot : MEMBER_SLOTS) {
            if (!iterator.hasNext()) {
                setItem(slot, EMPTY_MEMBER_SLOT);
            } else {
                final ClanMember member = iterator.next();
                final OfflinePlayer player = members.get(member);
                final boolean canEdit = admin && member.getRank().getPrivilege() < optRank.orElseThrow().getPrivilege();

                setItem(slot, new ClanMemberButton(clan, member, player, detailed, canEdit, clientManager));
            }
        }
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("View " + clan.getName());
    }
}
