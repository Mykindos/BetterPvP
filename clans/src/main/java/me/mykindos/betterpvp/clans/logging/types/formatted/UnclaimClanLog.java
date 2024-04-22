package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UnclaimClanLog extends FormattedClanLog {
    public UnclaimClanLog(long time, String mainPlayerName, @Nullable UUID mainClan, @Nullable String mainClanName, @Nullable UUID otherClan, @Nullable String otherClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, otherClan, otherClanName, ClanLogType.CLAN_UNCLAIM);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("unclaimed", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("territory", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getClanComponent(otherClan, otherClanName, ClanRelation.NEUTRAL));
    }
}
