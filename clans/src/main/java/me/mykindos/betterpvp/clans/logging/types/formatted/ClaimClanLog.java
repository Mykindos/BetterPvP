package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class ClaimClanLog extends FormattedClanLog {
    public ClaimClanLog(long time, OfflinePlayer offlinePlayer1, UUID clan1, String clan1Name) {
        super(time, offlinePlayer1, clan1, clan1Name, null, null, null, ClanLogType.CLAN_CLAIM);
    }

    @Override
    public Component getComponent() {
        assert offlinePlayer1 != null;
        assert clan1 != null;
        return getTimeComponent().append(getPlayerComponent(offlinePlayer1, ClanRelation.SELF)).appendSpace()
                .append(Component.text("claimed territory", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("for")).appendSpace()
                .append(getClanComponent(clan1, clan1Name, ClanRelation.SELF));
    }
}