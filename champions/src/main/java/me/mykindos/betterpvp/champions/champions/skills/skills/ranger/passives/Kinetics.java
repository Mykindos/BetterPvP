package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;

@Singleton
@BPvPListener
public class Kinetics extends Skill implements PassiveSkill, MovementSkill {

    private final WeakHashMap<Player, Integer> data = new WeakHashMap<>();
    private final Map<UUID, Long> arrowHitTime = new HashMap<>();
    private final WeakHashMap<Player, Boolean> hasJumped = new WeakHashMap<>();
    public double damageResetTime;
    public int storedVelocityCount;
    public int storedVelocityCountIncreasePerLevel;

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        if (player == null || !data.containsKey(player)) {
            return null;
        }

        int level = getLevel(player);

        final int maxCharges = getStoredVelocityCount(level);
        final int newCharges = Math.min(data.get(player), getStoredVelocityCount(level));

        return Component.text("Kinetic Charge ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxCharges - newCharges))).color(NamedTextColor.RED));
    });

    @Inject
    public Kinetics(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Kinetics";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your arrows no longer deal knockback, and instead",
                "the knockback is stored for up to " + getValueString(this::getDamageResetTime, level) + " seconds",
                "",
                "Double Jump to activate this stored knockback on yourself",
                "",
                "Can store up to " + getValueString(this::getStoredVelocityCount, level) + " projectiles worth of knockback"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    public double getDamageResetTime(int level) {
        return damageResetTime;
    }

    public int getStoredVelocityCount(int level) {
        return storedVelocityCount + ((level - 1) * storedVelocityCountIncreasePerLevel);
    }

    private boolean isValidProjectile(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof Trident;
    }

    @EventHandler
    public void onProjectileHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!isValidProjectile(projectile)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(100, actionBarComponent);

        int level = getLevel(player);
        if (level > 0) {
            int charge = data.getOrDefault(player, 0);
            if (charge < getStoredVelocityCount(level)) {
                charge++;
            }
            data.put(player, charge);

            arrowHitTime.put(player.getUniqueId(), System.currentTimeMillis());
            event.setKnockback(false);
        }
    }

    @UpdateEvent
    public void updateKineticsData() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Player, Integer>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<Player, Integer> entry = iterator.next();
            Player player = entry.getKey();

            UUID playerUUID = player.getUniqueId();
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
            }

            boolean jumped = hasJumped.getOrDefault(player, false);
            hasJumped.putIfAbsent(player, false);

            if (!jumped) {
                player.setAllowFlight(true);
            }

            if (UtilBlock.isGrounded(player, 1)){
                hasJumped.put(player, false);
            }

            player.setFlyingFallDamage(TriState.TRUE);

            Long lastTimeHit = arrowHitTime.get(playerUUID);
            if (lastTimeHit == null || (currentTime - lastTimeHit > getDamageResetTime(getLevel(player)) * 1000)) {
                int currentCharges = entry.getValue();
                if (currentCharges > 1) {
                    data.put(player, currentCharges - 1);
                    arrowHitTime.put(playerUUID, currentTime);
                } else {
                    iterator.remove();
                    arrowHitTime.remove(playerUUID);
                    Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                    gamer.getActionBar().remove(actionBarComponent);
                }
            }
        }
    }


    @EventHandler
    public void doubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        Integer chargeCount = data.get(player);
        if (chargeCount == null || chargeCount <= 0) return;

        Vector vec = player.getLocation().getDirection();
        double multiplier = Math.min(chargeCount, getStoredVelocityCount(getLevel(player)));
        VelocityData velocityData = new VelocityData(vec, 0.5 + (0.3 * multiplier), false, 0.0D, (0.15D * multiplier), (0.2D * multiplier), false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        data.put(player, 0);
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBarComponent);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 2.0f, 1.0f);
        new ParticleBuilder(Particle.GUST_EMITTER_SMALL)
                .location(player.getLocation().add(0, 1, 0))
                .count(1)
                .offset(0.0, 0.0, 0.0)
                .extra(0)
                .receivers(60)
                .spawn();

        player.setFlyingFallDamage(TriState.TRUE);
        hasJumped.put(player, true);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public void loadSkillConfig() {
        damageResetTime = getConfig("damageResetTime", 3.0, Double.class);
        storedVelocityCount = getConfig("storedVelocityCount", 1, Integer.class);
        storedVelocityCountIncreasePerLevel = getConfig("storedVelocityCountIncreasePerLevel", 1, Integer.class);
    }
}
