package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class KillClanLog {
    @NotNull
    private final Clan callerClan;
    private final long time;
    @NotNull
    private final String killer;
    @Nullable
    private final UUID killerClan;
    @Nullable
    private final String killerClanName;
    @NotNull
    private final String victim;
    @Nullable
    private final UUID victimClan;
    @Nullable
    private final String victimClanName;
    private final double dominance;

    public KillClanLog(@NotNull Clan callerClan, long time, @NotNull String killer, @Nullable UUID killerClan, @Nullable String killerClanName, @NotNull String victim, @Nullable UUID victimClan, @Nullable String victimClanName, double dominance) {
        this.callerClan = callerClan;
        this.time = time;
        this.killer = killer;
        this.killerClan = killerClan;
        this.killerClanName = killerClanName;
        this.victim = victim;
        this.victimClan = victimClan;
        this.victimClanName = victimClanName;
        this.dominance = dominance;
    }

    public Component getTimeComponent() {
        return UtilMessage.deserialize("<green>" + UtilTime.getTime((System.currentTimeMillis() - this.time), 2) + " ago</green> ");
    }

    private Component getKillerPlayerClanFormat() {
        return getPlayerClanFormatting(killer, killerClan, killerClanName);
    }

    private Component getVictimPlayerClanFormat() {
        return getPlayerClanFormatting(victim, victimClan, victimClanName);
    }

    private Component getPlayerClanFormatting(String player, UUID clanID, String clanName) {
        if (clanID != null && callerClan.getId() == clanID) {
            return Component.empty().append(Component.text(clanName, ClanRelation.SELF.getSecondary()).hoverEvent(HoverEvent.showText(Component.text(String.valueOf(clanID)))))
                    .appendSpace().append(Component.text(Objects.requireNonNull(player), ClanRelation.SELF.getPrimary()));
        }

        if (dominance > 0 && clanID != null) {
            return Component.empty().append(Component.text(clanName, ClanRelation.ENEMY.getSecondary()).hoverEvent(HoverEvent.showText(Component.text(String.valueOf(clanID)))))
                    .appendSpace().append(Component.text(Objects.requireNonNull(player), ClanRelation.ENEMY.getPrimary()));
        }

        if (clanID != null) {
            return Component.empty().append(Component.text(clanName, ClanRelation.NEUTRAL.getSecondary()).hoverEvent(HoverEvent.showText(Component.text(String.valueOf(clanID)))))
                    .appendSpace().append(Component.text(Objects.requireNonNull(player), ClanRelation.NEUTRAL.getPrimary()));
        }

        return Component.text(Objects.requireNonNull(player), ClanRelation.NEUTRAL.getPrimary());
    }

    public Component getComponent() {
        return getTimeComponent().append(getKillerPlayerClanFormat()).appendSpace()
                .append(Component.text("killed", NamedTextColor.GRAY)).appendSpace()
                .append(getVictimPlayerClanFormat()).appendSpace()
                .append(UtilMessage.deserialize("(<yellow>%s</yellow>)", dominance));
    }
}
