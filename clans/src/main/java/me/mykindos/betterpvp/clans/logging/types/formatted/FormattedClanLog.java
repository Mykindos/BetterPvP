package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.logging.FormattedLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNullableByDefault
public class FormattedClanLog extends FormattedLog {
    protected String mainPlayerName;
    protected UUID mainClan;
    protected String mainClanName;
    protected String otherPlayerName;
    protected UUID otherClan;
    protected String otherClanName;
    @NotNull
    protected ClanLogType type;

    /**
     *
     * @param time the time this log was generated
     * @param mainPlayerName player1 of the log
     * @param mainClan clan1 of the log
     * @param otherPlayerName player2 of the log
     * @param clan2 clan2 of the log
     * @param type the type of log
     */
    public FormattedClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName, String otherPlayerName, UUID clan2, String clan2Name, @NotNull ClanLogType type) {
        super(time);
        this.mainPlayerName = mainPlayerName;
        this.mainClan = mainClan;
        this.mainClanName = mainClanName;
        this.otherPlayerName = otherPlayerName;
        this.otherClan = clan2;
        this.otherClanName = clan2Name;
        this.type = type;
    }

    protected Component getPlayerClan1(@NotNull ClanRelation clanRelation) {
        return getPlayerClan(mainPlayerName, mainClan, mainClanName, clanRelation);
    }

    protected Component getPlayerClan2(@NotNull ClanRelation clanRelation) {
        return getPlayerClan(mainPlayerName, otherClan, otherClanName, clanRelation);
    }

    protected Component getPlayerClan(String player, UUID clan, String clanName, @NotNull ClanRelation clanRelation) {
        if (clan == null) {
            return getPlayerComponent(player, ClanRelation.NEUTRAL);
        }
        return getClanComponent(clan, clanName, clanRelation).appendSpace().append(getPlayerComponent(player, clanRelation));
    }

    protected Component getPlayerComponent(String player, @NotNull ClanRelation clanRelation) {
        if (player == null) {
            return Component.empty();
        }
        return Component.text(Objects.requireNonNull(player), clanRelation.getPrimary());
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
