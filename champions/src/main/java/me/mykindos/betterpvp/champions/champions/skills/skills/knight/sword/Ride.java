package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Ride extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill {

    private final WeakHashMap<Player, HorseData> horseData = new WeakHashMap<>();
    private double lifespan;

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
                "last for <val>" + (lifespan + (level - 1)) + "</val> seconds",
                "",
                "If the horse takes any damage or you",
                "dismount, it will disappear",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public void activate(Player player, int level) {

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 2.0f, 1f);

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
        long calculatedLifespan = (long) (lifespan + (level - 1)) * 1000;
        HorseData data = new HorseData(horse, System.currentTimeMillis(), calculatedLifespan);
        horseData.put(player, data);
    }

    @UpdateEvent(delay = 500)
    public void removeHorses() {
        Set<Player> toRemove = new HashSet<>();

        for (Map.Entry<Player, HorseData> data : horseData.entrySet()) {
            Player player = data.getKey();
            Horse horse = data.getValue().getHorse();

            if (horse == null) {
                toRemove.add(player);
            } else if (horse.isDead() || UtilTime.elapsed(data.getValue().getSpawnTime(), data.getValue().getLifespan())) {
                horse.remove();
                toRemove.add(player);
            }
        }

        for (Player player : toRemove) {
            horseData.remove(player);
        }
    }

    private static class HorseData {

        @Getter
        private final long lifespan;

        @Getter
        private final Horse horse;

        @Getter
        private final long spawnTime;

        private boolean wasKilled = false;  // Add this field

        public HorseData(Horse horse, long spawnTime, long lifespan) {
            this.horse = horse;
            this.spawnTime = spawnTime;
            this.lifespan = lifespan; // Store lifespan
        }


        public boolean wasKilled() {
            return wasKilled;
        }

        public void setWasKilled(boolean wasKilled) {
            this.wasKilled = wasKilled;
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

        if (!(event.getDamagee() instanceof Horse damagee)) return;

        if (!(damagee.getOwner() instanceof Player owner)) return;

        if (damager.equals(owner)) {
            event.setCancelled(true);
            return;
        }

        HorseData data = horseData.get(owner);
        data.setWasKilled(true);
        damagee.remove();
        horseData.remove(owner);
        UtilMessage.message(owner, getClassType().getName(), "Your horse has been killed.");
    }

    @EventHandler
    public void onPlayerDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && event.getDismounted() instanceof Horse horse) {
            HorseData data = horseData.get(player);
            if (data != null && data.getHorse().equals(horse)) {
                if (!data.wasKilled()) {
                    UtilMessage.message(player, getClassType().getName(), "Your horse has disappeared.");
                }
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