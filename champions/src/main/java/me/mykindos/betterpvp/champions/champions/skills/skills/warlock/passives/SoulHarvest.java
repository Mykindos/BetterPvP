package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class SoulHarvest extends Skill implements PassiveSkill, BuffSkill {

    private final List<SoulData> souls = new ArrayList<>();

    private double baseBuffDuration;

    private double buffDurationIncreasePerLevel;

    private int speedStrength;

    private int regenerationStrength;

    @Inject
    public SoulHarvest(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Soul Harvest";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "When enemies die, they will drop a soul",
                "which is only visible to " + getClassType().getName(),
                "",
                "Collected souls give bursts of",
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> and <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength) + "</effect>",
                "",
                "Buff duration: " + getValueString(this::getBuffDuration, level) + " seconds",
        };
    }

    private double getBuffDuration(int level) {
        return baseBuffDuration + ((level - 1) * buffDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntity().hasMetadata("PlayerSpawned")) return;
        souls.add(new SoulData(event.getEntity().getUniqueId(), event.getEntity().getLocation(), System.currentTimeMillis() + 120_000));
    }

    @UpdateEvent(delay = 250)
    public void displaySouls() {
        List<Player> active = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                active.add(player);

                if (!player.isDead() && player.getHealth() > 0) {
                    List<SoulData> remove = new ArrayList<>();
                    souls.forEach(soul -> {
                        if (soul.getLocation().getWorld().getName().equals(player.getWorld().getName()) && !soul.getUuid().equals(player.getUniqueId())) {
                            if (soul.getLocation().distance(player.getLocation()) <= 1.5 && !soul.getUuid().toString().equals(player.getUniqueId().toString())) {
                                giveEffect(player, level);
                                remove.add(soul);
                            }
                        }
                    });

                    souls.removeIf(remove::contains);
                }
            }
        }

        souls.removeIf(soul -> soul.getExpiry() - System.currentTimeMillis() <= 0);
        souls.forEach(soul -> {
            List<Player> newActives = new ArrayList<>(active);
            newActives.removeIf(p -> p.getUniqueId().equals(soul.getUuid()));
            Particle.HEART.builder().location(soul.getLocation().clone().add(0, 1, 0)).receivers(newActives).extra(0).spawn();
        });

    }

    @Override
    public void loadSkillConfig() {
        baseBuffDuration = getConfig("baseBuffDuration", 2.0, Double.class);
        buffDurationIncreasePerLevel = getConfig("buffDurationIncreasePerLevel", 1.0, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
        regenerationStrength = getConfig("regenerationStrength", 2, Integer.class);
    }

    private void giveEffect(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, speedStrength, (long) (getBuffDuration(level) * 1000));
        championsManager.getEffects().addEffect(player, EffectTypes.REGENERATION, regenerationStrength, (long) (getBuffDuration(level) * 1000));
    }

    @Data
    private static class SoulData {

        private final UUID uuid;
        private final Location location;
        private final long expiry;
    }
}
