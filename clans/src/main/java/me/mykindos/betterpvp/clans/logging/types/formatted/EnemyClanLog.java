package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EnemyClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param clan2          clan2 of the log
     */
    public EnemyClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, UUID clan1, String clan1Name, UUID clan2, String clan2Name) {
        super(time, offlinePlayer1, clan1, clan1Name, null, clan2, clan2Name, ClanLogType.CLAN_ENEMY);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("enemied", ClanRelation.ENEMY.getPrimary())).appendSpace()
                .append(getClanComponent(clan2, clan2Name, ClanRelation.ENEMY));
    }
}
