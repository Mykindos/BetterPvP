package me.mykindos.betterpvp.core.combat.stats.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ConcurrentHashMultiset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.stats.repository.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Setter
@Getter
@RequiredArgsConstructor
public abstract class CombatData extends PlayerData {

    /**
     * Gets how much rating a player would lose if they died, which is equal to
     * how much rating the killer would gain.
     *
     * @param killerData The killer's combat data
     * @param victimData The victim's combat data
     * @return The amount of rating the killer would gain and the player would lose
     */
    public static int getRatingDelta(@NotNull CombatData killerData, @NotNull CombatData victimData) {
        return (int) (30 * (1 - (1.0 / (1 + Math.pow(10, (victimData.getRating() - killerData.getRating()) / 1000f)))));
    }

    @Getter(AccessLevel.NONE)
    protected final ConcurrentHashMultiset<Kill> pendingKills = ConcurrentHashMultiset.create();
    @Getter(AccessLevel.NONE)
    protected final ConcurrentHashMultiset<ICombatDataAttachment> attachments = ConcurrentHashMultiset.create();

    protected final UUID holder;
    protected @IntRange(from = 0) int rating = 1_500; // Default is 1500 rating
    protected @IntRange(from = 0) long kills;
    protected @IntRange(from = 0) long assists;
    protected @IntRange(from = 0) long deaths;
    protected @IntRange(from = 0) int killStreak;
    protected @IntRange(from = 0) int highestKillStreak;

    public final <T> T getAttachment(Class<T> clazz) {
        return (T) attachments.stream().filter(attachment -> attachment.getClass().equals(clazz)).findFirst().orElseThrow();
    }

    public final void attach(ICombatDataAttachment attachment) {
        attachments.add(attachment);
    }

    public final Kill killed(long killId, @NotNull CombatData killer, @NotNull Map<CombatData, Contribution> contributions) {
        Preconditions.checkArgument(contributions.containsKey(killer), "Must have at least one contributor");
        final int ratingDelta = getRatingDelta(killer, this);

        // Self stats
        deaths++;
        killStreak = 0;

        // Rating
        rating = Math.max(0, rating - ratingDelta);
        killer.rating += ratingDelta;

        // Killer stats
        killer.kills++;
        killer.killStreak++;
        if (killer.killStreak > killer.highestKillStreak) {
            killer.highestKillStreak = killer.killStreak;
        }

        // Contributor stats
        for (CombatData contributor : contributions.keySet()) {
            if (contributor != killer) {
                contributor.assists++; // Only give assists if they did not get the kill credit
            }
        }

        // Allow modules to modify the kill
        final Kill kill = generateKill(killId,
                killer.getHolder(),
                getHolder(),
                ratingDelta,
                new ArrayList<>(contributions.values()));

        // Save
        pendingKills.add(kill);
        return kill;
    }

    protected Kill generateKill(long killId, UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions) {
        return new Kill(killId, killer, victim, ratingDelta, contributions);
    }

    @Override
    protected CompletableFuture<Void> prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        return CompletableFuture.completedFuture(null);
    }

    public final float getKillDeathRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (float) kills / deaths;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Translations.component("core.combat.stats.rating",
                        Component.text(String.format("%,d", rating), NamedTextColor.GREEN)),
                Component.empty(),
                Translations.component("core.combat.stats.kills",
                        Component.text(String.format("%,d", kills), NamedTextColor.YELLOW)),
                Translations.component("core.combat.stats.assists",
                        Component.text(String.format("%,d", assists), NamedTextColor.YELLOW)),
                Translations.component("core.combat.stats.deaths",
                        Component.text(String.format("%,d", deaths), NamedTextColor.YELLOW)),
                Translations.component("core.combat.stats.kdr",
                        Component.text(String.format("%,.2f", getKillDeathRatio()), NamedTextColor.YELLOW)),
                Translations.component("core.combat.stats.killstreak",
                        Component.text(String.format("%,d", killStreak), NamedTextColor.YELLOW)),
                Translations.component("core.combat.stats.highest_killstreak",
                        Component.text(String.format("%,d", highestKillStreak), NamedTextColor.YELLOW)),
        };
    }

}
