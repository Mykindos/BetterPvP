package me.mykindos.betterpvp.core.world.settler.recruit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * How anything outside the settler framework gives a site a settler: a prisoner freed from a dungeon, a captive
 * rescued from a boss, a hermit found on an island. The caller says what kind of settler and where it came from, and
 * the site's own table rolls the rest.
 */
@Singleton
public class SettlerGrants {

    private final SettlerService settlers;
    private final SettlerGenerator generator;

    @Inject
    public SettlerGrants(@NotNull SettlerService settlers, @NotNull SettlerGenerator generator) {
        this.settlers = settlers;
        this.generator = generator;
    }

    /** Rolls a settler from {@code template} and adds it to {@code site}, refused if there is no room. */
    public @NotNull SettlerResult grant(@NotNull SiteKey site, @NotNull SettlerTemplate template) {
        final SettlerTable table = settlers.site(site).flatMap(owner -> owner.table(site)).orElse(null);
        if (table == null) {
            return SettlerResult.refused("core.settler.not_loaded");
        }
        return settlers.grant(site, generator.roll(template, table, ThreadLocalRandom.current()));
    }

    /** Whether {@code site} has room for one more settler right now. */
    public boolean hasRoom(@NotNull SiteKey site) {
        return settlers.roster(site).map(roster -> roster.size() < settlers.populationCap(site)).orElse(false);
    }
}
