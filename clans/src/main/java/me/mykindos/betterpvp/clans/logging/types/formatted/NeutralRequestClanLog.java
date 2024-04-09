package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NeutralRequestClanLog extends FormattedClanLog{
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param clan2          clan2 of the log
     */
    public NeutralRequestClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable UUID clan1, String clan1Name, @Nullable UUID clan2, String clan2Name) {
        super(time, offlinePlayer1, clan1, clan2Name, null, clan2, clan2Name, ClanLogType.CLAN_NEUTRAL_REQUEST);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("requested", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("neutral", ClanRelation.NEUTRAL.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(clan2, clan2Name, ClanRelation.ENEMY));
    }
}
