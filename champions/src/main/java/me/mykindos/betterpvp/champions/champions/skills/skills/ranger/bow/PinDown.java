package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PinDown extends Skill implements InteractSkill, CooldownSkill, Listener, DebuffSkill, OffensiveSkill {

    private final WeakHashMap<Arrow, Player> arrows = new WeakHashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int slownessStrength;

    @Inject
    public PinDown(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Pin Down";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Left click with a Bow to activate",
                "",
                "Quickly launch an arrow that gives enemies",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level-1));
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            UtilMessage.message(player, getName(), "You need at least <alt2>1 Arrow</alt2> to use this skill.");
            return false;
        }

        return super.canUse(player);
    }

    @Override
    public void activate(Player player, int level) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            UtilInventory.remove(player, Material.ARROW, 1);
        }

        Arrow proj = player.launchProjectile(Arrow.class);
        proj.setShooter(player);
        arrows.put(proj, player);

        proj.setVelocity(player.getLocation().getDirection().multiply(1.6D));
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
    }

    @UpdateEvent
    public void onTick() {
        arrows.entrySet().removeIf(entry -> {
            final Arrow arrow = entry.getKey();
            final Player shooter = entry.getValue();
            if (arrow.isDead() || arrow.isOnGround() || shooter == null || !shooter.isOnline()) {
                return true;
            }

            if (arrow.getTicksLived() > 5 * 20) {
                arrow.remove();
                UtilMessage.message(shooter, getName(), "You missed <alt>%s</alt>.", getName());
                return true;
            }

            new ParticleBuilder(Particle.CRIT)
                    .location(arrow.getLocation())
                    .count(1)
                    .offset(0, 0, 0)
                    .extra(0)
                    .receivers(60)
                    .spawn();

            return false;
        });
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow arrow) || !arrows.containsKey(arrow)) {
            return;
        }

        final Player shooter = arrows.get(arrow);
        if (shooter == null || !shooter.isOnline()) {
            return;
        }

        final Entity entity = event.getHitEntity();
        if (!(entity instanceof LivingEntity target)) {
            UtilMessage.message(shooter, getName(), "You missed <alt>%s</alt>.", getName());
            return;
        }

        var canHurtEvent = UtilServer.callEvent(new EntityCanHurtEntityEvent(shooter, target));
        if(canHurtEvent.getResult() == Event.Result.DENY){
            return;
        }

        final int level = getLevel(shooter);
        championsManager.getEffects().addEffect(target, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration(level) * 1000));
        championsManager.getEffects().addEffect(target, EffectTypes.NO_JUMP, (long) (getDuration(level) * 1000));
        UtilMessage.message(shooter, getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", target.getName(), getName(), level);
        UtilMessage.message(target, getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", shooter.getName(), getName(), level);
        arrows.remove(arrow);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 4, Integer.class);
    }
}
