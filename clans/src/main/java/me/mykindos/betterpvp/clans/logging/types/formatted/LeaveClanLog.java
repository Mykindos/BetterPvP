package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

public class LeaveClanLog extends FormattedClanLog {
    public LeaveClanLog(long time, OfflinePlayer offlinePlayer1, IOldClan clan1) {
        super(time, offlinePlayer1, clan1, null, null, ClanLogType.CLAN_LEAVE);
    }

    @Override
    public Component getComponent() {
        assert offlinePlayer1 != null;
        assert clan1 != null;
        return getTimeComponent()
                .append(getPlayerComponent(offlinePlayer1, ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("left", NamedTextColor.RED)).appendSpace()
                .append(getClanComponent(clan1, ClanRelation.SELF));
    }
}
