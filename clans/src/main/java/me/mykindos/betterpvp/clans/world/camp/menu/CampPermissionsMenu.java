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
 * What each rank may do in the camp, one row per rank with one toggle per action, and a last row for members of
 * allied clans that also decides whether they may open containers. The leader can change any row but their own,
 * which may always do everything. Everyone else can look.
 */
public class CampPermissionsMenu extends AbstractGui implements Windowed {

    private static final List<ClanMember.MemberRank> RANKS = List.of(
            ClanMember.MemberRank.LEADER, ClanMember.MemberRank.ADMIN,
            ClanMember.MemberRank.MEMBER, ClanMember.MemberRank.RECRUIT);

    private final long clanId;
    private final CampPermissions permissions;
    private final boolean editable;

    public CampPermissionsMenu(long clanId, @NotNull CampPermissions permissions, boolean editable) {
        super(9, RANKS.size() + 1);
        this.clanId = clanId;
        this.permissions = permissions;
        this.editable = editable;
        populate();
    }

    private void populate() {
        final ConstructionAction[] actions = ConstructionAction.values();
        for (int row = 0; row < RANKS.size(); row++) {
            final ClanMember.MemberRank rank = RANKS.get(row);
            setItem(row * 9, label(Translations.component("clans.camp.rank." + rank.name().toLowerCase(Locale.ROOT))));
            for (int column = 0; column < actions.length && column < 8; column++) {
                refreshRank(row * 9 + column + 1, rank, actions[column]);
            }
        }

        final int allyRow = RANKS.size() * 9;
        setItem(allyRow, label(Translations.component("clans.camp.rank.ally")));
        for (int column = 0; column < actions.length && column < 7; column++) {
            refreshAlly(allyRow + column + 1, actions[column]);
        }
        refreshAllyContainers(allyRow + 8);
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private void refreshRank(int slot, @NotNull ClanMember.MemberRank rank, @NotNull ConstructionAction action) {
        final boolean allowed = permissions.granted(clanId, rank).contains(action);
        final boolean leader = rank == ClanMember.MemberRank.LEADER;
        toggle(slot, actionName(action), allowed, editable && !leader, leader, () -> {
            permissions.set(clanId, rank, action, !allowed);
            refreshRank(slot, rank, action);
        });
    }

    private void refreshAlly(int slot, @NotNull ConstructionAction action) {
        final boolean allowed = permissions.allyActions(clanId).contains(action);
        toggle(slot, actionName(action), allowed, editable, false, () -> {
            permissions.setAlly(clanId, action, !allowed);
            refreshAlly(slot, action);
        });
    }

    private void refreshAllyContainers(int slot) {
        final boolean allowed = permissions.allyContainers(clanId);
        toggle(slot, Translations.component("clans.camp.menu.permissions.containers"), allowed, editable, false, () -> {
            permissions.setAllyContainers(clanId, !allowed);
            refreshAllyContainers(slot);
        });
    }

    private void toggle(int slot, @NotNull Component name, boolean allowed, boolean changeable, boolean always,
                        @NotNull Runnable flip) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(allowed ? Material.LIME_DYE : Material.GRAY_DYE)
                .displayName(name.color(allowed ? NamedTextColor.GREEN : NamedTextColor.RED))
                .frameLore(true)
                .lore(Translations.component(allowed ? "clans.camp.menu.permissions.allowed" : "clans.camp.menu.permissions.denied")
                        .color(NamedTextColor.GRAY));
        if (changeable) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.menu.permissions.toggle"));
        } else if (always) {
            view.lore(Translations.component("clans.camp.menu.permissions.leader_always").color(NamedTextColor.GRAY));
        }

        setItem(slot, new SimpleItem(view.build(), click -> {
            if (changeable) {
                flip.run();
            }
        }));
    }

    private static @NotNull SimpleItem label(@NotNull Component name) {
        return new SimpleItem(ItemView.builder()
                .material(Material.NAME_TAG)
                .displayName(name.color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .build());
    }

    private static @NotNull Component actionName(@NotNull ConstructionAction action) {
        return Translations.component("clans.camp.action." + action.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.menu.permissions.title");
    }
}
