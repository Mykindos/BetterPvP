package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerItems;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.clans.world.camp.upgrade.GuestQuarters;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * The Steward's hiring board: settlers looking for work, each for a price. A new set turns up on its own after a
 * while, or at once for a fee. With {@link GuestQuarters} a right click keeps one candidate through new sets.
 */
public class HiringBoardMenu extends AbstractGui implements Windowed {

    private final HallMenus menus;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    HiringBoardMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key,
                    @Nullable Windowed previous) {
        super(9, 3);
        this.menus = menus;
        this.key = key;
        this.previous = previous;
        final CampRecruitment recruitment = menus.getRecruitment();

        final List<SettlerCandidate> board = recruitment.board(key);
        final long refresh = Math.max(0, recruitment.boardRefreshAt(key) - System.currentTimeMillis());
        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.OAK_SIGN)
                .displayName(Translations.component("clans.camp.hall.hiring.name").color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.hall.hiring.refresh",
                        Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(refresh)))).color(NamedTextColor.GRAY))
                .build()));

        final GuestQuarters quarters = menus.getGuestQuarters();
        final boolean reserving = quarters.isActive(key);
        final int first = 13 - Math.min(board.size(), 7) / 2;
        for (int i = 0; i < board.size() && i < 7; i++) {
            final SettlerCandidate candidate = board.get(i);
            final boolean reserved = reserving && quarters.isReserved(key, candidate);
            final ItemView.ItemViewBuilder view = SettlerItems.identity(candidate.getSettler())
                    .material(Material.PLAYER_HEAD)
                    .lore(Translations.component("clans.settler.recruit.price", Component.text(
                            UtilFormat.formatNumber((int) recruitment.price(key, candidate)), NamedTextColor.GOLD))
                            .color(NamedTextColor.GRAY));
            if (reserved) {
                view.lore(Translations.component("clans.camp.upgrade.guest_quarters.reserved").color(NamedTextColor.AQUA))
                        .glow(true);
            }
            if (reserving) {
                view.action(ClickActions.LEFT, Translations.component("clans.camp.hall.settlers.open_card"))
                        .action(ClickActions.RIGHT, Translations.component(reserved
                                ? "clans.camp.upgrade.guest_quarters.unreserve"
                                : "clans.camp.upgrade.guest_quarters.reserve"));
            } else {
                view.action(ClickActions.ALL, Translations.component("clans.camp.hall.settlers.open_card"));
            }
            setItem(first + i, new SimpleItem(view.build(), click -> {
                final Player player = click.getPlayer();
                if (reserving && ClickActions.RIGHT.accepts(click.getClickType())) {
                    final String problem = quarters.toggle(player, key, candidate.getSettler().getId());
                    if (problem != null) {
                        menus.tell(player, problem);
                    }
                    new HiringBoardMenu(menus, player, key, previous).show(player);
                    return;
                }
                menus.getCards().openCandidate(player, key, candidate.getSettler().getId(),
                        () -> new HiringBoardMenu(menus, player, key, previous));
            }));
        }

        final Component reroll = Component.text(UtilFormat.formatNumber((int) menus.getRecruitConfig().getReroll()),
                NamedTextColor.GOLD);
        setItem(26, new SimpleItem(ItemView.builder()
                .material(Material.CLOCK)
                .displayName(Translations.component("clans.camp.hall.hiring.reroll", reroll).color(NamedTextColor.YELLOW))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.hiring.reroll", reroll))
                .build(), click -> {
                    final String problem = recruitment.reroll(click.getPlayer(), key);
                    if (problem != null) {
                        menus.tell(click.getPlayer(), problem, reroll);
                    }
                    new HiringBoardMenu(menus, click.getPlayer(), key, previous).show(click.getPlayer());
                }));
        if (menus.getHarbourmaster().has(key)) {
            setItem(18, HarbourmasterMenu.button(menus, key,
                    player -> new HarbourmasterMenu(menus, key, new HiringBoardMenu(menus, player, key, previous))
                            .show(player)));
        }
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.hall.hiring.name");
    }
}
