package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

public class KickClanLog extends FormattedClanLog {
    public KickClanLog(long time, OfflinePlayer offlinePlayer1, IOldClan clan1, OfflinePlayer offlinePlayer2) {
        super(time, offlinePlayer1, clan1, offlinePlayer2, null, ClanLogType.CLAN_KICK);
    }

    @Override
    public Component getComponent() {
        assert offlinePlayer1 != null;
        assert clan1 != null;
        assert offlinePlayer2 != null;
        return getTimeComponent()
                .append(getPlayerComponent(offlinePlayer1, ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("was")).appendSpace()
                .append(Component.text("kicked", NamedTextColor.RED)).appendSpace()
                .append(Component.text("by")).appendSpace()
                .append(getPlayerComponent(offlinePlayer2, ClanRelation.SELF)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getClanComponent(clan1, ClanRelation.SELF));
    }
}
