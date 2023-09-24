package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.spigotmc.event.entity.EntityDismountEvent;

@Singleton
@BPvPListener
public class Ride extends Skill implements InteractSkill, CooldownSkill, Listener {

    private WeakHashMap<Player, HorseData> horseData = new WeakHashMap<>();

    private final Set<UUID> active = new HashSet<>();

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
                "Mount a valliant steed which will ",
                "last for <val>" + (lifespan + (level-1)) + "</val> seconds",
                "",
                "The horse will have <val>" + (horseHealth + ((level-1) *5)) + "</val> health",
                "",
                "If you dismount the horse it will dissapear",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public void activate(Player player, int level) {
        if (!canUse(player)) {
            UtilMessage.message(player, getClassType().getName(), "Cannot summon horse now");
            return;
        }
        active.add(player.getUniqueId());

        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setColor(Horse.Color.WHITE);
        horse.setStyle(Horse.Style.NONE);
        horse.setMaxHealth(horseHealth + ((level - 1) * 5));
        horse.setHealth(horseHealth + ((level - 1) * 5));
        horse.setJumpStrength(1.5D);
        horse.getInventory().setArmor(new ItemStack(Material.LEATHER_HORSE_ARMOR));
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        AttributeInstance horseSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (horseSpeed != null) {
            horseSpeed.setBaseValue(0.35D);
        }
        horse.setPassenger(player);
        HorseData data = new HorseData(horse, System.currentTimeMillis());
        horseData.put(player, data);


        new BukkitRunnable() {
            @Override
            public void run() {
                if (horse != null && !horse.isDead()) {
                    horse.remove();
                    horseData.remove(player);
                    UtilMessage.message(player, getClassType().getName(), "Your horse has evaporated");
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

    public boolean canUse(Player player) {
        HorseData data = horseData.get(player);
        if (data != null) {
            Horse horse = data.getHorse();
            if (!horse.isDead()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player && event.getDismounted() instanceof Horse) {
            Player player = (Player) event.getEntity();
            Horse horse = (Horse) event.getDismounted();

            HorseData data = horseData.get(player);
            if (data != null && data.getHorse().equals(horse)) {
                UtilMessage.message(player, getClassType().getName(), "Your horse has evaporated");
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
        lifespan = getConfig("lifespan", 6.0, Double.class);
        horseHealth = getConfig("horseHealth", 5.0, Double.class);
    }

}