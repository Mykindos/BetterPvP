package me.mykindos.betterpvp.clans.clans.fatigue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanCoreTeleportEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Lengthens the {@code /c home} (clan core teleport) cast for fatigued players.
 * <p>
 * It listens to {@link ClanCoreTeleportEvent} — a {@code PlayerDelayedActionEvent}
 * with a mutable {@code delayInSeconds}.
 * <p>
 * <b>Priority is deliberately {@link EventPriority#HIGHEST}.</b> Other clan
 * listeners <i>assign</i> an absolute base delay at {@code NORMAL}
 * ({@code ClansMovementListener.onClanHomeTeleport}) or cancel the event
 * ({@code PillageListener.onTeleportCore}). We must run <i>after</i> every such
 * absolute setter so our {@code +=} stacks on top instead of being overwritten,
 * yet still <i>before</i> the core {@code DelayedActionListener} which consumes
 * the value at {@code MONITOR}. {@code HIGHEST} is the last mutation stage that
 * satisfies both. {@code ignoreCancelled = true} cleanly excludes us when an
 * upstream listener has already denied the teleport.
 * <p>
 * Zero coupling: {@code CoreTransportButton}/{@code TransportListener}/
 * {@code ClansMovementListener} are never touched — the systems coordinate
 * purely through this Bukkit event.
 */
@Singleton
@BPvPListener
public class FatigueHomeCastListener implements Listener {

    private final BattleFatigueManager fatigueManager;

    @Inject
    @Config(path = "clans.fatigue.homeCast.extraSecondsPerTier", defaultValue = "8.0")
    private double extraSecondsPerTier;

    @Inject
    public FatigueHomeCastListener(BattleFatigueManager fatigueManager) {
        this.fatigueManager = fatigueManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCoreTeleport(ClanCoreTeleportEvent event) {
        final Player player = event.getPlayer();
        final FatigueTier tier = fatigueManager.getTier(player.getUniqueId());
        if (!tier.requiresHold()) {
            return; // Rested/low fatigue — leave the normal cast untouched.
        }

        final double extra = extraSecondsPerTier * tier.ordinal();
        event.setDelayInSeconds(event.getDelayInSeconds() + extra);

        UtilMessage.message(player, "clans.fatigue.prefix", Translations.component("clans.fatigue.home-cast.slow").color(NamedTextColor.RED));
    }
}
