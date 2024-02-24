package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.events.AnimationPlayEvent;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.events.RemoveModelEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Optional;

@PluginAdapter("ModelEngine")
@PluginAdapter("MythicMobs")
@BPvPListener
@Singleton
public class HealthBarAdapter implements Listener {

    private final Multimap<Entity, HealthBar> healthBars = ArrayListMultimap.create();

    @Inject
    @Config(path = "pvp.mythic-health-bars.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    private Core core;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobSpawn(final AddModelEvent event) {
        if (!enabled) {
            return;
        }

        final BaseEntity<?> base = event.getTarget().getBase();
        if (!(base instanceof BukkitEntity bukkitEntity)) {
            return;
        }

        spawnHealthBar(bukkitEntity.getOriginal(), event.getModel());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(final CustomDamageEvent event) {
        if (!enabled) {
            return;
        }

        this.healthBars.get(event.getDamagee()).forEach(HealthBar::update);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(final EntityRemoveFromWorldEvent event) {
        if (!UtilEntity.isRemoved(event.getEntity()) || !UtilEntity.getRemovalReason(event.getEntity()).isDestroy()) {
            return;
        }

        this.healthBars.removeAll(event.getEntity()).forEach(HealthBar::despawn);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final AnimationPlayEvent event) {
        if (!enabled || !event.getProperty().getName().equals("death")) {
            return;
        }

        Bukkit.getScheduler().runTask(core, () -> this.healthBars.values().removeIf(healthBar -> {
            if (healthBar.model().equals(event.getModel())) {
                healthBar.despawn();
                return true;
            } else {
                return false;
            }
        }));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModelRemove(final RemoveModelEvent event) {
        final BaseEntity<?> base = event.getModel().getModeledEntity().getBase();
        if (!(base instanceof BukkitEntity bukkitEntity)) {
            return;
        }

        this.healthBars.removeAll(bukkitEntity.getOriginal()).forEach(HealthBar::despawn);
    }

    @UpdateEvent
    public void ticker() {
        if (!enabled) {
            return;
        }

        new ArrayList<>(this.healthBars.values()).forEach(HealthBar::update);
    }

    @EventHandler
    public void onLoad(ModelRegistrationEvent event) {
        if (event.getPhase() != ModelGenerator.Phase.FINISHED) {
            return;
        }
        UtilServer.runTask(core, () -> {
            for (World world : Bukkit.getServer().getWorlds()) {
                for (LivingEntity entity : world.getLivingEntities()) {
                    final ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
                    if (modeledEntity == null) {
                        continue;
                    }

                    for (ActiveModel model : modeledEntity.getModels().values()) {
                        spawnHealthBar(entity, model);
                    }
                }
            }
        });
    }

    private void spawnHealthBar(Entity entity, ActiveModel model) {
        if (entity.isDead() || !entity.isValid() ||  !(entity instanceof LivingEntity living)) {
            return; // Skip non-living
        }

        Bukkit.getScheduler().runTaskLater(core, () -> {
            final Optional<ModelBone> opt = model.getBone("bpvp_health");
            if (opt.isEmpty()) {
                return; // Skip non-existing health bar
            }

            final ModelBone bone = opt.get();
            final Location location = bone.getLocation();
            final TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, ent -> {
                ent.setPersistent(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            });

            UtilEntity.setViewRangeBlocks(display, 10);
            final HealthBar healthBar = new HealthBar(display, living, model, bone);
            healthBar.update();
            healthBars.put(entity, healthBar);
        }, 1L);
    }

}
