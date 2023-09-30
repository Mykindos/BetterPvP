package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Ride extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, HorseData> horseData = new WeakHashMap<>();
    private final Collection<Horse> activeHorses = new HashSet<>();

    private double lifespan;
    private double health;
    
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
                "If the horse takes damage or you dismount, it will disappear",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public void activate(Player player, int level) {

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 2.0f, 1f);

        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setColor(Horse.Color.WHITE);
        horse.setStyle(Horse.Style.NONE);
        AttributeInstance horseMaxHealth = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if(horseMaxHealth != null) {
            horseMaxHealth.setBaseValue(1);
        }
        horse.setHealth(health);
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
        activeHorses.add(horse);


        new BukkitRunnable() {
            @Override
            public void run() {
                if (!horse.isDead()) {
                    horse.remove();
                    horseData.remove(player);
                    activeHorses.remove(horse);

                }
            }
        }.runTaskLater(champions, (long) (lifespan + (level - 1)) * 20L);
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

    @EventHandler
    public void onCustomHorseDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Horse && activeHorses.contains(event.getDamagee())) {
            if (!(event.getDamager() instanceof Player)) {
                event.setCancelled(true);
            }
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
    public void onPlayerDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && event.getDismounted() instanceof Horse horse) {
            HorseData data = horseData.get(player);
            if (data != null && data.getHorse().equals(horse)) {
                UtilMessage.message(player, getClassType().getName(), "Your horse has disappeared.");
                horse.remove();
                horseData.remove(player);
                activeHorses.remove(horse);
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
        health = getConfig("health", 1.0, Double.class);
    }

}
