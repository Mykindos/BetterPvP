package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.logging.FormattedLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FormattedClanLog extends FormattedLog {
    @Nullable
    protected OfflinePlayer offlinePlayer1;
    @Nullable
    protected IOldClan clan1;
    @Nullable
    protected OfflinePlayer offlinePlayer2;
    @Nullable
    protected IOldClan clan2;
    protected ClanLogType type;

    /**
     *
     * @param time the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1 clan1 of the log
     * @param offlinePlayer2 player2 of the log
     * @param clan2 clan2 of the log
     * @param type the type of log
     */
    public FormattedClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable IOldClan clan1, @Nullable OfflinePlayer offlinePlayer2, @Nullable IOldClan clan2, ClanLogType type) {
        super(time);
        this.offlinePlayer1 = offlinePlayer1;
        this.clan1 = clan1;
        this.offlinePlayer2 = offlinePlayer2;
        this.clan2 = clan2;
        this.type = type;
    }

    protected Component getPlayerClan1(ClanRelation clanRelation) {
        return getPlayerClan(offlinePlayer1, clan1, clanRelation);
    }

    protected Component getPlayerClan2(ClanRelation clanRelation) {
        return getPlayerClan(offlinePlayer1, clan1, clanRelation);
    }

    protected Component getPlayerClan(OfflinePlayer player, IOldClan clan, ClanRelation clanRelation) {
        if (clan == null) {
            return getPlayerComponent(player, ClanRelation.NEUTRAL);
        }
        return getClanComponent(clan, clanRelation).appendSpace().append(getPlayerComponent(player, clanRelation));
    }

    protected Component getPlayerComponent(OfflinePlayer player, ClanRelation clanRelation) {
        if (player == null) {
            return Component.empty();
        }
        return Component.text(Objects.requireNonNull(player.getName()), clanRelation.getPrimary());
    }

    protected Component getClanComponent(IOldClan clan, ClanRelation clanRelation) {
        if (clan == null) {
            return Component.empty();
        }
        return Component.text(clan.getName(), clanRelation.getSecondary());
    }

    @Override
    public Component getComponent() {
        return super.getComponent().append(UtilMessage.deserialize("<yellow>%s</yellow> <aqua>%s</aqua> <white>%s</white> <yellow>%s</yellow> <aqua>%s</aqua>",
                offlinePlayer1 == null ? null : offlinePlayer1.getName(),
                clan1 == null ? null : clan1.getName(),
                type.name(),
                offlinePlayer2 == null ? null : offlinePlayer2.getName(),
                clan2 == null ? null : clan2.getName()
                ));
    }
}
