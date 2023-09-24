package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.WeakHashMap;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

@Singleton
@BPvPListener
public class Ride extends PrepareSkill implements CooldownSkill, Listener {

    private WeakHashMap<Player, HorseData> horseData = new WeakHashMap<>();
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
                "Summon a rideable armored horse that ",
                "last for <val>" + (lifespan + ((level-1) * 2)) + "</val> seconds",
                "",
                "The horse will have <val>" + (horseHealth + ((level-1) *5)),
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public void activate(Player player, int level) {
        active.add(player.getUniqueId());

        Horse horse = player.getWorld().spawn(player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2)), Horse.class);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setVariant(Horse.Variant.HORSE);
        horse.setColor(Horse.Color.BROWN);
        horse.setStyle(Horse.Style.NONE);
        horse.setHealth(horseHealth + ((level - 1) * 5));
        horse.setMaxHealth(horseHealth + ((level - 1) * 5));
        horse.setJumpStrength(0.7D);
        horse.getInventory().setArmor(new ItemStack(Material.DIAMOND_HORSE_ARMOR));
        AttributeInstance horseSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (horseSpeed != null) {
            horseSpeed.setBaseValue(0.25D);
        }

        HorseData data = new HorseData(horse, System.currentTimeMillis());
        horseData.put(player, data);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (horse != null && !horse.isDead()) {
                    horse.remove();
                    horseData.remove(player);
                }
            }
        }.runTaskLater(champions, (long) (lifespan + ((level - 1) * 2)) * 20L);
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
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 1.5);
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