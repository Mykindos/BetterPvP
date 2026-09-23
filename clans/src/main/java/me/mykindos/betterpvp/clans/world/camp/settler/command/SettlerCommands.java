package me.mykindos.betterpvp.clans.world.camp.settler.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** What the settler commands share: finding a clan's camp by name, and showing a settler in one line. */
@Singleton
public class SettlerCommands {

    static final String PREFIX = "clans.prefix.settler";

    private final Clans clans;
    private final ClanManager clanManager;
    private final CampStore store;
    private final ProfessionRegistry professions;
    private final TraitRegistry traits;

    @Inject
    public SettlerCommands(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull CampStore store,
                           @NotNull ProfessionRegistry professions, @NotNull TraitRegistry traits) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.store = store;
        this.professions = professions;
        this.traits = traits;
    }

    /** Reads the camp of the clan named {@code name}, then runs {@code action} on the main thread. */
    void withCamp(@NotNull Player player, @NotNull String name, @NotNull Consumer<Clan> action) {
        final Clan clan = clanManager.getClanByName(name).orElse(null);
        if (clan == null) {
            send(player, "clans.command.settler.no_clan", Component.text(name, NamedTextColor.YELLOW));
            return;
        }
        store.load(clan.getId()).thenRun(() -> UtilServer.runTask(clans, () -> action.accept(clan)));
    }

    /** Tells {@code player} the message under translation key {@code key}. */
    static void send(@NotNull Player player, @NotNull String key, @NotNull ComponentLike... args) {
        send(player, Translations.component(key, args).color(NamedTextColor.GRAY));
    }

    static void send(@NotNull Player player, @NotNull Component message) {
        UtilMessage.message(player, Translations.component(PREFIX), message);
    }

    static @NotNull SiteKey key(@NotNull Clan clan) {
        return Camps.keyFor(clan);
    }

    @NotNull List<String> clanNames(@NotNull String typed) {
        return clanManager.getObjects().values().stream()
                .map(Clan::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT)))
                .toList();
    }

    @NotNull List<String> professionIds() {
        return professions.all().stream().map(Profession::getId).toList();
    }

    /** A settler's name in its rarity's colour. */
    static @NotNull Component name(@NotNull Settler settler) {
        return Component.text(settler.getName(), settler.getRarity().getColor());
    }

    /** Name, rarity, profession and specialty, traits, and the start of its id. */
    @NotNull Component line(@NotNull Settler settler) {
        final Profession profession = settler.getProfession() == null ? null
                : professions.find(settler.getProfession()).orElse(null);
        Component work = profession == null
                ? Translations.component("clans.command.settler.no_profession")
                : Translations.component(profession.getKey());
        if (profession != null && settler.getSpecialty() != null) {
            work = work.append(Component.text(" ("))
                    .append(Translations.component(profession.specialtyKey(settler.getSpecialty())))
                    .append(Component.text(")"));
        }

        final List<Component> traitNames = settler.getTraits().stream()
                .map(id -> traits.find(id)
                        .map(trait -> Translations.component(trait.nameKey()))
                        .orElseGet(() -> Component.text(id)))
                .toList();

        return name(settler)
                .append(Component.text(" " + settler.getId().toString().substring(0, 8), NamedTextColor.DARK_GRAY))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(settler.getRarity().displayName())
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(work.color(NamedTextColor.YELLOW))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.join(JoinConfiguration.commas(true), traitNames).color(NamedTextColor.GRAY));
    }
}
