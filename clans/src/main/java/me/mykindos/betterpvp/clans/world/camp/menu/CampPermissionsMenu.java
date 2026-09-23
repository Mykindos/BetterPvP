package me.mykindos.betterpvp.clans.world.camp.menu;

import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * What each rank may do in the camp: one row per rank, one toggle per action. The leader can change any rank but
 * their own, which may always do everything. Everyone else can look.
 */
public class CampPermissionsMenu extends AbstractGui implements Windowed {

    private static final List<ClanMember.MemberRank> RANKS = List.of(
            ClanMember.MemberRank.LEADER, ClanMember.MemberRank.ADMIN,
            ClanMember.MemberRank.MEMBER, ClanMember.MemberRank.RECRUIT);

    private final long clanId;
    private final CampPermissions permissions;
    private final boolean editable;

    public CampPermissionsMenu(long clanId, @NotNull CampPermissions permissions, boolean editable) {
        super(9, RANKS.size());
        this.clanId = clanId;
        this.permissions = permissions;
        this.editable = editable;
        populate();
    }

    private void populate() {
        for (int row = 0; row < RANKS.size(); row++) {
            final ClanMember.MemberRank rank = RANKS.get(row);
            setItem(row * 9, new SimpleItem(ItemView.builder()
                    .material(Material.NAME_TAG)
                    .displayName(rankName(rank).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                    .build()));

            final ConstructionAction[] actions = ConstructionAction.values();
            for (int column = 0; column < actions.length && column < 8; column++) {
                refresh(row * 9 + column + 1, rank, actions[column]);
            }
        }
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private void refresh(int slot, @NotNull ClanMember.MemberRank rank, @NotNull ConstructionAction action) {
        final boolean allowed = permissions.granted(clanId, rank).contains(action);
        final boolean changeable = editable && rank != ClanMember.MemberRank.LEADER;
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(allowed ? Material.LIME_DYE : Material.GRAY_DYE)
                .displayName(actionName(action).color(allowed ? NamedTextColor.GREEN : NamedTextColor.RED))
                .frameLore(true)
                .lore(Translations.component(allowed ? "clans.camp.menu.permissions.allowed" : "clans.camp.menu.permissions.denied")
                        .color(NamedTextColor.GRAY));
        if (changeable) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.menu.permissions.toggle"));
        } else if (rank == ClanMember.MemberRank.LEADER) {
            view.lore(Translations.component("clans.camp.menu.permissions.leader_always").color(NamedTextColor.GRAY));
        }

        setItem(slot, new SimpleItem(view.build(), click -> {
            if (!changeable) {
                return;
            }
            permissions.set(clanId, rank, action, !allowed);
            refresh(slot, rank, action);
        }));
    }

    private static @NotNull Component rankName(@NotNull ClanMember.MemberRank rank) {
        return Translations.component("clans.camp.rank." + rank.name().toLowerCase(Locale.ROOT));
    }

    private static @NotNull Component actionName(@NotNull ConstructionAction action) {
        return Translations.component("clans.camp.action." + action.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.menu.permissions.title");
    }
}
