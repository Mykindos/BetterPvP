package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.wage.Payroll;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/** Opens settler cards and carries out what their buttons ask for. */
@Singleton
public class SettlerCards {

    @Getter(AccessLevel.PACKAGE)
    private final SettlerService service;
    @Getter(AccessLevel.PACKAGE)
    private final ProfessionRegistry professions;
    @Getter(AccessLevel.PACKAGE)
    private final TraitRegistry traits;
    @Getter(AccessLevel.PACKAGE)
    private final CrewMenus crews;
    @Getter(AccessLevel.PACKAGE)
    private final Payroll payroll;
    @Getter(AccessLevel.PACKAGE)
    private final CampRecruitment recruitment;

    @Inject
    public SettlerCards(@NotNull SettlerService service, @NotNull ProfessionRegistry professions,
                        @NotNull TraitRegistry traits, @NotNull CrewMenus crews, @NotNull Payroll payroll,
                        @NotNull CampRecruitment recruitment) {
        this.service = service;
        this.professions = professions;
        this.traits = traits;
        this.crews = crews;
        this.payroll = payroll;
        this.recruitment = recruitment;
    }

    /**
     * Shows {@code player} candidate {@code candidateId}, if they are still waiting. Back leads to whatever
     * {@code previous} makes, made again so it shows who is still waiting.
     */
    public void openCandidate(@NotNull Player player, @NotNull SiteKey site, @NotNull UUID candidateId,
                              @Nullable Supplier<Windowed> previous) {
        recruitment.find(site, candidateId).ifPresentOrElse(
                candidate -> new CandidateMenu(this, player, site, candidate, previous).show(player),
                () -> tell(player, Translations.component("clans.settler.recruit.gone").color(NamedTextColor.GRAY)));
    }

    /** Shows {@code player} the card of settler {@code settlerId}, if it still lives at {@code site}. */
    public void open(@NotNull Player player, @NotNull SiteKey site, @NotNull UUID settlerId) {
        open(player, site, settlerId, null);
    }

    /** As {@link #open(Player, SiteKey, UUID)}, with Back leading to {@code previous}. */
    public void open(@NotNull Player player, @NotNull SiteKey site, @NotNull UUID settlerId,
                     @Nullable Windowed previous) {
        service.roster(site).flatMap(roster -> roster.find(settlerId))
                .ifPresent(settler -> new SettlerCardMenu(this, player, site, settler, previous).show(player));
    }

    boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull SettlerAction action) {
        return service.site(site).map(found -> found.allows(player, site, action)).orElse(false);
    }

    /** Tells the player why an action was refused, or reopens the card to show what it changed. */
    void after(@NotNull Player player, @NotNull SiteKey site, @NotNull UUID settlerId, @NotNull SettlerResult result,
               @Nullable Windowed previous) {
        if (!result.isSuccess() && result.getReason() != null) {
            tell(player, result.getReason());
        }
        open(player, site, settlerId, previous);
    }

    void tell(@NotNull Player player, @NotNull Component message) {
        UtilMessage.message(player, Translations.component("clans.prefix.settler"), message);
    }
}
