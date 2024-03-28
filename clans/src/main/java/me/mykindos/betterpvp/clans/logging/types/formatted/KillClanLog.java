package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class KillClanLog extends FormattedClanLog{

    /**
     * @param time
     * @param offlinePlayer1
     * @param clan1
     * @param offlinePlayer2
     * @param clan2
    */
    public KillClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable Clan clan1, @Nullable OfflinePlayer offlinePlayer2, @Nullable Clan clan2) {
        super(time, offlinePlayer1, clan1, offlinePlayer2, clan2, ClanLogType.CLAN_KILL);
    }

    //todo show clan relations
    public Component getComponent() {
        assert offlinePlayer1 != null;
        assert offlinePlayer2 != null;
        return getTimeComponent().append(UtilMessage.deserialize(
                "<yellow>%s</yellow> <aqua>%s</aqua> <red>killed</red> <yellow>%s</yellow> <aqua>%s</aqua>",
                offlinePlayer1.getName(),
                clan1 == null ? "UNKOWN CLAN" : clan1.getName(),
                offlinePlayer2.getName(),
                clan2 == null ? "UNKOWN CLAN" : clan2.getName()));
    }
}
