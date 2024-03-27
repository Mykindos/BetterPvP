package me.mykindos.betterpvp.clans.logging.formattedtypes;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.logging.ClanLogType;
import me.mykindos.betterpvp.clans.logging.FormattedClanLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;

public class JoinClanLog extends FormattedClanLog {
    public JoinClanLog(long time, OfflinePlayer offlinePlayer1, Clan clan1) {
        super(time, offlinePlayer1, clan1, null, null, ClanLogType.JOIN);
    }

    @Override
    public Component getComponent() {
        assert offlinePlayer1 != null;
        assert clan1 != null;
        return getTimeComponent().append(UtilMessage.deserialize("<yellow>%s</yellow> <green>joined <aqua>%s</aqua>", offlinePlayer1.getName(), clan1.getName()));
    }
}
