package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class TrustAcceptLog extends FormattedClanLog{
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param clan2          clan2 of the log
     */
    public TrustAcceptLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable IOldClan clan1, @Nullable IOldClan clan2) {
        super(time, offlinePlayer1, clan1, null, clan2, ClanLogType.CLAN_TRUST_ACCEPT);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("accepted", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("trust", ClanRelation.ALLY_TRUST.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(clan2, ClanRelation.ALLY_TRUST));
    }
}
