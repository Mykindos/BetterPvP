package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;


import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
public class Clone extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DebuffSkill {

    WeakHashMap<PiglinBrute, Long> clones = new WeakHashMap<>();

    private double duration;
    private double durationIncreasePerLevel;
    private double baseHealth;
    private double healthIncreasePerLevel;
    private double teleportDistance;

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
                "Spawn a clone that lasts for " + getValueString(this::getDuration, level) + " seconds",
                "that has " + getValueString(this::getCloneHealth, level) + " health",
                "",
                "Every hit your clone gets on an enemy player, they inflict:",
                "<effect>Blindness I</effect> and <effect>Slowness I</effect>, and deals knockback.",
                "These effects last for 1 second.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds."
        };
    }

    public double getDuration(int level) {
        return duration + (durationIncreasePerLevel * (level - 1));
    }

    public double getCloneHealth(int level) {
        return baseHealth + (healthIncreasePerLevel * (level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        PiglinBrute clone = (PiglinBrute) player.getWorld().spawnEntity(player.getLocation(), EntityType.PIGLIN_BRUTE);

        Disguise disguise = new PlayerDisguise(player).setNameVisible(false);
        DisguiseAPI.disguiseToAll(clone, disguise);


        setCloneProperties(clone, player);
        clone.setMetadata("spawner", new FixedMetadataValue(champions, player.getUniqueId()));

        clones.put(clone, System.currentTimeMillis());

        //teleport the clone forward slightly
        UtilLocation.teleportForward(clone, teleportDistance, false, success -> {});
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<PiglinBrute, Long>> it = clones.entrySet().iterator();
        while (it.hasNext()) {
            PiglinBrute clone = it.next().getKey();

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

            if (UtilTime.elapsed(clones.get(clone), (long) getDuration(level) * 1000)) {
                it.remove();
                removeClone(clone);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof PiglinBrute clone && event.getTarget() instanceof Player target && clones.containsKey(clone)) {
            final Player player = getCloneSpawner(clone);
            if (UtilEntity.getRelation(player, target) == EntityProperty.FRIENDLY) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageEvent(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof PiglinBrute clone && clones.containsKey(clone)) {
            event.setCancelled(true);
            handleCloneDamage(player, clone);
            return;
        }
        if (event.getEntity() instanceof PiglinBrute clone && event.getDamager() instanceof Player player && clones.containsKey(clone)) {
            if (UtilEntity.getRelation(getCloneSpawner(clone), player) == EntityProperty.FRIENDLY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCloneDeath(EntityDeathEvent event){
        if(event.getEntity() instanceof PiglinBrute clone && clones.containsKey(clone)){
            removeClone(clone);
        }
    }

    private void handleCloneDamage(Player player, PiglinBrute clone) {
        final Player spawner = getCloneSpawner(clone);

        int level = getLevel(spawner);

        if (!(level > 0)) {
            return;
        }

        UtilDamage.doCustomDamage(new CustomDamageEvent(player, spawner, null, EntityDamageEvent.DamageCause.CUSTOM, 0, true, getName()));

        championsManager.getEffects().addEffect(player, EffectTypes.BLINDNESS, 1, 1000L);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, 1, 1000L);

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);
    }

    private void setCloneProperties(PiglinBrute clone, Player player) {
        PlayerInventory playerInventory = player.getInventory();
        clone.setAI(true);
        clone.setHealth(getCloneHealth(getLevel(player)));

        clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));

        // Copy player equipment
        clone.getEquipment().clear();
        clone.getEquipment().setHelmet(playerInventory.getHelmet());
        clone.getEquipment().setChestplate(playerInventory.getChestplate());
        clone.getEquipment().setLeggings(playerInventory.getLeggings());
        clone.getEquipment().setBoots(playerInventory.getBoots());
        clone.getEquipment().setItemInMainHand(playerInventory.getItemInMainHand());

        //Target nearest enemy
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 16);
        if(!nearbyEnemies.isEmpty()){
            clone.setTarget(nearbyEnemies.get(0));
        }
    }

    private void removeClone(PiglinBrute clone) {
        clone.getWorld().spawnParticle(Particle.SQUID_INK, clone.getLocation(), 50, 0.5, 0.5, 0.5, 0.01);
        clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0F, 1.0F);

        //not sure if you need this, but adding it anyway
        DisguiseAPI.undisguiseToAll(clone);
        clone.remove();
        clones.remove(clone);
    }
   
    private Player getCloneSpawner(PiglinBrute clone) {
        return Bukkit.getPlayer((UUID) Objects.requireNonNull(clone.getMetadata("spawner").get(0).value()));
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
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        baseHealth = getConfig("baseHealth", 5.0, Double.class);
        healthIncreasePerLevel = getConfig("healthIncreasePerLevel", 2.5, Double.class);
        teleportDistance = getConfig("healthIncreasePerLevel", 2.0, Double.class);
    }
}
