package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Swarm extends ChannelSkill implements InteractSkill, EnergyChannelSkill, Listener {

    private final WeakHashMap<Player, Long> batCD = new WeakHashMap<>();
    private final WeakHashMap<Player, ArrayList<BatData>> batData = new WeakHashMap<>();

    private double batLifespan;

    private double batDamage;

    @Inject
    public Swarm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Swarm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Release a swarm of bats which",
                "damage and knock back any enemies",
                "they come in contact with",
                "",
                "Energy: <val>" + getEnergy(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
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

        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player cur = Bukkit.getPlayer(iterator.next());
            if (cur == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
            if (!gamer.isHoldingRightClick()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(cur);
            if (level <= 0) {
                iterator.remove();
            } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 2, true)) {
                iterator.remove();
            } else if (!isHolding(cur)) {
                iterator.remove();
            } else {
                if (batData.containsKey(cur)) {

                    Bat bat = cur.getWorld().spawn(cur.getLocation().add(0, 0.5, 0), Bat.class);
                    bat.setHealth(1);
                    bat.setMetadata("PlayerSpawned", new FixedMetadataValue(champions, true));
                    bat.setVelocity(cur.getLocation().getDirection().multiply(2));
                    batData.get(cur).add(new BatData(bat, System.currentTimeMillis(), cur.getLocation()));

                }
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

                for (var data : UtilEntity.getNearbyEntities(player, bat.getLocation(), 3, EntityProperty.ENEMY)) {
                    LivingEntity other = data.get();

                    if (other instanceof Bat) continue;
                    if (!hitPlayer(bat.getLocation(), other)) continue;

                    if (other instanceof Player) {
                        if (batCD.containsKey(other)) {
                            if (!UtilTime.elapsed(batCD.get(other), 500)) continue;
                        }
                        batCD.put((Player) other, System.currentTimeMillis());
                        championsManager.getEffects().addEffect(other, EffectTypes.SHOCK, 800L);
                    }

                    final CustomDamageEvent event = new CustomDamageEvent(other,
                            player,
                            null,
                            DamageCause.CUSTOM,
                            batDamage,
                            false,
                            getName());
                    UtilDamage.doCustomDamage(event);

                    if (!event.isCancelled()) {
                        Vector vector = bat.getLocation().getDirection();
                        final VelocityData velocityData = new VelocityData(vector, 0.4d, 0.2d, 7.5d, true);
                        UtilVelocity.velocity(other, player, velocityData);

                        bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_HURT, 0.1F, 0.7F);
                    }

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

                if (UtilTime.elapsed(bat.getTimer(), (long) batLifespan * 1000)) {
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
    public void loadSkillConfig() {
        batLifespan = getConfig("batLifespan", 2.0, Double.class);
        batDamage = getConfig("batDamage", 1.0, Double.class);
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
