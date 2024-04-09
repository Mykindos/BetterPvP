package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
public class InviteClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param offlinePlayer2 player2 of the log
     */
    public InviteClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, UUID clan1, String clan1Name, OfflinePlayer offlinePlayer2) {
        super(time, offlinePlayer1, clan1, clan1Name, offlinePlayer2, null, null, ClanLogType.CLAN_INVITE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("invited", NamedTextColor.GREEN)).appendSpace()
                .append(getPlayerComponent(offlinePlayer2, ClanRelation.NEUTRAL));
    }
}
