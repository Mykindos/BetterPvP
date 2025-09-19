package me.mykindos.betterpvp.core.effects.projectile;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.ReturningLinkProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

import java.util.Collection;

public class VineProjectile extends ReturningLinkProjectile {

    private final int poisonAmplifier;
    private final long poisonDuration;
    private final String name;
    private final EffectManager effectManager;
    private boolean finished;

    public VineProjectile(Player caster, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed, int poisonAmplifier, long poisonDuration, String name) {
        super(caster, hitboxSize, location, aliveTime, pullTime, pullSpeed);
        this.poisonAmplifier = poisonAmplifier;
        this.poisonDuration = poisonDuration;
        this.name = name;
        this.effectManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(EffectManager.class);
    }

    @Override
    protected void onTick() {
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        final BlockData data = Material.VINE.createBlockData();
        for (Location point : interpolateLine()) {
            Particle.BLOCK.builder()
                    .count(3)
                    .extra(0)
                    .data(data)
                    .offset(0.5, 0.5, 0.5)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }
        super.onTick();

        if (finished) {
            return;
        }

        if (target != null && (hasFinishedPulling() || UtilBlock.isGrounded(target))) {
            effectManager.addEffect(target, caster, EffectTypes.POISON, name, poisonAmplifier, poisonDuration, false);
            finished = true;
        }
    }

    @Override
    protected Display item() {
        return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.AIR.createBlockData());
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected Display createLink(double height) {
        final Location location = lead.getLocation();
        return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.KELP_PLANT.createBlockData());
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-1, 1, -1);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalZ(0f);
            transformation.getScale().set(2, height, 2);
            spawned.setTransformation(transformation);

            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected SoundEffect pullSound() {
        return new SoundEffect(Sound.BLOCK_WET_GRASS_BREAK, 0f, 1f);
    }

    @Override
    protected SoundEffect pushSound() {
        return new SoundEffect(Sound.BLOCK_WET_GRASS_PLACE, 2f, 1f);
    }

    @Override
    protected SoundEffect impactSound() {
        return new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f, 1f);
    }
}
