package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@BPvPListener
public class MoltenBlast extends Skill implements InteractSkill, CooldownSkill {

    @Inject
    @Config(path="skills.paladin.moltenblast.speed", defaultValue = "2")
    private double speed;

    public final List<LargeFireball> fireballs = new ArrayList<>();

    @Inject
    public MoltenBlast(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }


    @Override
    public String getName() {
        return "Molten Blast";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to Activate",
                "",
                "Shoot a large fireball that deals",
                "area of effect damage, and igniting any players hit",
                "for " + ChatColor.GREEN + ((level * 0.5)) + ChatColor.GRAY + " seconds",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
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
                Particle.LAVA.builder().location(fireball.getLocation()).receivers(30).extra(0).spawn();
            } else {
                it.remove();
            }
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);
        }
    }


    @EventHandler
    public void onaDamage(CustomDamageEvent event) {

        if (event.getProjectile() != null) {
            Projectile fireball = event.getProjectile();
            if (fireball instanceof LargeFireball && fireball.getShooter() instanceof Player player) {

                event.setKnockback(true);
                event.setDamage(6);
                event.setReason(getName());
                UtilServer.runTaskLater(clans, () -> event.getDamagee().setFireTicks((int) (20 * (0 + (getLevel(player) * 0.5)))), 2);

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

        return 23 - ((level - 1) * 2);
    }


    @Override
    public void activate(Player player, int level) {
        LargeFireball fireball = player.launchProjectile(LargeFireball.class, player.getLocation().getDirection().multiply(speed));
        fireball.setYield(2.0F);
        fireball.setIsIncendiary(false);

        fireballs.add(fireball);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
