package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;


import java.util.*;


@Singleton
@BPvPListener
public class LifeBonds extends ActiveToggleSkill implements EnergySkill {

    private double minRadius;
    private double duration;


    @Inject
    public LifeBonds(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Life Bonds";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Connect you and all your allies through",
                "the power of nature, spreading your health",
                "between all allies within <val>" + (minRadius + level) + "</val> blocks",
                "",
                "While life bonds is active you take double damage",
                "",
                "Energy / Second: <val>" + getEnergy(level)

        };
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            sendState(player, false);
        } else {
            if (championsManager.getEnergy().use(player, getName(), 10, false)) {
                active.add(player.getUniqueId());
                sendState(player, true);
            }

        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (active.contains(player.getUniqueId())) {
                event.setDamage(event.getDamage() * 2); // Double the damage
            }
        }
    }

    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player cur = Bukkit.getPlayer(uuid);
            if (cur != null) {
                cur.getWorld().playSound(cur.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3F, 0.0F);
            }
        }
    }

    @UpdateEvent(delay = 125)
    public void grassAura() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                int level = getLevel(player);
                if (level <= 0 || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 2, true) || championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                    iterator.remove();
                } else {
                    double distance = minRadius + level;

                    HashMap<Block, Double> blocks = UtilBlock.getInRadius(player.getLocation(), distance);

                    shareHealth(player, distance);
                    spawnParticlesAboveAllies(player, distance);
                }
            } else {
                iterator.remove();
            }
        }
    }

    private void spawnParticlesAboveAllies(Player player, double distance) {
        List<Player> allies = getAllAllies(player, distance);
        for (Player ally : allies) {
            spawnParticleAboveHead(ally);
        }
    }

    private List<Player> getAllAllies(Player player, double distance) {
        List<Player> allies = UtilPlayer.getNearbyAllies(player, player.getLocation(), distance);
        allies.add(player);
        return allies;
    }


    private void spawnParticleAboveHead(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 2, 0.2, 0.2, 0.2, 0);
    }

    private void shareHealth(Player player, double distance) {
        List<Player> allies = new ArrayList<>();
        double totalHealth = player.getHealth();
        allies.add(player);

        for (var data : UtilPlayer.getNearbyPlayers(player, distance)) {
            Player target = data.getKey();
            boolean friendly = data.getValue() == EntityProperty.FRIENDLY;
            if (friendly && target.getHealth() > 0) {
                totalHealth += target.getHealth();
                allies.add(target);
            }
        }

        double sharedHealth = totalHealth / allies.size();
        for (Player ally : allies) {
            if (ally.getHealth() > 0) {
                ally.setHealth(sharedHealth);
            }
        }
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }


    @Override
    public float getEnergy(int level) {

        return (float) energy - ((level - 1));
    }

    private void sendState(Player player, boolean state) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Life Bonds: %s", state ? "<green>On" : "<red>Off");
    }

    @Override
    public void loadSkillConfig() {
        minRadius = getConfig("minRadius", 2.0, Double.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}