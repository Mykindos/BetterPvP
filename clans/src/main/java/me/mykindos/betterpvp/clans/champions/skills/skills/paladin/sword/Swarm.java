package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Swarm extends ChannelSkill implements InteractSkill, EnergySkill, Listener {

    private final WeakHashMap<Player, Long> batCD = new WeakHashMap<>();
    private final WeakHashMap<Player, ArrayList<BatData>> batData = new WeakHashMap<>();

    @Inject
    public Swarm(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Swarm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold Block with a sword to Channel",
                "",
                "Release a swarm of bats",
                "which damage, and knockback",
                "any enemies they come in contact with",
                "",
                "Energy: " + ChatColor.GREEN + getEnergy(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public float getEnergy(int level) {

        return getSkillConfig().getEnergyCost() - ((level - 1));
    }


    public boolean hitPlayer(Location loc, LivingEntity player) {
        if (loc.add(0, -loc.getY(), 0).toVector().subtract(player.getLocation()
                .add(0, -player.getLocation().getY(), 0).toVector()).length() < 0.8D) {
            return true;
        }
        if (loc.add(0, -loc.getY(), 0).toVector().subtract(player.getLocation()
                .add(0, -player.getLocation().getY(), 0).toVector()).length() < 1.2) {
            return (loc.getY() > player.getLocation().getY()) && (loc.getY() < player.getEyeLocation().getY());
        }
        return false;
    }


    @UpdateEvent
    public void checkChannelling() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            if (!active.contains(cur.getUniqueId())) continue;

            if (cur.isHandRaised()) {
                int level = getLevel(cur);
                if (level <= 0) {
                    active.remove(cur.getUniqueId());
                } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 2, true)) {
                    active.remove(cur.getUniqueId());
                } else if (!UtilPlayer.isHoldingItem(cur, SkillWeapons.SWORDS)) {
                    active.remove(cur.getUniqueId());
                } else {
                    if (batData.containsKey(cur)) {

                        Bat bat = cur.getWorld().spawn(cur.getLocation().add(0, 0.5, 0), Bat.class);
                        bat.setHealth(1);
                        bat.setVelocity(cur.getLocation().getDirection().multiply(2));
                        batData.get(cur).add(new BatData(bat, System.currentTimeMillis(), cur.getLocation()));

                    }
                }
            } else {
                active.remove(cur.getUniqueId());
            }
        }

    }

    @UpdateEvent(delay = 100)
    public void batHit() {
        for (Player player : batData.keySet()) {
            for (BatData batData : batData.get(player)) {
                Bat bat = batData.getBat();
                Vector rand = new Vector((Math.random() - 0.5D) / 3.0D, (Math.random() - 0.5D) / 3.0D, (Math.random() - 0.5D) / 3.0D);
                bat.setVelocity(batData.getLoc().getDirection().clone().multiply(0.5D).add(rand));

                for (LivingEntity other : UtilEntity.getNearbyEntities(player, bat.getLocation(), 3)) {
                    if (other instanceof Bat) continue;
                    if (other instanceof ArmorStand) continue;
                    if (!hitPlayer(bat.getLocation(), other)) continue;

                    if (other instanceof Player) {
                        if (batCD.containsKey(other)) {
                            if (!UtilTime.elapsed(batCD.get(other), 500)) continue;
                        }
                        batCD.put((Player) other, System.currentTimeMillis());
                        championsManager.getEffects().addEffect((Player) other, EffectType.SHOCK, 800L);
                    }


                    Vector vector = bat.getLocation().getDirection();
                    vector.normalize();
                    vector.multiply(.4d);
                    vector.setY(vector.getY() + 0.2d);

                    if (vector.getY() > 7.5)
                        vector.setY(7.5);

                    if (other.isOnGround())
                        vector.setY(vector.getY() + 0.4d);

                    other.setFallDistance(0);
                    UtilDamage.doCustomDamage(new CustomDamageEvent(other, player, null, DamageCause.CUSTOM, 1, false, getName()));

                    other.setVelocity(bat.getLocation().getDirection().add(new Vector(0, .4F, 0)).multiply(0.50));


                    bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_HURT, 0.1F, 0.7F);

                    bat.remove();

                }
            }
        }
    }


    @UpdateEvent(delay = 500)
    public void destroyBats() {

        Iterator<Entry<Player, ArrayList<BatData>>> iterator = batData.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Player, ArrayList<BatData>> data = iterator.next();
            ListIterator<BatData> batIt = data.getValue().listIterator();
            while (batIt.hasNext()) {
                BatData bat = batIt.next();

                if (bat.getBat() == null || bat.getBat().isDead()) {
                    batIt.remove();
                    continue;
                }

                if (UtilTime.elapsed(bat.getTimer(), 2000)) {
                    bat.getBat().remove();
                    batIt.remove();

                }
            }

            if (data.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        if (!batData.containsKey(player)) {
            batData.put(player, new ArrayList<>());
        }
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Data
    private static class BatData {

        private final Bat bat;
        private final long timer;
        private final Location loc;

    }

}
