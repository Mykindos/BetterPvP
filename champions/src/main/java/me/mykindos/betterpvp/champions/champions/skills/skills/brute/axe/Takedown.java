package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Takedown extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DamageSkill, DebuffSkill, MovementSkill {

    private final TaskScheduler taskScheduler;

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private double damage;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int slownessStrength;
    private double recoilDamage;
    private double recoilDamageIncreasePerLevel;
    private double damageIncreasePerLevel;
    private double velocityStrength;
    private double fallDamageLimit;

    @Inject
    public Takedown(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Takedown";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component slowness = Component.text(
                UtilFormat.getRomanNumeral(slownessStrength),
                NamedTextColor.YELLOW
        );
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.brute.takedown.description",
                damage,
                slowness,
                duration,
                cooldown
        );
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRecoilDamage(int level) {
        return recoilDamage + ((level - 1) * recoilDamageIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @UpdateEvent
    public void checkCollision() {

        Iterator<Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (player.isDead()) {
                it.remove();
                continue;
            }

            final Location midpoint = UtilPlayer.getMidpoint(player).clone();

            Vector velocity = player.getVelocity().normalize().multiply(0.5);
            if (!Double.isFinite(velocity.getX()) || !Double.isFinite(velocity.getY()) || !Double.isFinite(velocity.getZ())) {
                continue;
            }

            final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(midpoint,
                            midpoint.clone().add(velocity),
                            (float) 0.9,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

            if (hit.isPresent()) {
                if (hit.get().getType() == EntityType.ARMOR_STAND || hit.get().hasMetadata("AlmPet")) {
                    continue;
                }
                it.remove();
                doTakedown(player, hit.get());
                continue;
            }


            if (UtilBlock.isGrounded(player) && UtilTime.elapsed(next.getValue(), 750L)) {
                it.remove();
            }

            if (player.getGameMode() == GameMode.SPECTATOR) {
                it.remove();
            }
        }

    }

    public void doTakedown(Player player, LivingEntity target) {
        int level = getLevel(player);

        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameAsComponent(target, player), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilDamage.doDamage(new DamageEvent(target, player, null, new SkillDamageCause(this), getDamage(level), "Takedown"));

        UtilMessage.message(target, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameAsComponent(player, target), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilDamage.doDamage(new DamageEvent(player, target, null, new SkillDamageCause(this), getRecoilDamage(level), "Takedown Recoil"));

        long duration = (long) (getDuration(level) * 1000L);
        championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(target, player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, slownessStrength, duration);
        championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, slownessStrength, duration);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 1.2F, 0.5f);
    }

    @Override
    public boolean canUse(Player p) {

        if (UtilBlock.isGrounded(p)) {
            UtilMessage.message(p, getClassType().getDisplayName(), "champions.skill.brute.takedown.grounded", getDisplayName().color(NamedTextColor.GREEN));
            return false;
        }

        return true;
    }

    @Override
    public boolean activate(Player player, int leel) {
        Vector vec = player.getLocation().getDirection();
        VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 0.0D, 0.4D, 0.6D, false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
        taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
            Player target = Bukkit.getPlayer(uuid);
            if(target != null) {
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                        250L, true, true, UtilBlock::isGrounded);
            }
        }, 1000));
        active.put(player, System.currentTimeMillis());
        return true;
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 4, Integer.class);
        recoilDamage = getConfig("recoilDamage", 0.0, Double.class);
        recoilDamageIncreasePerLevel = getConfig("recoilDamageIncreasePerLevel", 0.0, Double.class);
        velocityStrength = getConfig("velocityStrength", 1.5, Double.class);
        fallDamageLimit = getConfig("fallDamageLimit", 4.0, Double.class);
    }
}
