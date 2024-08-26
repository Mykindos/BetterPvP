package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

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
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;

import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import net.minecraft.world.item.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
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
    public double velocityResetTime;
    public int storedVelocityCount;
    public int storedVelocityCountIncreasePerLevel;
    private double fallDamageLimit;
    private final TaskScheduler taskScheduler;

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
    public Kinetics(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Kinetics";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your arrows no longer deal knockback, and instead",
                "the velocity is stored for up to " + getValueString(this::getDamageResetTime, level) + " seconds",
                "",
                "Double Jump to activate this stored velocity on yourself",
                "",
                "Can store up to " + getValueString(this::getStoredVelocityCount, level) + " levels of velocity"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    public double getDamageResetTime(int level) {
        return velocityResetTime;
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

            if (!jumped && data.get(player) > 0) {
                player.setAllowFlight(true);
            }

            if (UtilBlock.isGrounded(player)) {
                hasJumped.put(player, false);
            }

            player.setFlyingFallDamage(TriState.TRUE);

            // Check if player is in the air
            if (!UtilBlock.isGrounded(player) && hasJumped.get(player) && !UtilBlock.isInLiquid(player)) {
                // Use NMS to set riptide animation
                ItemStack trident = new ItemStack(Items.TRIDENT);
                ServerPlayer entity = ((CraftPlayer) player).getHandle();
                entity.startAutoSpinAttack(1, 0, trident);
            }

            Long lastTimeHit = arrowHitTime.get(playerUUID);
            if (lastTimeHit == null || (currentTime - lastTimeHit > getDamageResetTime(getLevel(player)) * 1000)) {
                int currentCharges = entry.getValue();
                if (currentCharges > 1) {
                    data.put(player, currentCharges - 1);
                    arrowHitTime.put(playerUUID, currentTime);
                } else {
                    hasJumped.put(player, false);
                    iterator.remove();
                    arrowHitTime.remove(playerUUID);
                    Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                    gamer.getActionBar().remove(actionBarComponent);
                    player.setAllowFlight(false);
                }
            }
        }
    }

    @EventHandler
    public void endOnInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        int level = getLevel(player);

        if (hasJumped.containsKey(player) && data.containsKey(player) && level > 0){
            if (hasJumped.get(player)) {
                hasJumped.put(player, false);
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
        VelocityData velocityData = new VelocityData(vec, 0.8 + (0.35 * multiplier), false, 0.0D, 0.25, 0.25 + (0.1D * multiplier), false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        data.put(player, 0);
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBarComponent);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 2.0f, 1.0f);

        player.setFlyingFallDamage(TriState.TRUE);
        hasJumped.put(player, true);

        taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
            Player target = Bukkit.getPlayer(uuid);
            if(target != null) {
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                        250L, true, true, UtilBlock::isGrounded);
            }
        }, 1000));
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        velocityResetTime = getConfig("damageResetTime", 4.0, Double.class);
        storedVelocityCount = getConfig("storedVelocityCount", 1, Integer.class);
        storedVelocityCountIncreasePerLevel = getConfig("storedVelocityCountIncreasePerLevel", 1, Integer.class);
        fallDamageLimit = getConfig("storedVelocityCountIncreasePerLevel", 4.0, Double.class);
    }
}
