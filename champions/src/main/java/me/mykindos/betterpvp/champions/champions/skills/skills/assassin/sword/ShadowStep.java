package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;

@Singleton
@BPvPListener
public class ShadowStep extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill, OffensiveSkill, DamageSkill {

    HashSet<Zombie> zombies = new HashSet<>();

    private double distance;
    private double distanceIncreasePerLevel;
    private double cooldownReduction;
    private double cooldownReductionPerLevel;
    private double damage;
    private double damageIncreasePerLevel;

    @Inject
    public ShadowStep(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Shadow Step";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dash forwards " + getValueString(this::getDistance, level) + " blocks, dealing " + getValueString(this::getDamage, level),
                "damage to anything you pass through",
                "",
                "Every hit will reduce the cooldown by " + getValueString(this::getCooldownDecrease, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getCooldownDecrease(int level) {
        return cooldownReduction + (cooldownReductionPerLevel * (level - 1));
    }

    public double getDistance(int level) {
        return distance + (distanceIncreasePerLevel * (level - 1));
    }

    public double getDamage(int level) {
        return damage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        Zombie zombie = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        zombie.setAdult();

        // Disguise the zombie as a player
        Disguise disguise = new PlayerDisguise(player.getName());
        DisguiseAPI.disguiseToAll(zombie, disguise);
        setZombieEquipment(zombie, player.getInventory());

        //this is required to clear the drops, I have no clue why.
        zombie.setCustomName(player.getDisplayName());
        zombie.setCustomNameVisible(true);

        zombie.setShouldBurnInDay(false);
        zombie.setAI(true);

        AttributeInstance zombieSpeed = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (zombieSpeed != null) zombieSpeed.setBaseValue(zombieSpeed.getDefaultValue() * 0.5);

        zombies.add(zombie);

        UtilLocation.teleportForward(player, getDistance(level), false, success -> {});
    }


    private void setZombieEquipment(Zombie zombie, PlayerInventory playerInventory) {
        // Clear existing equipment
        zombie.getEquipment().clear();

        // Set each piece of equipment
        zombie.getEquipment().setHelmet(playerInventory.getHelmet());
        zombie.getEquipment().setChestplate(playerInventory.getChestplate());
        zombie.getEquipment().setLeggings(playerInventory.getLeggings());
        zombie.getEquipment().setBoots(playerInventory.getBoots());

        // Copy inventory items (main hand)
        zombie.getEquipment().setItemInMainHand(playerInventory.getItemInMainHand());
    }

    @EventHandler
    public void onDamageEvent(EntityDamageByEntityEvent event) {

        if(event.getDamager() instanceof Zombie zombie){
            if(zombies.contains(zombie)){
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Zombie zombie) {
            if (zombies.contains(zombie)) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Player attacker) {
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1)); // Slowness for 2 seconds
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1)); // Blindness for 2 seconds

                    attacker.playSound(attacker.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);
                }
                zombie.setHealth(0);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            if (zombies.contains(zombie)) {
                zombies.remove(zombie);
                event.getDrops().clear();
                zombie.getWorld().spawnParticle(Particle.SPELL_WITCH, zombie.getLocation(), 50, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.5, Double.class);
        distance = getConfig("distance", 5.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.0, Double.class);
        cooldownReduction = getConfig("cooldownReduction", 3.0, Double.class);
        cooldownReductionPerLevel = getConfig("cooldownReductionPerLevel", 0.0, Double.class);
    }
}
