package me.mykindos.betterpvp.core.loot.chest;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.loot.AwardStrategy;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

// Only usable with MythicMobs on
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LootChest implements AwardStrategy {

    @EqualsAndHashCode.Include
    private final UUID uuid = UUID.randomUUID();
    private final String mythicMobName;
    private final SoundEffect dropSound;
    private final long dropDelay;
    private final long dropInterval;

    @Getter
    private ActiveMob activeMob;
    @Getter
    private LootBundle bundle;

    public LootChest(String mythicMobName, SoundEffect dropSound, long dropDelay, long dropInterval) {
        this.mythicMobName = mythicMobName;
        this.dropSound = dropSound;
        this.dropDelay = dropDelay;
        this.dropInterval = dropInterval;
    }

    // Spawn the chest entity
    @Override
    public void award(LootBundle bundle) {
        this.bundle = bundle;

        final Location location = bundle.getContext().getLocation().add(0, 1, 0);
        final Collection<Player> nearbyPlayers = location.getNearbyPlayers(5);
        if (!nearbyPlayers.isEmpty()) {
            location.setDirection(nearbyPlayers.iterator().next().getLocation().getDirection());
        }

        try (MythicBukkit mythic = MythicBukkit.inst()) {
            this.activeMob = mythic.getMobManager().spawnMob(mythicMobName, location);
            this.activeMob.getEntity().getBukkitEntity().setInvisible(true);
        }

        final Core plugin = JavaPlugin.getPlugin(Core.class);
        plugin.getInjector().getInstance(LootChestManager.class).addLootChest(this);
    }

    // Start the dropping animation
    void dropItems() {
        final int dropCount = bundle.getSize();
        final Iterator<@NotNull Loot<?, ?>> iterator = bundle.iterator();
        for (int i = 0; i < dropCount; i++) {
            final Loot<?, ?> next = iterator.next();
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), false, () -> {
                dropItem(bundle.getContext(), next);
            }, dropDelay + (i * dropInterval));
        }

        long removalDelay = dropDelay + (dropCount * dropInterval);

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), activeMob::remove, removalDelay + 20L);
    }

    private void dropItem(LootContext context, Loot<?, ?> loot) {
        dropSound.play(context.getLocation());
        if (loot.award(context) instanceof Item item) {
            final Vector offset = new Vector(UtilMath.randDouble(-0.15, 0.15), UtilMath.randDouble(0.30, 0.45), UtilMath.randDouble(-0.15, 0.15));
            item.setVelocity(context.getLocation().getDirection().clone().add(offset));
        }
    }

}
