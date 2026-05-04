package me.mykindos.betterpvp.progression.profession.skill.fishing.nomoremobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.entity.EntitySpawnLoot;
import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

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
        if (!"Fishing".equals(event.getContext().getSource())) return;
        if (!(event.getLoot() instanceof EntitySpawnLoot)) return;

        final Audience audience = event.getContext().getSession().getAudience();
        if (!(audience instanceof Player player)) return;
        if (skill.getSkillLevel(player) <= 0) return;

        // The swimmer entity has just been spawned at context.getLocation() and tagged via PDC.
        final Location location = event.getContext().getLocation();
        location.getNearbyEntities(2.0, 2.0, 2.0).stream()
                .filter(NoMoreMobsListener::isFishingSwimmer)
                .forEach(Entity::remove);

        UtilMessage.message(player, "Fishing", "<alt>No More Mobs</alt> removed nearby swimmers!");
    }

    private static boolean isFishingSwimmer(Entity entity) {
        return entity.getPersistentDataContainer().getOrDefault(
                ProgressionNamespacedKeys.FISHING_SWIMMER, PersistentDataType.BOOLEAN, false);
    }
}
