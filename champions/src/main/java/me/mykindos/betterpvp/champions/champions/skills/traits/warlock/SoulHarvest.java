package me.mykindos.betterpvp.champions.champions.skills.traits.warlock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class SoulHarvest extends Trait implements PassiveSkill, BuffSkill, HealthSkill {

    private final List<SoulData> souls = new ArrayList<>();

    private double soulDurationSeconds;
    private double buffDuration;
    private int speedStrength;
    private double healthPerSecond;

    @Inject
    public SoulHarvest(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Soul Harvest";
    }

    @Override
    public Component[] getDescription(int level) {
        Component speedRoman = Component.text(UtilFormat.getRomanNumeral(speedStrength), NamedTextColor.YELLOW);
        return traitDescription(speedRoman, traitValue(healthPerSecond, 1), traitValue(buffDuration, 1));
    }

    @Override
    public @NotNull Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public Material getIcon() {
        return Material.SOUL_LANTERN;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (UtilEntity.isPlayerSpawned(event.getEntity())) return;
        souls.add(new SoulData(event.getEntity().getUniqueId(), event.getEntity().getLocation(),
                System.currentTimeMillis() + (long) (soulDurationSeconds * 1000L)));
    }

    @UpdateEvent(delay = 250)
    public void displaySouls() {
        List<Player> active = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getLevel(player) <= 0) continue;
            active.add(player);

            if (player.isDead() || player.getHealth() <= 0) continue;

            List<SoulData> remove = new ArrayList<>();
            souls.forEach(soul -> {
                if (soul.getLocation().getWorld().getName().equals(player.getWorld().getName()) && !soul.getUuid().equals(player.getUniqueId())) {
                    if (soul.getLocation().distance(player.getLocation()) <= 1.5) {
                        giveEffect(player);
                        remove.add(soul);
                    }
                }
            });

            souls.removeIf(remove::contains);
        }

        souls.removeIf(soul -> soul.getExpiry() - System.currentTimeMillis() <= 0 || !soul.getLocation().isWorldLoaded());
        souls.forEach(soul -> {
            List<Player> newActives = new ArrayList<>(active);
            newActives.removeIf(p -> p.getUniqueId().equals(soul.getUuid()));
            Particle.HEART.builder().location(soul.getLocation().clone().add(0, 1, 0)).receivers(newActives).extra(0).spawn();
        });
    }

    @Override
    protected void loadTraitConfig() {
        soulDurationSeconds = getConfig("soulDurationSeconds", 120.0, Double.class);
        buffDuration = getConfig("buffDuration", 2.0, Double.class);
        speedStrength = getConfig("speedStrength", 2, Integer.class);
        healthPerSecond = getConfig("healthPerSecond", 4.0, Double.class);
    }

    private void giveEffect(Player player) {
        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, speedStrength, (long) (buffDuration * 1000));
        UtilPlayer.slowHealth(champions, player, healthPerSecond * buffDuration, (int) (buffDuration * 20), false);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, 2.0F, 2.0F);

        Location center = player.getLocation().add(0, 2, 0);
        double radius = 1;
        int points = 20;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            player.getWorld().spawnParticle(Particle.SOUL, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }

    @Data
    private static class SoulData {

        private final UUID uuid;
        private final Location location;
        private final long expiry;
    }
}
