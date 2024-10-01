package me.mykindos.betterpvp.core.combat.stats.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ConcurrentHashMultiset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.repository.PlayerData;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    protected @IntRange(from = 0) int kills;
    protected @IntRange(from = 0) int assists;
    protected @IntRange(from = 0) int deaths;
    protected @IntRange(from = 0) int killStreak;
    protected @IntRange(from = 0) int highestKillStreak;

    public final <T> T getAttachment(Class<T> clazz) {
        return (T) attachments.stream().filter(attachment -> attachment.getClass().equals(clazz)).findFirst().orElseThrow();
    }

    public final void attach(ICombatDataAttachment attachment) {
        attachments.add(attachment);
    }

    public final Kill killed(UUID killId, @NotNull CombatData killer, @NotNull Map<CombatData, Contribution> contributions) {
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

    protected Kill generateKill(UUID killId, UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions) {
        return new Kill(killId, killer, victim, ratingDelta, contributions);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {

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
                UtilMessage.deserialize("Rating: <alt>%,d", rating),
                Component.empty(),
                UtilMessage.deserialize("Kills: <alt2>%,d", kills),
                UtilMessage.deserialize("Assists: <alt2>%,d", assists),
                UtilMessage.deserialize("Deaths: <alt2>%,d", deaths),
                UtilMessage.deserialize("KDR: <alt2>%,.2f", getKillDeathRatio()),
                UtilMessage.deserialize("Killstreak: <alt2>%,d", killStreak),
                UtilMessage.deserialize("Highest Killstreak: <alt2>%,d", highestKillStreak),
        };
    }

}
