package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Singleton
@BPvPListener
public class MoltenBlast extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double speed;

    public final List<LargeFireball> fireballs = new ArrayList<>();

    private double damage;

    @Inject
    public MoltenBlast(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Molten Blast";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Shoot a large fireball that deals",
                "<stat>" + damage + "</stat> area of effect damage, and igniting any players hit",
                "for <val>" + (level * 0.5) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @UpdateEvent
    public void update() {
        Iterator<LargeFireball> it = fireballs.iterator();
        while (it.hasNext()) {
            LargeFireball fireball = it.next();
            if (fireball == null || fireball.isDead()) {
                it.remove();
                continue;
            }
            if (fireball.getLocation().getY() < 255 || !fireball.isDead()) {
                Particle.LAVA.builder().location(fireball.getLocation()).receivers(30).count(10).spawn();
            } else {
                it.remove();
            }
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);

        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);
            largeFireball.getWorld().playSound(largeFireball.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);
        }
    }


    @EventHandler
    public void onaDamage(CustomDamageEvent event) {

        if (event.getProjectile() != null) {
            Projectile fireball = event.getProjectile();
            if (fireball instanceof LargeFireball && fireball.getShooter() instanceof Player player) {

                event.setKnockback(true);
                event.setDamage(damage);
                event.setReason(getName());
                UtilServer.runTaskLater(champions, () -> event.getDamagee().setFireTicks((int) (20 * (0 + (getLevel(player) * 0.5)))), 2);

            }
        }
    }

    /*
     * Stops players from deflecting fireballs (Molten blast)
     */
    @EventHandler
    public void onDeflect(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                if (projectile instanceof LargeFireball) {
                    if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }


    @Override
    public void activate(Player player, int level) {
        LargeFireball fireball = player.launchProjectile(LargeFireball.class, player.getLocation().getDirection().multiply(speed));
        fireball.setYield(2.0F);
        fireball.setIsIncendiary(false);

        fireballs.add(fireball);

    }

    @Override
    public void loadSkillConfig(){
        speed = getConfig("speed", 2.0, Double.class);
        damage = getConfig("damage", 6.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
