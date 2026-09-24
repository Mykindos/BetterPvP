package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.upgrade.WagePolicy;
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
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * The camp's wage fund: what it holds, what the camp's settlers cost an hour, how long that lasts, and whether anyone
 * is on strike. Members whose rank may pay put coins in here. With {@link WagePolicy} each member can also choose to
 * cover what the fund cannot.
 */
public class WageFundMenu extends AbstractGui implements Windowed {

    private final HallMenus menus;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    WageFundMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key, @Nullable Windowed previous) {
        super(9, 3);
        this.menus = menus;
        this.key = key;
        this.previous = previous;
        menus.getPayroll().settle(key);

        setItem(4, summary());
        final boolean allowed = menus.mayPay(viewer, key);
        setItem(11, payButton(1_000, allowed));
        setItem(13, payButton(10_000, allowed));
        setItem(15, payButton(100_000, allowed));
        if (menus.getWagePolicy().isActive(key)) {
            setItem(26, chipInButton(viewer));
        }
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem summary() {
        final long fund = menus.getWageFund().balance(key);
        final double hourly = menus.getPayroll().hourly(key);
        final int striking = menus.getSettlers().roster(key).map(roster -> roster.inState(SettlerState.STRIKING).size())
                .orElse(0);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.GOLD_BLOCK)
                .displayName(Translations.component("clans.camp.hall.wages.fund",
                        Component.text(UtilFormat.formatNumber((int) fund), NamedTextColor.GOLD))
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.hall.wages.hourly",
                        Component.text(UtilFormat.formatNumber((int) Math.ceil(hourly)), NamedTextColor.GOLD))
                        .color(NamedTextColor.GRAY));
        if (hourly > 0) {
            final Duration covers = Duration.ofMillis((long) (fund / hourly * 3_600_000));
            view.lore(Translations.component("clans.camp.hall.wages.covers",
                    Component.text(UtilTime.humanReadableFormat(covers), NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
        }
        if (striking > 0) {
            view.lore(Translations.component("clans.camp.hall.wages.striking", Component.text(striking))
                    .color(NamedTextColor.RED));
        }
        return new SimpleItem(view.build());
    }

    private @NotNull SimpleItem payButton(long amount, boolean allowed) {
        final Component coins = Component.text(UtilFormat.formatNumber((int) amount), NamedTextColor.GOLD);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.GOLD_NUGGET)
                .displayName(Translations.component("clans.camp.hall.wages.pay", coins)
                        .color(allowed ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (allowed) {
            view.action(ClickActions.ALL, Translations.component("clans.camp.hall.wages.pay", coins));
        } else {
            view.frameLore(true).lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
        }
        return new SimpleItem(view.build(), click -> {
            if (allowed) {
                menus.pay(click.getPlayer(), key, amount);
                new WageFundMenu(menus, click.getPlayer(), key, previous).show(click.getPlayer());
            }
        });
    }

    private @NotNull SimpleItem chipInButton(@NotNull Player viewer) {
        final WagePolicy policy = menus.getWagePolicy();
        final boolean on = policy.contributes(key, viewer.getUniqueId());
        final Component state = Translations.component(on ? "clans.camp.upgrade.wage_policy.on"
                : "clans.camp.upgrade.wage_policy.off").color(on ? NamedTextColor.GREEN : NamedTextColor.RED);
        final ItemView view = ItemView.builder()
                .material(on ? Material.LIME_DYE : Material.GRAY_DYE)
                .displayName(Translations.component("clans.camp.upgrade.wage_policy.toggle", state)
                        .color(NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.wage_policy.explain",
                        Component.text(UtilFormat.formatNumber((int) policy.dailyCap()), NamedTextColor.GOLD))
                        .color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.upgrade.wage_policy.today",
                        Component.text(UtilFormat.formatNumber((int) policy.paidToday(key, viewer.getUniqueId())),
                                NamedTextColor.GOLD)).color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.wage_policy.toggle", state))
                .build();
        return new SimpleItem(view, click -> {
            policy.toggle(click.getPlayer(), key);
            new WageFundMenu(menus, click.getPlayer(), key, previous).show(click.getPlayer());
        });
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.hall.wages.name");
    }
}
