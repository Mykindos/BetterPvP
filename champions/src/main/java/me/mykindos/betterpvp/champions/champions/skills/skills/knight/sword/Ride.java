package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.*;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Ride extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, HorseData> horseData = new WeakHashMap<>();


    private double lifespan;
    private double horseHealth;
    @Inject
    public Ride(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Ride";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Mount a valiant steed which will ",
                "last for <val>" + (lifespan + (level-1)) + "</val> seconds",
                "",
                "If the horse takes any damage or you",
                "dismount, the horse will disappear",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_ANGRY, 2.0f, 0.5f);

        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setColor(Horse.Color.WHITE);
        horse.setStyle(Horse.Style.NONE);
        horse.setJumpStrength(1.5D);
        horse.getInventory().setArmor(new ItemStack(Material.LEATHER_HORSE_ARMOR));
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        AttributeInstance horseSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (horseSpeed != null) {
            horseSpeed.setBaseValue(0.35D);
        }
        horse.addPassenger(player);
        HorseData data = new HorseData(horse, System.currentTimeMillis());
        horseData.put(player, data);
    }

    public void removeHorses() {
        Iterator<Map.Entry<Player, HorseData>> iterator = horseData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, HorseData> data = iterator.next();
            Horse horse = data.getValue().getHorse();

            if (horse == null || horse.isDead() || UtilTime.elapsed(data.getValue().getSpawnTime(), (long) lifespan * 1000)) {
                horse.remove();
                iterator.remove();
            }
        }
    }

    private static class HorseData {
        private final Horse horse;
        private final long spawnTime;

        public HorseData(Horse horse, long spawnTime) {
            this.horse = horse;
            this.spawnTime = spawnTime;
        }

        public Horse getHorse() {
            return horse;
        }

        public long getSpawnTime() {
            return spawnTime;
        }
    }

    @Override
    public boolean canUse(Player player) {
        HorseData data = horseData.get(player);
        if (data != null) {
            Horse horse = data.getHorse();
            if (!horse.isDead()) {
                UtilMessage.message(player, getClassType().getName(), "You have already summoned a horse.");
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onHorseDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamager() instanceof Horse damagee)) return;
        HorseData data = horseData.get(damager);
        if (data != null && data.getHorse().equals(damagee)) {
            // If the horse belongs to the player who is damaging it, we ignore the event.
            return;
        }

        if (damagee.getOwner() instanceof Player owner) {
            if (owner.getUniqueId().equals(damager.getUniqueId())) {
                // If the horse's owner is the player, we ignore the event.
                return;
            }

            damagee.remove();
            horseData.remove(owner);
            UtilMessage.message(owner, getClassType().getName(), "Your horse has been killed.");
        }

    }

    @EventHandler
    public void onPlayerDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && event.getDismounted() instanceof Horse horse) {
            HorseData data = horseData.get(player);
            if (data != null && data.getHorse().equals(horse)) {
                UtilMessage.message(player, getClassType().getName(), "Your horse has disappeared.");
                horse.remove();
                horseData.remove(player);
            }
        }
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return (cooldown - level);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        lifespan = getConfig("lifespan", 2.0, Double.class);
    }

}
