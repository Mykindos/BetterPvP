package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
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
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
public class ShadowStep extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DebuffSkill {

    WeakHashMap<Vindicator, Long> clones = new WeakHashMap<>();

    private double distance;
    private double duration;
    private double durationIncreasePerLevel;
    private int effectStrength;
    private double effectDuration;
    private double effectDurationIncreasePerLevel;

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
                "Activate by right-clicking with a Sword",
                "",
                "Move forward " + getValueString(this::getDistance, level) + " blocks, leaving a clone behind.",
                "The clone lasts for " + getValueString(this::getDuration, level) + " seconds to distract enemies.",
                "",
                "While shifting, you stay in place and the clone is",
                "sent " + getValueString(this::getDistance, level) + " blocks forward.",
                "",
                "The clone has a chance to inflict:",
                "<effect>Blindness</effect> and <effect>Slowness " + UtilFormat.getRomanNumeral(effectStrength) + "</effect>",
                "These effects last for " + getValueString(this::getEffectDuration, level) + " seconds.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds."
        };
    }

    public double getDuration(int level) {
        return duration + (durationIncreasePerLevel * (level - 1));
    }

    public double getDistance(int level) {
        return distance;
    }

    public double getEffectDuration(int level) {
        return effectDuration + (effectDurationIncreasePerLevel * (level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        Vindicator clone = (Vindicator) player.getWorld().spawnEntity(player.getLocation(), EntityType.VINDICATOR);

        Disguise disguise = new PlayerDisguise(player).setNameVisible(false);
        DisguiseAPI.disguiseToAll(clone, disguise);


        setCloneProperties(clone, player);
        clone.setMetadata("spawner", new FixedMetadataValue(champions, player.getUniqueId()));

        clones.put(clone, System.currentTimeMillis());

        UtilLocation.teleportForward(clone, 1, false, success -> {
            if (!success) {
                return;
            }
            clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);
        });
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Vindicator, Long>> it = clones.entrySet().iterator();
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

            if (UtilTime.elapsed(clones.get(clone), (long) getDuration(level) * 1000)) {
                it.remove();
                removeClone(clone);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Vindicator clone && event.getTarget() instanceof Player target && clones.containsKey(clone)) {
            final Player player = getCloneSpawner(clone);
            if (UtilEntity.getRelation(player, target) == EntityProperty.FRIENDLY) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageEvent(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Vindicator clone && clones.containsKey(clone)) {
            handleCloneDamage(player, clone);
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Vindicator clone && event.getDamager() instanceof Player player && clones.containsKey(clone)) {
            event.setCancelled(true);
            handlePlayerDamage(player, clone);
        }
    }

    private void handleCloneDamage(Player player, Vindicator clone) {
        final Player spawner = getCloneSpawner(clone);

        int level = getLevel(spawner);

        if (!(level > 0)) {
            return;
        }

        long eDuration = (long) (getEffectDuration(level) * 1000L);

        championsManager.getEffects().addEffect(player, EffectTypes.BLINDNESS, effectStrength, eDuration);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, effectStrength, eDuration);

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 2.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 2.0F, 1.0F);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You have been hit by <alt>" + spawner.getName() + "'s</alt>. clone");

        removeClone(clone);
    }

    private void handlePlayerDamage(Player player, Vindicator clone) {
        final Player spawner = getCloneSpawner(clone);

        if (UtilEntity.getRelation(spawner, player) == EntityProperty.FRIENDLY) return;

        UtilMessage.simpleMessage(player, getClassType().getName(), "You have been tricked by <alt>" + spawner.getName() + "'s</alt> clone");

        DisguiseAPI.undisguiseToAll(clone);
        removeClone(clone);
    }

    private void setCloneProperties(Vindicator clone, Player player) {

        PlayerInventory playerInventory = player.getInventory();
        clone.setAI(true);

        clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));

        // Clear existing equipment
        clone.getEquipment().clear();

        // Set each piece of equipment
        clone.getEquipment().setHelmet(playerInventory.getHelmet());
        clone.getEquipment().setChestplate(playerInventory.getChestplate());
        clone.getEquipment().setLeggings(playerInventory.getLeggings());
        clone.getEquipment().setBoots(playerInventory.getBoots());
        clone.getEquipment().setItemInMainHand(playerInventory.getItemInMainHand());


        //set target to enemy within 16 block radius
        List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 16);
        if(!nearbyEnemies.isEmpty()){
            clone.setTarget(nearbyEnemies.get(0));
        }
    }

    private void removeClone(Vindicator clone) {
        clone.getWorld().spawnParticle(Particle.SQUID_INK, clone.getLocation(), 50, 0.5, 0.5, 0.5, 0.01);
        clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 2.0F, 1.0F);

        //not sure if you need this, but adding it anyways
        DisguiseAPI.undisguiseToAll(clone);
        clone.remove();
        clones.remove(clone);
    }
   
    private Player getCloneSpawner(Vindicator clone) {
        return Bukkit.getPlayer((UUID) Objects.requireNonNull(clone.getMetadata("spawner").get(0).value()));
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
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        distance = getConfig("distance", 3.0, Double.class);
        duration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        effectStrength = getConfig("effectStrength", 1, Integer.class);
        effectDuration = getConfig("effectDuration", 1.0, Double.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 0.5, Double.class);
    }
}
