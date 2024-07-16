package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;


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
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
@BPvPListener
public class Clone extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DebuffSkill, DefensiveSkill, HealthSkill {

    WeakHashMap<Vindicator, CloneData> clones = new WeakHashMap<>();

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
                "Sacrifice " + getValueString(this::getHealthReduction, level, 100, "%", 0) + " of your health to",
                "spawn a clone that lasts for " + getValueString(this::getDuration, level) + " seconds",
                "that has <val>" + baseHealth + "</val> health",
                "",
                "Every hit your clone gets on an enemy player, ",
                "restore " + healthPerEnemyHit + " health, whilst inflicting the following effects:",
                "<effect>Blindness " + UtilFormat.getRomanNumeral(blindnessLevel) +"</effect> and <effect>Slowness "+ UtilFormat.getRomanNumeral(slownessLevel) +"</effect>, and deals knockback.",
                "",
                "These effects last for <val>" + effectDuration + "</val> seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds."
        };
    }

    public double getDuration(int level) {
        return duration + (durationIncreasePerLevel * (level - 1));
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level - 1) * healthReductionDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (player.getHealth() * healthReduction);
        UtilPlayer.slowHealth(champions, player, -proposedHealth, 5, false);

        Vindicator clone = (Vindicator) player.getWorld().spawnEntity(player.getLocation(), EntityType.VINDICATOR);

        Disguise disguise = new PlayerDisguise(player).setNameVisible(false);
        DisguiseAPI.disguiseToAll(clone, disguise);

        setCloneProperties(clone, player);

        //Target nearest enemy
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 16);
        Player target = null;
        if(!nearbyEnemies.isEmpty()){
            target = nearbyEnemies.get(0);
        }

        clones.put(clone, new CloneData(target, System.currentTimeMillis()));

        //leap the clone forward
        VelocityData velocityData = new VelocityData(clone.getLocation().getDirection(), leapStrength, false, 0.0D, 0.2D, 1.0D, true);
        UtilVelocity.velocity(clone, null, velocityData, VelocityType.CUSTOM);
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Vindicator, CloneData>> it = clones.entrySet().iterator();
        while (it.hasNext()) {
            Vindicator clone = it.next().getKey();

            if (clone == null) {
                it.remove();
                continue;
            }

            final Player player = getCloneSpawner(clone);

            if (player == null || !player.isOnline()) {
                it.remove();
                removeClone(clone);
                continue;
            }

            int level = getLevel(player);

            if (!(level > 0)) {
                it.remove();
                removeClone(clone);
                continue;
            }

            if (UtilTime.elapsed(clones.get(clone).getSpawnTime(), (long) getDuration(level) * 1000)) {
                it.remove();
                removeClone(clone);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Vindicator clone && event.getTarget() instanceof Player target && clones.containsKey(clone)) {

            //keep target if it is set
            if(!(clones.get(clone).getTarget() == null)){
                clone.setTarget(clones.get(clone).getTarget());
            }

            final Player player = getCloneSpawner(clone);
            if (UtilEntity.getRelation(player, target) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageEvent(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player && event.getDamager() instanceof Vindicator clone && clones.containsKey(clone)) {
            event.setCancelled(true);
            handleCloneDamage(player, clone);
            return;
        }
        if (event.getDamagee() instanceof Vindicator clone && event.getDamager() instanceof Player player && clones.containsKey(clone)) {
            event.setKnockback(false);
            if (UtilEntity.getRelation(getCloneSpawner(clone), player) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCloneDeath(EntityDeathEvent event){
        if(event.getEntity() instanceof Vindicator clone && clones.containsKey(clone)){
            removeClone(clone);
        }
    }

    private void handleCloneDamage(Player player, Vindicator clone) {
        final Player spawner = getCloneSpawner(clone);

        int level = getLevel(spawner);

        if (!(level > 0)) {
            return;
        }

        UtilDamage.doCustomDamage(new CustomDamageEvent(player, spawner, null, EntityDamageEvent.DamageCause.CUSTOM, 0, true, getName()));
        UtilPlayer.health(spawner, healthPerEnemyHit);

        long eDuration = (long) (this.effectDuration * 1000);
        championsManager.getEffects().addEffect(player, EffectTypes.BLINDNESS, blindnessLevel, eDuration);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, slownessLevel, eDuration);

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);
    }

    private void setCloneProperties(Vindicator clone, Player player) {
        clone.setMetadata("spawner", new FixedMetadataValue(champions, player.getUniqueId()));
        PlayerInventory playerInventory = player.getInventory();
        clone.setAI(true);
        clone.setHealth(baseHealth);

        clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));

        // Copy player equipment
        clone.getEquipment().clear();
        clone.getEquipment().setHelmet(playerInventory.getHelmet());
        clone.getEquipment().setChestplate(playerInventory.getChestplate());
        clone.getEquipment().setLeggings(playerInventory.getLeggings());
        clone.getEquipment().setBoots(playerInventory.getBoots());
        clone.getEquipment().setItemInMainHand(playerInventory.getItemInMainHand());
    }

    private void removeClone(Vindicator clone) {
        clone.getWorld().spawnParticle(Particle.SQUID_INK, clone.getLocation(), 50, 0.5, 0.5, 0.5, 0.01);
        clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0F, 1.0F);

        //not sure if you need this, but adding it anyway
        DisguiseAPI.undisguiseToAll(clone);
        clone.remove();
        clones.remove(clone);
    }
   
    private Player getCloneSpawner(Vindicator clone) {
        return Bukkit.getPlayer((UUID) Objects.requireNonNull(clone.getMetadata("spawner").get(0).value()));
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
        private final Player target;
        private final long spawnTime;
    }
}
