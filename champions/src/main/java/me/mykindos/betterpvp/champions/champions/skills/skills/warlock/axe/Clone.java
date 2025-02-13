package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.utilities.MobPathfinder;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Clone extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DebuffSkill, DefensiveSkill, HealthSkill {

    private final WeakHashMap<Player, CloneData> clones = new WeakHashMap<>();

    @Getter
    private double duration;
    private double health;
    private double leapStrength;
    private double effectDuration;
    private int slownessLevel;
    private int blindnessLevel;

    @Inject
    public Clone(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Clone";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Summon a clone that lasts for <val>" + getDuration() + "</val> seconds",
                "and has <val>" + getHealth() + "</val> health. This clone switches target",
                "to the player you are attacking.",
                "",
                "Every hit your clone gets on an enemy player",
                "inflicts the following effects: <effect>Blindness " + UtilFormat.getRomanNumeral(blindnessLevel) + "</effect>,",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessLevel) + "</effect>, and <effect>Knockback</effect>",
                "",
                "These effects last for <val>" + getEffectDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown() + "</val> seconds",
        };
    }

    private double getHealth() {
        return health;
    }

    private double getEffectDuration() {
        return effectDuration;
    }

    @Override
    public void activate(Player player) {

        if (championsManager.getEffects().hasEffect(player, EffectTypes.PROTECTION)) {
            UtilMessage.message(player, "Clone", "You cannot use this skill with protection");
            return;
        }
        //Check if player already has a clone - mainly to prevent op'd players from spamming clones
        if (clones.containsKey(player)) return;

        Disguise disguise = new PlayerDisguise(player).setNameVisible(false);
        DisguiseAPI.disguiseNextEntity(disguise); // Apparently fixes a client crash

        Vindicator clone = (Vindicator) player.getWorld().spawnEntity(player.getLocation(), EntityType.VINDICATOR);

        setCloneProperties(clone, player);

        //leap the clone forward
        VelocityData velocityData = new VelocityData(clone.getLocation().getDirection(), leapStrength, false, 0.0D, 0.2D, 1.0D, false);
        UtilVelocity.velocity(clone, player, velocityData, VelocityType.CUSTOM);

        //Find nearby enemies relative to the clones location after teleporting
        List<LivingEntity> nearbyEnemies = UtilEntity.getNearbyEnemies(player, clone.getLocation(), 24);
        nearbyEnemies.remove(clone);
        nearbyEnemies.removeIf(entity -> !player.canSee(entity));
        nearbyEnemies.removeIf(entity -> entity instanceof Vindicator c && UtilEntity.getRelation(getCloneOwner(c), player) == EntityProperty.FRIENDLY);

        LivingEntity initTarget = null;
        if (!nearbyEnemies.isEmpty()) {
            //Pick a random nearby enemy
            initTarget = nearbyEnemies.get(UtilMath.randomInt(nearbyEnemies.size()));
        }

        MobPathfinder mobPathfinder = new MobPathfinder(champions, clone, initTarget);
        clones.put(player, new CloneData(clone, mobPathfinder, System.currentTimeMillis()));
        Bukkit.getMobGoals().addGoal(clone, 0, mobPathfinder);
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Player, CloneData>> it = clones.entrySet().iterator();
        while (it.hasNext()) {
            Player player = it.next().getKey();
            Vindicator clone = clones.get(player).getClone();

            if (player == null || !player.isOnline()) {
                it.remove();
                removeClone(clone, player);
                continue;
            }

            if (clone == null) {
                it.remove();
                continue;
            }

            if (!hasSkill(player)) {
                it.remove();
                removeClone(clone, player);
                continue;
            }

            if (UtilTime.elapsed(clones.get(player).getDuration(), (long) getDuration() * 1000)) {
                it.remove();
                removeClone(clone, player);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Vindicator clone && event.getTarget() instanceof Player target && clones.containsKey(getCloneOwner(clone))) {
            final Player player = getCloneOwner(clone);
            if (UtilEntity.getRelation(player, target) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCustomDamageEvent(CustomDamageEvent event) {

        //Lock/Switch clone onto player being damaged by its owner.
        if (event.getDamager() instanceof Player damager && clones.containsKey(damager)) {

            if (event.getDamagee() instanceof Vindicator clone && UtilEntity.getRelation(getCloneOwner(clone), damager) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
                return;
            }

            clones.get(damager).getPathFinder().setTarget(event.getDamagee());
            return;
        }

        if (event.getDamager() instanceof Vindicator clone) {
            Player cloneOwner = getCloneOwner(clone);
            if (clones.containsKey(cloneOwner)) {

                event.setDamage(0);
                event.addReason(getName());

                sendEffects(event.getDamagee());
                return;
            }
        }

        if (event.getDamagee() instanceof Vindicator clone && event.getDamager() instanceof Player player && clones.containsKey(getCloneOwner(clone))) {
            if (UtilEntity.getRelation(getCloneOwner(clone), player) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeathEvent(EntityDeathEvent event) {
        if (event.getEntity() instanceof Vindicator clone && clones.containsKey(getCloneOwner(clone))) {
            removeClone(clone, getCloneOwner(clone));
            return;
        }
        if (event.getEntity() instanceof Player player && clones.containsKey(player)) {
            removeClone(clones.get(player).getClone(), player);
        }
    }

    private void sendEffects(LivingEntity target) {
        long eDuration = (long) (this.effectDuration * 1000);
        championsManager.getEffects().addEffect(target, EffectTypes.BLINDNESS, blindnessLevel, eDuration);
        championsManager.getEffects().addEffect(target, EffectTypes.SLOWNESS, slownessLevel, eDuration);

        if (target instanceof Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        }

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);
    }

    private void setCloneProperties(Vindicator clone, Player player) {
        clone.setMetadata("owner", new FixedMetadataValue(champions, player.getUniqueId()));
        PlayerInventory playerInventory = player.getInventory();
        clone.setAI(true);
        clone.setHealth(health);

        clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));

        // Copy player equipment
        clone.getEquipment().clear();
        clone.getEquipment().setHelmet(playerInventory.getHelmet());
        clone.getEquipment().setChestplate(playerInventory.getChestplate());
        clone.getEquipment().setLeggings(playerInventory.getLeggings());
        clone.getEquipment().setBoots(playerInventory.getBoots());
        clone.getEquipment().setItemInMainHand(playerInventory.getItemInMainHand());
    }

    private void removeClone(Vindicator clone, Player player) {
        clone.getWorld().spawnParticle(Particle.SQUID_INK, clone.getLocation(), 50, 0.5, 0.5, 0.5, 0.01);
        clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0F, 1.0F);

        //Remove disguise
        DisguiseAPI.undisguiseToAll(clone);
        clone.remove();
        clones.remove(player);
    }

    private Player getCloneOwner(Vindicator clone) {
        if (clone == null) {
            return null;
        }

        if (!clone.hasMetadata("owner")) {
            return null;
        }

        return Bukkit.getPlayer((UUID) Objects.requireNonNull(clone.getMetadata("owner").getFirst().value()));
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 3.0, Double.class);
        health = getConfig("health", 10.0, Double.class);
        leapStrength = getConfig("leapStrength", 2.0, Double.class);
        blindnessLevel = getConfig("blindnessLevel", 2, Integer.class);
        slownessLevel = getConfig("slownessLevel", 1, Integer.class);
        effectDuration = getConfig("effectDuration", 1.0, Double.class);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class CloneData {
        private final Vindicator clone;
        private final MobPathfinder pathFinder;
        private final long duration;
    }
}
