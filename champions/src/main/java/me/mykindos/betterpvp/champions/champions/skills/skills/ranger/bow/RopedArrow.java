package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RopedArrow extends PrepareArrowSkill {
    private HashMap<UUID, Boolean> canTakeFall = new HashMap<>();
    public double fallDamageLimit;


    @Inject
    public RopedArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Roped Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will pull you",
                "towards the location it hits",
                "",
                "Cooldown: <val>" + getCooldown(level) + "</val>"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;

        Vector vec = UtilVelocity.getTrajectory(player, arrow);

        VelocityData velocityData = new VelocityData(vec, 1.8D, false, 0.8D, 0.3D, 1.5D, true);
        UtilVelocity.velocity(player, null, velocityData);

        arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        arrows.remove(arrow);
        canTakeFall.put(player.getUniqueId(), true);
    }

    @EventHandler
    public void reduceFallDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (event.getDamagee() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            if (canTakeFall.containsKey(playerId) && canTakeFall.get(playerId)) {
                if (event.getDamage() <= fallDamageLimit) {
                    event.setCancelled(true);
                } else {
                    event.setDamage(event.getDamage() - fallDamageLimit);
                }
                canTakeFall.remove(playerId);
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<UUID, Boolean>> fallIterator = canTakeFall.entrySet().iterator();
        while (fallIterator.hasNext()) {
            Map.Entry<UUID, Boolean> entry = fallIterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && (UtilBlock.isGrounded(player) || player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())) {
                UtilServer.runTaskLater(champions, () -> {
                    if (canTakeFall.containsKey(player.getUniqueId())) {
                        canTakeFall.remove(player.getUniqueId());
                    }
                }, 2L);
            }
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // No implementation - ignore
    }

    @Override
    public void displayTrail(Location location) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1);
        new ParticleBuilder(Particle.REDSTONE)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .data(dustOptions)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return (double) cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public void loadSkillConfig(){
        fallDamageLimit = getConfig("fallDamageLimit", 8.0, Double.class);
    }

}
