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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
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
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Clone extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DebuffSkill, DefensiveSkill, HealthSkill {

    private final WeakHashMap<Player, CloneData> clones = new WeakHashMap<>();

    private double duration;
    private double durationIncreasePerLevel;
    private double baseHealth;

    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;

    private double healthPerEnemyHit;

    private double leapStrength;
    private double effectDuration;
    private int blindnessLevel;
    private int slownessLevel;


    @Inject
    public Clone(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Clone";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice " + getValueString(this::getHealthReduction, level, 100, "%", 0) + " of your health to send a clone",
                "that lasts for " + getValueString(this::getDuration, level) + " seconds which has " + getValueString(this::getBaseHealth, level) + " health",
                "",
                "Every hit your clone gets on an enemy player, ",
                "restore " + getValueString(this::getHealthRegen, level) + " health, whilst inflicting the following effects:",
                "<effect>Blindness " + UtilFormat.getRomanNumeral(blindnessLevel) + "</effect>, <effect>Slowness " + UtilFormat.getRomanNumeral(slownessLevel) + "</effect>, and <effect>Knockback</effect>",
                "",
                "These effects last for " + getValueString(this::getBaseEffectDuration, level) + " seconds",
                "",
                "<green>Hint:</green>",
                "This clone switches target to the player you are attacking",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds."
        };
    }

    private double getBaseHealth(int level) {
        return baseHealth;
    }

    private double getBaseEffectDuration(int level) {
        return effectDuration;
    }

    private double getHealthRegen(int level) {
        return healthPerEnemyHit;
    }

    public double getDuration(int level) {
        return duration + (durationIncreasePerLevel * (level - 1));
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level - 1) * healthReductionDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {

        //Check if player already has a clone - mainly to prevent op'd players from spamming clones
        if(clones.containsKey(player)) return;

        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (player.getHealth() * healthReduction);
        UtilPlayer.slowHealth(champions, player, -proposedHealth, 5, false);

        Vindicator clone = (Vindicator) player.getWorld().spawnEntity(player.getLocation(), EntityType.VINDICATOR);

        Disguise disguise = new PlayerDisguise(player).setNameVisible(false);
        DisguiseAPI.disguiseToAll(clone, disguise);

        setCloneProperties(clone, player);

        //leap the clone forward
        VelocityData velocityData = new VelocityData(clone.getLocation().getDirection(), leapStrength, false, 0.0D, 0.2D, 1.0D, false);
        UtilVelocity.velocity(clone, player, velocityData, VelocityType.CUSTOM);

        //Find nearby enemies relative to the clones location after teleporting
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, clone.getLocation(), 24);
        Player initTarget = null;
        if (!nearbyEnemies.isEmpty()) {
            //Pick a random nearby enemy
            Random random = new Random();
            initTarget = nearbyEnemies.get(random.nextInt(nearbyEnemies.size()));
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

            int level = getLevel(player);

            if (level <= 0) {
                it.remove();
                removeClone(clone, player);
                continue;
            }

            if (UtilTime.elapsed(clones.get(player).getDuration(), (long) getDuration(level) * 1000)) {
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
        if (event.getDamager() instanceof Player damager && clones.containsKey(damager) && event.getDamagee() instanceof Player damagee){
            clones.get(damager).getPathFinder().setTarget(damagee);
            return;
        }

        if (event.getDamagee() instanceof Player player && event.getDamager() instanceof Vindicator clone && clones.containsKey(getCloneOwner(clone))) {

            event.setDamage(0);
            event.addReason(getName());

            UtilPlayer.health(player, healthPerEnemyHit);

            sendEffects(player);
            return;
        }

        if (event.getDamagee() instanceof Vindicator clone && event.getDamager() instanceof Player player && clones.containsKey(getCloneOwner(clone))) {
            if (UtilEntity.getRelation(getCloneOwner(clone), player) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeathEvent(EntityDeathEvent event){
        if(event.getEntity() instanceof Vindicator clone && clones.containsKey(getCloneOwner(clone))){
            removeClone(clone, getCloneOwner(clone));
            return;
        }
        if(event.getEntity() instanceof Player player && clones.containsKey(player)){
            removeClone(clones.get(player).getClone(), player);
        }
    }

    private void sendEffects(Player player) {
        long eDuration = (long) (this.effectDuration * 1000);
        championsManager.getEffects().addEffect(player, EffectTypes.BLINDNESS, blindnessLevel, eDuration);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, slownessLevel, eDuration);

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);
    }

    private void setCloneProperties(Vindicator clone, Player player) {
        clone.setMetadata("owner", new FixedMetadataValue(champions, player.getUniqueId()));
        PlayerInventory playerInventory = player.getInventory();
        clone.setAI(true);
        clone.setHealth(baseHealth);

        clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));

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
        if(clone == null) {
            return null;
        }

        if(!clone.hasMetadata("owner")) {
            return null;
        }

        return Bukkit.getPlayer((UUID) Objects.requireNonNull(clone.getMetadata("owner").getFirst().value()));
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (UtilPlayer.getMaxHealth(player) - (UtilPlayer.getMaxHealth(player) * healthReduction));

        if (proposedHealth <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);

        baseHealthReduction = getConfig("baseHealthReduction", 0.3, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.05, Double.class);

        baseHealth = getConfig("baseHealth", 10.0, Double.class);

        healthPerEnemyHit = getConfig("healthPerEnemyHit", 1.0, Double.class);

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
