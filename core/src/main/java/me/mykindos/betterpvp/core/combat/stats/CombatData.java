package me.mykindos.betterpvp.core.combat.stats;

import com.google.common.collect.ConcurrentHashMultiset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.events.KillDataSaveEvent;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.stats.repository.PlayerData;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@RequiredArgsConstructor
public class CombatData extends PlayerData {

    /**
     * Gets how much rating a player would lose if they died, which is equal to
     * how much rating the killer would gain.
     *
     * @param killerData The killer's combat data
     * @param victimData The victim's combat data
     * @return The amount of rating the killer would gain and the player would lose
     */
    private static int getRatingDelta(@NotNull CombatData killerData, @NotNull CombatData victimData) {
        return (int) Math.ceil(1.0f / (1 + (Math.pow(10, (killerData.getRating() - victimData.getRating()) / 400f))));
    }

    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMap<Kill, List<Statement>> pending = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMultiset<ICombatDataAttachment> attachments = ConcurrentHashMultiset.create();

    private final UUID holder;
    private @IntRange(from = 0) int rating = 1_000; // Default is 1000 rating
    private @IntRange(from = 0) int kills;
    private @IntRange(from = 0) int assists;
    private @IntRange(from = 0) int deaths;
    private @IntRange(from = 0) int killStreak;
    private @IntRange(from = 0) int highestKillStreak;

    public <T> T getAttachment(Class<T> clazz) {
        return (T) attachments.stream().filter(attachment -> attachment.getClass().equals(clazz)).findFirst().orElseThrow();
    }

    void attach(ICombatDataAttachment attachment) {
        attachments.add(attachment);
    }

    private void died(int ratingLoss) {
        deaths++;
        killStreak = 0;
        rating = Math.max(0, rating - ratingLoss);
    }

    private void assisted() {
        assists++;
    }

    public void killed(CombatData victim, List<CombatData> assists) {
        final int ratingDelta = getRatingDelta(this, victim);
        rating += ratingDelta;
        victim.died(ratingDelta);

        kills++;
        killStreak++;
        if (killStreak > highestKillStreak) {
            highestKillStreak = killStreak;
        }

        assists.forEach(CombatData::assisted);
        final Kill kill = new Kill(getHolder(),
                victim.getHolder(),
                assists.stream().map(data -> new Assist(data.getHolder())).toArray(Assist[]::new));
        final KillDataSaveEvent killDataSaveEvent = new KillDataSaveEvent(kill);
        UtilServer.callEvent(killDataSaveEvent);
        pending.put(kill, killDataSaveEvent.getStatements());
    }

    public float getKillDeathRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (float) kills / deaths;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                UtilMessage.deserialize("Rating: <alt2>%,d", rating),
                Component.empty(),
                UtilMessage.deserialize("Kills: <alt>%,d", kills),
                UtilMessage.deserialize("Assists: <alt>%,d", assists),
                UtilMessage.deserialize("Deaths: <alt>%,d", deaths),
                UtilMessage.deserialize("KDR: <alt>%,.2f", getKillDeathRatio()),
                UtilMessage.deserialize("Killstreak: <alt>%,d", killStreak),
                UtilMessage.deserialize("Highest Killstreak: <alt>%,d", highestKillStreak),
        };
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        List<Statement> statements = new ArrayList<>();
        final String killStmt = "INSERT INTO " + databasePrefix + "kills (Id, Killer, Victim) VALUES (?, ?, ?);";
        final String assistStmt = "INSERT INTO " + databasePrefix + "assists (AssistId, KillId, Assister) VALUES (?, ?);";

        for (Map.Entry<Kill, List<Statement>> killEntry: pending.entrySet()) {
            final Kill kill = killEntry.getKey();

            final UUID killId = kill.getId();
            Statement killStatement = new Statement(killStmt,
                    new UuidStatementValue(killId),
                    new UuidStatementValue(kill.getKiller()),
                    new UuidStatementValue(kill.getVictim()));
            statements.add(killStatement);

            for (int i = 0; i < kill.getAssists().length; i++) {
                final Assist assist = kill.getAssists()[i];
                Statement assistStatement = new Statement(assistStmt,
                        new UuidStatementValue(assist.getId()),
                        new UuidStatementValue(killId),
                        new UuidStatementValue(assist.getPlayer()));
                statements.add(assistStatement);
            }

            statements.addAll(killEntry.getValue());
        }

        database.executeBatch(statements, false);
    }
}
