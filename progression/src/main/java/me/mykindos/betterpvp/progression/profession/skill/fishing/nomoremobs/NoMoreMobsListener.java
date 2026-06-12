package me.mykindos.betterpvp.progression.profession.skill.fishing.nomoremobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.entity.EntitySpawnLoot;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class NoMoreMobsListener implements Listener {

    private final NoMoreMobs skill;

    @Inject
    public NoMoreMobsListener(NoMoreMobs skill) {
        this.skill = skill;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwimmerAwarded(LootAwardedEvent event) {
        if (!event.getContext().getSource().getId().startsWith("fishing:")) return;
        if (!(event.getLoot() instanceof EntitySpawnLoot)) return;

        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        if (skill.getSkillLevel(player) <= 0) return;

        final Entity swimmer = event.getResult();
        if (swimmer == null) return;

        UtilServer.runTaskLater(skill.getProgression(), () -> {
            if (!swimmer.isValid()) return;
            final Location at = swimmer.getLocation();
            swimmer.remove();
            Particle.CLOUD.builder()
                    .location(at)
                    .count(15)
                    .offset(0.3, 0.3, 0.3)
                    .extra(0.02)
                    .receivers(48)
                    .spawn();
        }, 1L);

        UtilMessage.message(player, "core.prefix.fishing", "progression.fishing.no-more-mobs.removed-swimmers",
                Component.text("No More Mobs", NamedTextColor.GREEN));
    }
}
