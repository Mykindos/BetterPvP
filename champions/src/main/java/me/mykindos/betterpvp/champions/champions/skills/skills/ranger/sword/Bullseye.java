package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.BullsEyeData;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;


@Singleton
@BPvPListener
public class Bullseye extends ChannelSkill implements CooldownSkill, InteractSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<UUID, BullsEyeData> bullsEyeData = new WeakHashMap<>();

    @Getter
    private double curveDistance;
    @Getter
    private double bonusDamage;
    @Getter
    private double bonusCurveDistance;


    @Inject
    public Bullseye(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bulls Eye";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "While looking at an enemy you will gain charge",
                "on them, when you next shoot an arrow towards",
                "that enemy it will curve towards them from a",
                "distance of up to <val>" + getCurveDistance() + "</val> blocks and also deal",
                getBonusDamage() + " bonus damage",
                "",
                "Cooldown: <val>" + getCooldown()};
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !bullsEyeData.containsKey(Objects.requireNonNull(gamer.getPlayer()).getUniqueId()) && isHolding(gamer.getPlayer());
    }

    @Override
    public void activate(Player player) {
        UUID playerUUID = player.getUniqueId();
        active.add(playerUUID);
        BullsEyeData playerBullsEyeData = new BullsEyeData(player, new ChargeData((float) (0.01)), null, null, null);
        bullsEyeData.put(playerUUID, playerBullsEyeData);
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<UUID> iterator = bullsEyeData.keySet().iterator();
        while (iterator.hasNext()) {
            UUID playerUUID = iterator.next();
            if (playerUUID == null || Bukkit.getPlayer(playerUUID) == null || !Objects.requireNonNull(Bukkit.getPlayer(playerUUID)).isOnline()) {
                iterator.remove();
                continue;
            }
            Player player = Bukkit.getPlayer(playerUUID);
            BullsEyeData playerBullsEyeData = bullsEyeData.get(playerUUID);
            // Remove if they no longer have the skill
            if (!hasSkill(player)) {
                iterator.remove();
                continue;
            }
            // Spawn particles
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (playerBullsEyeData.getTargetFocused() != null && playerBullsEyeData.getTarget().isValid()) {
                if (player.getInventory().getItemInMainHand().getType().equals(Material.BOW) || (isHolding(player) && gamer.isHoldingRightClick())) {
                    playerBullsEyeData.spawnFocusingParticles();
                }
            }

            // Check if they still are blocking and charge
            if (isHolding(player) && gamer.isHoldingRightClick()) {
                playerBullsEyeData.getCasterCharge().tick();
                championsManager.getCooldowns().removeCooldown(player, getName(), true);
                focusTarget(playerBullsEyeData);
            }
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (bullsEyeData.get(player.getUniqueId()) == null) return;
            BullsEyeData playerBullsEyeData = bullsEyeData.get(player.getUniqueId());
            if (playerBullsEyeData.getTarget() == null || playerBullsEyeData.getTargetFocused() == null) return;
            Entity arrow = event.getProjectile();
            if (!(arrow instanceof Arrow)) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    Collection<LivingEntity> nearbyEntities = arrow.getLocation().getNearbyLivingEntities((getCurveDistance() * playerBullsEyeData.getTargetFocused().getCharge()) + getBonusCurveDistance());
                    if (!arrow.isValid() || playerBullsEyeData.getTarget() == null || !playerBullsEyeData.getTarget().isValid()) {
                        this.cancel();
                        return;
                    }
                    if (nearbyEntities.contains(playerBullsEyeData.getTarget())) {

                        Particle.DustOptions dustOptions = new Particle.DustOptions(playerBullsEyeData.getColor(), 1);
                        new ParticleBuilder(Particle.DUST)
                                .location(arrow.getLocation())
                                .count(1)
                                .offset(0.1, 0.1, 0.1)
                                .extra(0)
                                .receivers(60)
                                .data(dustOptions)
                                .spawn();

                        Vector direction = playerBullsEyeData.getTarget().getLocation().add(0, playerBullsEyeData.getTarget().getHeight() / 2, 0).toVector().subtract(arrow.getLocation().toVector()).normalize();
                        arrow.setVelocity(direction);
                    }
                }
            }.runTaskTimer(champions, 0, 2);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        LivingEntity damagee = event.getDamagee();
        if (bullsEyeData.get(damager.getUniqueId()) == null) return;
        if (damagee == bullsEyeData.get(damager.getUniqueId()).getTarget()) {
            bullsEyeData.keySet().removeIf(playerUUID -> damager == Bukkit.getPlayer(playerUUID));
            event.setDamage(getBonusDamage() + (event.getDamage()));
            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 2f, 1.2f);
            UtilMessage.simpleMessage(damagee, getName(), "<alt>" + damager.getName() + "</alt> hit you with <alt>" + getName());

            //apply cooldown
            championsManager.getCooldowns().removeCooldown(damager, getName(), true);
            championsManager.getCooldowns().use(damager,
                    getName(),
                    getCooldown(),
                    true,
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);
        }
    }

    private void focusTarget(BullsEyeData playerBullsEyeData) {
        Player caster = playerBullsEyeData.getCaster();
        RayTraceResult result = caster.rayTraceEntities(64);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            if (!playerBullsEyeData.hasTarget()) {
                playerBullsEyeData.setTarget(target);
                playerBullsEyeData.setTargetFocused(new ChargeData((float) (0.5)));
                bullsEyeData.put(caster.getUniqueId(), playerBullsEyeData);
                playerBullsEyeData.getTargetFocused().tick();
                playerBullsEyeData.getTargetFocused().tickSound(caster);
                playerBullsEyeData.updateColor();
            }
        }

        if (playerBullsEyeData.getTarget() == null || playerBullsEyeData.getTargetFocused() == null) return;

        int degrees = 10;

        Vector casterToEntity = playerBullsEyeData.getTarget().getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
        Vector playerDirection = caster.getLocation().getDirection().normalize();

        double dotProduct = playerDirection.dot(casterToEntity);
        double angle = Math.acos(dotProduct);

        // Convert radians to degrees
        double angleDegrees = Math.toDegrees(angle);

        // Check if angle is within the specified degrees
        if (angleDegrees <= degrees) {
            playerBullsEyeData.getTargetFocused().tick();
            playerBullsEyeData.getTargetFocused().tickSound(caster);
            playerBullsEyeData.updateColor();
        }
    }

    @Override
    public void loadSkillConfig() {
        bonusCurveDistance = getConfig("bonusCurveDistance", 2.5, Double.class);

        bonusDamage = getConfig("bonusDamage", 2.0, Double.class);

        curveDistance = getConfig("curveDistance", 0.5, Double.class);
    }
}
