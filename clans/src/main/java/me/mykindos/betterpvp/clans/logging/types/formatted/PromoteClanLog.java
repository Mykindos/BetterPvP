package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class PromoteClanLog extends FormattedClanLog{
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param offlinePlayer2 player2 of the log
     * @param clan2          clan2 of the log
     */
    public PromoteClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable IOldClan clan1, @Nullable OfflinePlayer offlinePlayer2, @Nullable IOldClan clan2) {
        super(time, offlinePlayer1, clan1, offlinePlayer2, clan2, ClanLogType.CLAN_PROMOTE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan2(ClanRelation.SELF)).appendSpace()
                .append(Component.text("promoted", NamedTextColor.GREEN)).appendSpace()
                .append(getPlayerClan1(ClanRelation.SELF));
    }
}
