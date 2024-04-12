package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.logging.FormattedLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNullableByDefault
public class FormattedClanLog extends FormattedLog {
    protected OfflinePlayer offlinePlayer1;
    protected UUID clan1;
    protected String clan1Name;
    protected OfflinePlayer offlinePlayer2;
    protected UUID clan2;
    protected String clan2Name;
    @NotNull
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
    public FormattedClanLog(long time, OfflinePlayer offlinePlayer1, UUID clan1, String clan1Name, OfflinePlayer offlinePlayer2, UUID clan2, String clan2Name, @NotNull ClanLogType type) {
        super(time);
        this.offlinePlayer1 = offlinePlayer1;
        this.clan1 = clan1;
        this.clan1Name = clan1Name;
        this.offlinePlayer2 = offlinePlayer2;
        this.clan2 = clan2;
        this.clan2Name = clan2Name;
        this.type = type;
    }

    protected Component getPlayerClan1(@NotNull ClanRelation clanRelation) {
        return getPlayerClan(offlinePlayer1, clan1, clan1Name, clanRelation);
    }

    protected Component getPlayerClan2(@NotNull ClanRelation clanRelation) {
        return getPlayerClan(offlinePlayer2, clan2, clan2Name, clanRelation);
    }

    protected Component getPlayerClan(OfflinePlayer player, UUID clan, String clanName, @NotNull ClanRelation clanRelation) {
        if (clan == null) {
            return getPlayerComponent(player, ClanRelation.NEUTRAL);
        }
        return getClanComponent(clan, clanName, clanRelation).appendSpace().append(getPlayerComponent(player, clanRelation));
    }

    protected Component getPlayerComponent(OfflinePlayer player, @NotNull ClanRelation clanRelation) {
        if (player == null) {
            return Component.empty();
        }
        return Component.text(Objects.requireNonNull(player.getName()), clanRelation.getPrimary());
    }

    protected Component getClanComponent(UUID clan, String clanName, @NotNull ClanRelation clanRelation) {
        if (clan == null) {
            return Component.empty();
        }
        assert clanName != null;
        return Component.empty().append(Component.text(clanName, clanRelation.getSecondary()).hoverEvent(HoverEvent.showText(Component.text(String.valueOf(clan)))));
    }

    @Override
    public Component getComponent() {
        return super.getComponent().append(getPlayerClan1(ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text(type.name())).appendSpace()
                .append(getPlayerClan2(ClanRelation.NEUTRAL));
    }
}
