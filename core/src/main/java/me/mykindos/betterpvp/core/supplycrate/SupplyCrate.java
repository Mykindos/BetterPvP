package me.mykindos.betterpvp.core.supplycrate;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.cause.EnvironmentalDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;

@Getter
public class SupplyCrate extends Projectile {

    private final Dummy<?> backingEntity;
    private final ModeledEntity modeledEntity;
    @Getter(AccessLevel.NONE)
    private ActiveModel activeModel;
    private final SupplyCrateType type;
    private Iterator<Loot<?, ?>> lootIterator;
    private LootContext lootContext;

    protected SupplyCrate(Player caller, Location location, SupplyCrateType type, long crateAliveTime) {
        super(caller, type.getSize(), location, crateAliveTime);
        this.type = type;
        this.backingEntity = new Dummy<>();
        this.backingEntity.setVisible(true);
        this.backingEntity.setRenderRadius(512);
        this.backingEntity.setLocation(location);
        this.modeledEntity = ModelEngineAPI.createModeledEntity(this.backingEntity, created -> {
            this.activeModel = ModelEngineAPI.createActiveModel(type.getModelId());
            this.activeModel.setScale(type.getSize());
            this.activeModel.setHitboxScale(type.getSize());
            created.addModel(this.activeModel, true);
            created.setBaseEntityVisible(false);
            created.setSaved(false);
            created.setModelRotationLocked(true);
        });
        // todo: spawn firework particles
    }

    @Override
    public boolean isExpired() {
        return impacted && UtilTime.elapsed(impactTime, aliveTime);
    }

    public boolean hasLoot() {
        return lootIterator != null && lootIterator.hasNext();
    }

    public Loot<?, ?> consumeLoot() {
        return lootIterator.next();
    }

    @Override
    protected void onTick() {
        this.backingEntity.setLocation(location.clone().add(0, 0.01, 0));
        if (!impacted) {
            Particle.CAMPFIRE_COSY_SMOKE.builder()
                    .count(2)
                    .location(lastLocation.clone().add(0, hitboxSize * 2, 0))
                    .offset(hitboxSize / 3, hitboxSize, hitboxSize / 3)
                    .extra(0.01)
                    .receivers(128)
                    .spawn();

            if (Bukkit.getCurrentTick() % 2 == 0) {
                new SoundEffect(Sound.BLOCK_PISTON_CONTRACT, 0.4f, 3.5f).play(location);
                new SoundEffect(Sound.BLOCK_PISTON_EXTEND, 0.4f, 3.5f).play(location);
                new SoundEffect(Sound.BLOCK_CHEST_CLOSE, 0.4f, 2.5f).play(location);
            }
            return;
        }

        if (Bukkit.getCurrentTick() % 30 == 0) {
            new SoundEffect(Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 2.5f).play(location);

            // Particle walls every tick.
            final double half = hitboxSize / 2.0;
            final int density = 8; // points per edge per wall.

            Vector[] normals = {
                    new Vector(1, 0, 0),   // +X face
                    new Vector(-1, 0, 0),  // -X face
                    new Vector(0, 0, 1),   // +Z face
                    new Vector(0, 0, -1)   // -Z face
            };

            for (Vector normal : normals) {

                // Parallel axes defining the plane.
                Vector axis1 = new Vector(normal.getZ(), 0, -normal.getX()).normalize();
                Vector axis2 = new Vector(0, 1, 0);

                for (int i = 0; i <= density; i++) {
                    double a = ((double) i / density - 0.5) * hitboxSize;

                    for (int j = 0; j <= density; j++) {
                        double b = ((double) j / density - 0.5) * hitboxSize; // 2 because it's a radius

                        Vector point = location.clone().add(0, hitboxSize / 2, 0).toVector()
                                .add(normal.clone().multiply(half))
                                .add(axis1.clone().multiply(a))
                                .add(axis2.clone().multiply(b));

                        Particle.BLOCK_CRUMBLE.builder()
                                .count(1)
                                .data(Material.OAK_PLANKS.createBlockData())
                                .extra(0)
                                .location(point.toLocation(location.getWorld()))
                                .receivers(128)
                                .spawn();
                    }
                }
            }
        }
    }

    public void remove() {
        this.backingEntity.setRemoved(true);
        new SoundEffect(Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 0.4f, 1.3f).play(location);
        Particle.POOF.builder()
                .count(20)
                .location(location.add(0, this.hitboxSize, 0))
                .offset(hitboxSize, hitboxSize, hitboxSize)
                .extra(0.1)
                .receivers(60)
                .spawn();
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        // Squash entities
        if (result.getHitEntity() instanceof LivingEntity livingEntity) {
            new DamageEvent(livingEntity,
                    null,
                    null,
                    new EnvironmentalDamageCause(this.type.getDisplayName(), this.type.getDisplayName(), EntityDamageEvent.DamageCause.FALLING_BLOCK),
                    100,
                    this.type.getDisplayName()
            );
            return CollisionResult.CONTINUE;
        }
        return super.onCollide(result);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // Remove the modeled entities and stop moving
        this.modeledEntity.removeModel(type.getModelId());
        this.activeModel.destroy();
        this.activeModel = null;
        redirect(new Vector());

        // Replace the modeled entity with the crate
        this.activeModel = ModelEngineAPI.createActiveModel("supply_crate");
        this.activeModel.setScale(this.type.getSize());
        this.activeModel.setHitboxScale(this.type.getSize());
        this.modeledEntity.addModel(this.activeModel, true);

        // Award the loot and stop moving
        final LootTable lootTable = type.getLootTable();
        final LootSession session = LootSession.newSession(lootTable, Bukkit.getServer());
        this.lootContext = new LootContext(session, this.location.clone().add(0, hitboxSize, 0), this.type.getDisplayName());
        this.lootIterator = lootTable.generateLoot(lootContext).iterator();

        // Sounds
        new SoundEffect(Sound.BLOCK_CHEST_LOCKED, 0.4f, 4.5f).play(location);
    }
}
