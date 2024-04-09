package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SetHomeClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     */
    public SetHomeClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable UUID clan1, String clan1Name) {
        super(time, offlinePlayer1, clan1, clan1Name, null, null, null, ClanLogType.CLAN_SETHOME);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("set", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("the clan home"));
    }
}
