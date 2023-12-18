package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class Leech extends PrepareSkill implements CooldownSkill {

    private final List<LeechData> leechData = new ArrayList<>();
    private final List<LeechData> removeList = new ArrayList<>();

    private double baseRange;

    private double rangeIncreasePerLevel;

    private double baseMaxRangeFromCaster;

    private double maxRangeFromCasterIncreasePerLevel;

    private double baseLeechedHealth;

    private double leachedHealthIncreasePerLevel;


    @Inject
    public Leech(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Leech";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Create a soul link between all enemies within",
                "<stat>" + getRange(level) + "</stat> blocks of your target, and all enemies within",
                "<stat>" + getRange(level) + "</stat> blocks of them and within <stat>" + getMaxRangeFromCaster(level) + "</stat> blocks of you",
                "",
                "Linked targets have <stat>" + getLeechedHealth(level) + "</stat> health leeched per second",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getRange(int level) {
        return baseRange + level * rangeIncreasePerLevel;
    }

    public double getMaxRangeFromCaster(int level) {
        return baseMaxRangeFromCaster + level * maxRangeFromCasterIncreasePerLevel;
    }

    public double getLeechedHealth(int level) {
        return baseLeechedHealth + level * leachedHealthIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!active.contains(damager.getUniqueId())) return;

        int level = getLevel(damager);
        if (level > 0) {
            leechData.add(new LeechData(damager, damager, event.getDamagee()));
            chainEnemies(damager, event.getDamagee());
            active.remove(damager.getUniqueId());

            championsManager.getCooldowns().removeCooldown(damager, getName(), true);
            championsManager.getCooldowns().use(damager, getName(), getCooldown(level), showCooldownFinished());
        }

    }

    private void chainEnemies(Player player, LivingEntity link) {
        int level = getLevel(player);
        List<LivingEntity> temp = new ArrayList<>();
        for (var entAData : UtilEntity.getNearbyEntities(player, link.getLocation(), getRange(level), EntityProperty.ENEMY)) {
            LivingEntity entA = entAData.get();
            if (isNotLinked(player, entA)) {
                leechData.add(new LeechData(player, link, entA));
                temp.add(entA);
            }


        }

        for (LivingEntity entA : temp) {
            for (var entBData : UtilEntity.getNearbyEntities(player, entA.getLocation(), getRange(level), EntityProperty.ENEMY)) {
                LivingEntity entB = entBData.get();
                if (isNotLinked(player, entB)) {
                    leechData.add(new LeechData(player, entA, entB));
                }
            }
        }
    }

    private void removeLinks(LivingEntity link) {
        List<LivingEntity> children = new ArrayList<>();
        leechData.forEach(leech -> {
            if (leech.getLinkedTo().getUniqueId().equals(link.getUniqueId()) || leech.getTarget().getUniqueId().equals(link.getUniqueId())) {
                children.add(leech.getTarget());
                children.add(leech.getLinkedTo());
                removeList.add(leech);
            }
        });

        children.forEach(ent -> {
            leechData.forEach(leech -> {
                if (leech.getLinkedTo().getUniqueId().equals(ent.getUniqueId()) || leech.getTarget().getUniqueId().equals(ent.getUniqueId())) {
                    removeList.add(leech);
                }
            });
        });
    }

    private void breakChain(LeechData leech) {
        leechData.forEach(l -> {
            if (l.getOwner().getUniqueId().equals(leech.getOwner().getUniqueId())) {
                removeList.add(l);
            }
        });
    }

    private boolean isNotLinked(Player player, LivingEntity ent) {
        if (player.equals(ent)) return false;
        for (LeechData leech : leechData) {
            if (leech.owner.equals(player)) {
                if (leech.linkedTo.equals(ent) || leech.target.equals(ent)) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @UpdateEvent
    public void onLeech() {
        if (!removeList.isEmpty()) {
            leechData.removeIf(removeList::contains);
            removeList.clear();
        }
    }

    @UpdateEvent(delay = 250)
    public void chain() {
        for (LeechData leech : leechData) {
            if (leech.getLinkedTo() == null || leech.getTarget() == null || leech.getOwner() == null) {
                removeList.add(leech);
                continue;
            }

            if (leech.getLinkedTo().isDead() || leech.getOwner().isDead() || leech.getLinkedTo().isDead()) {
                if (leech.getOwner().isDead()) {
                    breakChain(leech);
                }
                removeList.add(leech);
                continue;
            }
            int level = getLevel(leech.getOwner());
            if (leech.getTarget().getLocation().distance(leech.getLinkedTo().getLocation()) > getRange(level)
                    || leech.getTarget().getLocation().distance(leech.getOwner().getLocation()) > getMaxRangeFromCaster(level)) {
                if (leech.getLinkedTo().getUniqueId().equals(leech.getOwner().getUniqueId())) {
                    breakChain(leech);
                }
                removeList.add(leech);
            }

        }
    }

    @UpdateEvent(delay = 125)
    public void display() {
        for (LeechData leech : leechData) {
            if (leech.getLinkedTo() == null || leech.getTarget() == null || leech.getOwner() == null) {
                continue;
            }

            Location loc = leech.getLinkedTo().getLocation();
            Vector v = leech.getTarget().getLocation().toVector().subtract(loc.toVector());
            double distance = leech.getLinkedTo().getLocation().distance(leech.getTarget().getLocation());
            int level = getLevel(leech.getOwner());
            boolean remove = false;
            if (distance > getRange(level)) continue;
            for (double i = 0.5; i < distance; i += 0.5) {

                v.multiply(i);
                loc.add(v);
                if (UtilBlock.solid(loc.getBlock()) && UtilBlock.solid(loc.clone().add(0, 1, 0).getBlock())) {
                    remove = true;
                }
                Particle.REDSTONE.builder().location(loc.clone().add(0, 0.7, 0)).receivers(30).color(230, 0, 0).extra(0).spawn();
                loc.subtract(v);
                v.normalize();

            }

            if (remove) {
                removeList.add(leech);
            }

        }
    }

    @UpdateEvent(delay = 1000)
    public void dealDamage() {
        for (LeechData leech : leechData) {
            int level = getLevel(leech.getOwner());
            CustomDamageEvent leechDmg = new CustomDamageEvent(leech.getTarget(), leech.getOwner(), null, EntityDamageEvent.DamageCause.MAGIC, getLeechedHealth(level), false, getName());
            leechDmg.setIgnoreArmour(true);
            UtilDamage.doCustomDamage(leechDmg);
            UtilPlayer.health(leech.getOwner(), getLeechedHealth(level));
        }
    }

    @EventHandler
    public void removeOnDeath(EntityDeathEvent e) {
        removeLinks(e.getEntity());
    }

    @EventHandler
    public void onEffectClear(EffectClearEvent event) {
        leechData.forEach(leechData -> {
            if (leechData.getTarget().equals(event.getPlayer())) {
                removeList.add(leechData);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeLinks(event.getPlayer());
    }


    @Override
    public void loadSkillConfig(){
        baseRange = getConfig("baseRange", 7.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 0.0, Double.class);

        baseLeechedHealth = getConfig("baseLeechedHealth", 1.0, Double.class);
        leachedHealthIncreasePerLevel = getConfig("leachedHealthIncreasePerLevel", 0.0, Double.class);

        baseMaxRangeFromCaster = getConfig("baseMaxRangeFromCaster", 21.0, Double.class);
        maxRangeFromCasterIncreasePerLevel = getConfig("baseMaxRangeFromCaster", 0.0, Double.class);
    }

    @Data
    private static class LeechData {
        private final Player owner;

        private final LivingEntity linkedTo;
        private final LivingEntity target;

    }



}
