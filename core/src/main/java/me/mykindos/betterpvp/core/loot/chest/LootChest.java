package me.mykindos.betterpvp.core.loot.chest;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.loot.AwardStrategy;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

// Only usable with MythicMobs on
public class LootChest implements AwardStrategy {

    private final String mythicMobName;
    private final SoundEffect dropSound;
    private final long dropDelay;
    private final long dropInterval;

    public LootChest(String mythicMobName, SoundEffect dropSound, long dropDelay, long dropInterval) {
        this.mythicMobName = mythicMobName;
        this.dropSound = dropSound;
        this.dropDelay = dropDelay;
        this.dropInterval = dropInterval;
    }

    // Spawn the chest entity
    @Override
    public void award(LootBundle bundle) {
        final Location location = bundle.getContext().getLocation().clone().add(0, 1, 0);
        location.setDirection(location.getDirection().multiply(-1));

        ActiveMob activeMob = MythicBukkit.inst().getMobManager().spawnMob(mythicMobName, location);
        final Entity entity = activeMob.getEntity().getBukkitEntity();
        entity.setInvisible(true);
        entity.setMetadata("bundle", new FixedMetadataValue(JavaPlugin.getPlugin(Core.class), bundle));

        final Core plugin = JavaPlugin.getPlugin(Core.class);
        plugin.getInjector().getInstance(LootChestManager.class).addLootChest(this, activeMob);
    }

    // Start the dropping animation
    void dropItems(ActiveMob activeMob) {
        final Entity entity = activeMob.getEntity().getBukkitEntity();
        final LootBundle bundle = (LootBundle) Objects.requireNonNull(entity.getMetadata("bundle").getFirst().value());

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
            final Vector direction = context.getLocation().clone().getDirection().clone().multiply(0.2 + Math.random() * 0.15);
            direction.rotateAroundY(Math.toRadians(Math.random() * 25));
            direction.setY(0.2 + Math.random() * 0.15);
            item.setVelocity(direction);
        }
    }

}
