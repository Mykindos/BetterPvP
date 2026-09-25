package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** The Rank lockbox's page: one toggle per clan rank for whether it may open the camp's lockboxes. */
public class RankLockboxMenu extends AbstractGui implements Windowed {

    private static final List<ClanMember.MemberRank> RANKS = List.of(ClanMember.MemberRank.LEADER,
            ClanMember.MemberRank.ADMIN, ClanMember.MemberRank.MEMBER, ClanMember.MemberRank.RECRUIT);

    private final RankLockbox lockbox;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    RankLockboxMenu(@NotNull RankLockbox lockbox, @NotNull Player viewer, @NotNull SiteKey key,
                    @Nullable Windowed previous) {
        super(9, 3);
        this.lockbox = lockbox;
        this.key = key;
        this.previous = previous;

        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.IRON_BARS)
                .displayName(Translations.component("clans.camp.upgrade.rank_lockbox.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .lore(Translations.component("clans.camp.upgrade.rank_lockbox.hint").color(NamedTextColor.GRAY))
                .lore(Translations.component(lockbox.isLeader(viewer, key.getOwnerId())
                        ? "clans.camp.upgrade.rank_lockbox.editable"
                        : "clans.camp.upgrade.rank_lockbox.leader_only").color(NamedTextColor.GRAY))
                .build()));

        for (int i = 0; i < RANKS.size(); i++) {
            setItem(10 + i * 2, rank(RANKS.get(i)));
        }
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem rank(@NotNull ClanMember.MemberRank rank) {
        final boolean allowed = lockbox.ranks(key.getOwnerId()).contains(rank);
        final boolean leader = rank == ClanMember.MemberRank.LEADER;
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(allowed ? Material.LIME_DYE : Material.GRAY_DYE)
                .displayName(Translations.component("clans.camp.rank." + rank.name().toLowerCase(Locale.ROOT))
                        .color(allowed ? NamedTextColor.GREEN : NamedTextColor.RED))
                .lore(Translations.component(allowed
                        ? "clans.camp.upgrade.rank_lockbox.allowed"
                        : "clans.camp.upgrade.rank_lockbox.not_allowed").color(NamedTextColor.GRAY));
        if (leader) {
            view.lore(Translations.component("clans.camp.upgrade.rank_lockbox.always").color(NamedTextColor.GRAY));
            return new SimpleItem(view.build());
        }
        view.action(ClickActions.ALL, Translations.component("clans.camp.upgrade.rank_lockbox.toggle"));
        return new SimpleItem(view.build(), click -> {
            final Player player = click.getPlayer();
            final String problem = lockbox.toggle(player, key.getOwnerId(), rank);
            if (problem != null) {
                UtilMessage.plain(player, Translations.component(problem).color(NamedTextColor.RED));
                return;
            }
            new RankLockboxMenu(lockbox, player, key, previous).show(player);
        });
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.rank_lockbox.name");
    }
}
