package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
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
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Someone who could join the camp: who they are, what they would do, what they ask, and how long they will wait.
 * Members whose rank may hire can take them on or send them away.
 */
public class CandidateMenu extends AbstractGui implements Windowed {

    private final SettlerCards cards;
    private final SiteKey site;
    private final SettlerCandidate candidate;
    private final @Nullable Supplier<Windowed> previous;

    CandidateMenu(@NotNull SettlerCards cards, @NotNull Player viewer, @NotNull SiteKey site,
                  @NotNull SettlerCandidate candidate, @Nullable Supplier<Windowed> previous) {
        super(9, 4);
        this.cards = cards;
        this.site = site;
        this.candidate = candidate;
        this.previous = previous;
        final Settler settler = candidate.getSettler();
        final CampRecruitment recruitment = cards.getRecruitment();

        final ItemView.ItemViewBuilder identity = SettlerItems.identity(settler);
        identity.lore(price(recruitment.price(site, candidate)));
        if (candidate.getExpiresAt() > 0) {
            final long left = Math.max(0, candidate.getExpiresAt() - System.currentTimeMillis());
            identity.lore(Translations.component("clans.settler.recruit.waits",
                    Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(left)))).color(NamedTextColor.GRAY));
        }
        setItem(4, new SimpleItem(identity.build()));
        setItem(13, new SimpleItem(SettlerItems.profession(settler, settler.getProfession() == null ? null
                : cards.getProfessions().find(settler.getProfession()).orElse(null)).build()));

        final List<Trait> traits = settler.getTraits().stream()
                .flatMap(id -> cards.getTraits().find(id).stream())
                .toList();
        final int first = 22 - traits.size() / 2;
        for (int i = 0; i < traits.size(); i++) {
            setItem(first + i, new SimpleItem(SettlerItems.trait(traits.get(i))));
        }

        final boolean allowed = cards.allows(viewer, site, SettlerAction.HIRE);
        setItem(29, button(Material.EMERALD, "clans.settler.recruit.hire", allowed, this::hire));
        setItem(33, button(Material.BARRIER, "clans.settler.recruit.turn_away", allowed, this::turnAway));
        setItem(31, new BackButton(previous == null ? null : previous.get()));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull Component price(long price) {
        return price <= 0
                ? Translations.component("clans.settler.recruit.free").color(NamedTextColor.GREEN)
                : Translations.component("clans.settler.recruit.price",
                Component.text(UtilFormat.formatNumber((int) price), NamedTextColor.GOLD)).color(NamedTextColor.GRAY);
    }

    private @NotNull SimpleItem button(@NotNull Material icon, @NotNull String key, boolean allowed,
                                       @NotNull Consumer<Player> action) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(icon)
                .displayName(Translations.component(key).color(allowed ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (allowed) {
            view.action(ClickActions.ALL, Translations.component(key));
        } else {
            view.frameLore(true).lore(Translations.component("clans.settler.card.not_allowed").color(NamedTextColor.RED));
        }
        return new SimpleItem(view.build(), click -> {
            if (allowed) {
                action.accept(click.getPlayer());
            }
        });
    }

    private void hire(@NotNull Player player) {
        final SettlerResult result = cards.getRecruitment().hire(player, site, candidate.getSettler().getId());
        if (!result.isSuccess()) {
            if (result.getReason() != null) {
                cards.tell(player, result.getReason());
            }
            return;
        }
        cards.tell(player, Translations.component("clans.settler.recruit.hired",
                Component.text(candidate.getSettler().getName(), candidate.getSettler().getRarity().getColor()))
                .color(NamedTextColor.GREEN));
        leave(player);
    }

    private void turnAway(@NotNull Player player) {
        final SettlerResult result = cards.getRecruitment().turnAway(player, site, candidate.getSettler().getId());
        if (!result.isSuccess() && result.getReason() != null) {
            cards.tell(player, result.getReason());
            return;
        }
        leave(player);
    }

    private void leave(@NotNull Player player) {
        if (previous == null) {
            player.closeInventory();
        } else {
            previous.get().show(player);
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.settler.recruit.title");
    }
}
