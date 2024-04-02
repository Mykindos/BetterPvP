package me.mykindos.betterpvp.clans.logging.types.formatted;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
@Slf4j
public class KillClanLog {
    @NotNull
    private final Clan callerClan;
    private final long time;
    @NotNull
    private final OfflinePlayer killer;
    @Nullable
    private final IOldClan killerClan;
    @NotNull
    private final OfflinePlayer victim;
    @Nullable
    private final IOldClan victimClan;
    private final double dominance;

    public KillClanLog(@NotNull Clan callerClan, long time, @NotNull OfflinePlayer killer, @Nullable IOldClan killerClan, @NotNull OfflinePlayer victim, @Nullable IOldClan victimClan, double dominance) {
        this.callerClan = callerClan;
        this.time = time;
        this.killer = killer;
        this.killerClan = killerClan;
        this.victim = victim;
        this.victimClan = victimClan;
        this.dominance = dominance;
    }

    public Component getTimeComponent() {
        log.info("Current time " + System.currentTimeMillis());
        log.info("Time         " + this.time);
        return UtilMessage.deserialize("<green>" + UtilTime.getTime((System.currentTimeMillis() - this.time), 2) + " ago</green> ");
    }

    private Component getKillerPlayerClanFormat() {
        return getPlayerClanFormatting(killer, killerClan);
    }

    private Component getVictimPlayerClanFormat() {
        return getPlayerClanFormatting(victim, victimClan);
    }

    private Component getPlayerClanFormatting(OfflinePlayer player, IOldClan clan) {
        if (clan != null && callerClan.getId() == clan.getId()) {
            return Component.text(clan.getName(), ClanRelation.SELF.getSecondary()).appendSpace().append(Component.text(Objects.requireNonNull(player.getName()), ClanRelation.SELF.getPrimary()));
        }

        if (dominance > 0 && clan != null) {
            return Component.text(clan.getName(), ClanRelation.ENEMY.getSecondary()).appendSpace().append(Component.text(Objects.requireNonNull(player.getName()), ClanRelation.ENEMY.getPrimary()));
        }

        if (clan != null) {
            return Component.text(clan.getName(), ClanRelation.NEUTRAL.getSecondary()).appendSpace().append(Component.text(Objects.requireNonNull(player.getName()), ClanRelation.NEUTRAL.getPrimary()));
        }

        return Component.text(Objects.requireNonNull(player.getName()), ClanRelation.NEUTRAL.getPrimary());
    }

    public Component getComponent() {
        return getTimeComponent().append(getKillerPlayerClanFormat()).appendSpace()
                .append(Component.text("killed", NamedTextColor.GRAY)).appendSpace()
                .append(getVictimPlayerClanFormat()).appendSpace()
                .append(UtilMessage.deserialize("(<yellow>%s</yellow>)", dominance));
    }
}
