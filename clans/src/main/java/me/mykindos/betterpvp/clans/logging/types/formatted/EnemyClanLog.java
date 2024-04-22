package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EnemyClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param player1Name player1 of the log
     * @param clan1          clan1 of the log
     * @param clan2          clan2 of the log
     */
    public EnemyClanLog(long time, String mainPlayerName, @Nullable UUID mainClan, @Nullable String mainClanName, @Nullable UUID otherClan, @Nullable String otherClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, otherClan, otherClanName, ClanLogType.CLAN_ENEMY);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("enemied", ClanRelation.ENEMY.getPrimary())).appendSpace()
                .append(getClanComponent(otherClan, otherClanName, ClanRelation.ENEMY));
    }
}
