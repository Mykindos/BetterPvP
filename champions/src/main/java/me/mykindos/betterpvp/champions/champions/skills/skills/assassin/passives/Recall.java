package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class Recall extends Skill implements ToggleSkill, CooldownSkill, Listener {

    public WeakHashMap<Player, RecallData> data = new WeakHashMap<>();
    public double percentHealthRecovered;
    public double currHealth;
    public double markerTiming;

    public double duration;
    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Recall";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Teleports you back in time <val>" + (duration + (level)) + "</val> seconds, increasing",
                "your health by <stat>" + (percentHealthRecovered * 100) + "%</stat> of the health you had",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @EventHandler
    public void onRoleChange(RoleChangeEvent e) {
        data.remove(e.getPlayer());
    }


    @UpdateEvent(delay = 500)
    public void updateRecallData() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            int level = getLevel(onlinePlayer);
            if (level > 0) {
                RecallData recallData = data.computeIfAbsent(onlinePlayer, k -> new RecallData());

                if (UtilTime.elapsed(recallData.getTime(), (long) (markerTiming * 1000))) {
                    recallData.addLocation(onlinePlayer.getLocation(), onlinePlayer.getHealth(), (int) ((2 + level) * (1 / markerTiming)));
                    recallData.addLocationMarker(onlinePlayer.getLocation());
                    recallData.setTime(System.currentTimeMillis());
                }

                Iterator<RecallData.LocationMarker> iterator = recallData.getLocationMarkers().iterator();
                while (iterator.hasNext()) {
                    RecallData.LocationMarker marker = iterator.next();
                    if (UtilTime.elapsed(marker.getTimestamp(), (long) ((duration + level) * 1000))) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSkillDequip(SkillDequipEvent event) {
        if (event.getSkill().equals(this)) {
            data.remove(event.getPlayer());
        }
    }

    @EventHandler
    public void onEquip(SkillEquipEvent e) {
        if (e.getSkill() == this) {
            if (data.containsKey(e.getPlayer())) {
                data.get(e.getPlayer()).locations.clear();
            }
        }
    }


    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }

    @Override
    public boolean canUse(Player player) {
        RecallData recallData = data.get(player);
        if (recallData != null) {
            if(recallData.locations.size() > 0) {
                if (!player.getWorld().getName().equalsIgnoreCase(recallData.getLocation().getWorld().getName())) {
                    UtilMessage.message(player, getClassType().getName(), "You can not recall into a different world");
                    return false;
                }
            }else{
                UtilMessage.message(player, getClassType().getName(), "You have nowhere to recall to.");
            }
        }

        return true;
    }


    @Override
    public void toggle(Player player, int level) {
        RecallData recallData = data.get(player);
        List<RecallData.LocationMarker> locationMarkers = recallData.getLocationMarkers();

        Location originalLocation = player.getLocation().add(0, 1.0, 0);
        Location recallLocation = recallData.getLocation().add(0, 0, 0);
        int maxMarkers = (int) ((2 + (level - 1)) / markerTiming);
        while (locationMarkers.size() > maxMarkers) {
            locationMarkers.remove(locationMarkers.size() - 1);
        }
        player.teleport(recallLocation);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);
        UtilEntity.setHealth(player, currHealth + (recallData.getHealth() * percentHealthRecovered));
        UtilServer.callEvent(new EffectClearEvent(player));

        //sequentially go through all the markers, drawing lines between them
        Location previousLocation = recallLocation;
        for (RecallData.LocationMarker marker : locationMarkers) {
            Location markerLocation = marker.getLocation().add(0, 1.0, 0);
            drawParticleTrail(previousLocation, markerLocation, player);
            previousLocation = markerLocation;
        }

        //draw a line from the final marker back to the original player location
        if (!locationMarkers.isEmpty()) {
            Location lastMarkerLocation = locationMarkers.get(locationMarkers.size() - 1).getLocation();
            drawParticleTrail(lastMarkerLocation, originalLocation, player);
        }
    }

    private void drawParticleTrail(Location from, Location to, Player player) {
        World world = from.getWorld();
        double distance = from.distance(to);
        Vector vector = to.toVector().subtract(from.toVector()).normalize().multiply(0.1);
        Location location = from.clone();
        Random random = new Random();

        for (double length = 0; length < distance; length += 0.1) {
            double yOffset = Math.sin(length) * 0.3;

            double randomX = (random.nextDouble() - 0.5) * 0.1;
            double randomZ = (random.nextDouble() - 0.5) * 0.1;

            world.spawnParticle(Particle.SPELL_WITCH, location.clone().add(randomX, yOffset, randomZ), 1);
            location.add(vector);
        }
    }

    @Override
    public void loadSkillConfig(){
        markerTiming = getConfig("markerTiming", 0.25, Double.class);
        percentHealthRecovered = getConfig("percentHealthRecovered", 0.5, Double.class);
        duration = getConfig("duration", 1.5, Double.class);
    }
}
