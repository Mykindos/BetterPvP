package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.BullsEyeData;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
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

    private double baseCurveDistance;
    private double bonusCurveDistanceIncreasePerLevel;
    private double baseBonusDamage;
    private double baseBonusDamageIncreasePerLevel;
    private double bonusCurveDistance;
    private double decayRate;
    private double hitboxSize;
    private int chargeDistanceIncreasePerLevel;
    private int chargeDistance;

    @Inject
    public Bullseye(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bulls Eye";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Block on an enemy within " + getValueString(this::getChargeDistance, level) + " blocks to start",
                "charging, when you next shoot an arrow towards",
                "that enemy it will curve towards them from a",
                "distance of up to " + getValueString(this::getCurveDistance, level) + " blocks and also deal",
                getValueString(this::getBonusDamage, level) + " bonus damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)};
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

    public double getCurveDistance(int level) {
        return baseCurveDistance + ((level - 1) * bonusCurveDistanceIncreasePerLevel);
    }

    public double getBonusDamage(int level) {
        return baseBonusDamage + ((level - 1) * baseBonusDamageIncreasePerLevel);
    }

    public double getBonusCurveDistance(int level) {
        return bonusCurveDistance;
    }
    public int getChargeDistance(int level){
        return chargeDistance + ((level - 1) * chargeDistanceIncreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        UUID playerUUID = player.getUniqueId();
        active.add(playerUUID);

        LivingEntity potentialTarget = getPotentialTarget(player);

        if (bullsEyeData.containsKey(playerUUID)) {
            BullsEyeData existingData = bullsEyeData.get(playerUUID);
            // check if the player is looking at the same target
            if (existingData.getTarget() != null && existingData.getTarget().equals(potentialTarget)) {
                // same target, no need to create new data
                return;
            }
        }

        // create new BullsEyeData if no existing data or different target
        BullsEyeData playerBullsEyeData = new BullsEyeData(player, new ChargeData(0.5f), potentialTarget, new ChargeData(0.5f), null);
        bullsEyeData.put(playerUUID, playerBullsEyeData);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !bullsEyeData.containsKey(Objects.requireNonNull(gamer.getPlayer()).getUniqueId()) && isHolding(gamer.getPlayer());
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
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }
            // Spawn particles
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (playerBullsEyeData.getTargetFocused() != null && playerBullsEyeData.getTarget() != null && playerBullsEyeData.getTarget().isValid()) {
                if (player.getInventory().getItemInMainHand().getType().equals(Material.BOW) || (isHolding(player) && gamer.isHoldingRightClick())) {
                    playerBullsEyeData.spawnFocusingParticles();
                }
            }
            // Check if they still are blocking and charge
            if (isHolding(player) && gamer.isHoldingRightClick()) {
                playerBullsEyeData.setLastChargeTime(System.currentTimeMillis());
                championsManager.getCooldowns().removeCooldown(player, getName(), true);
                focusTarget(playerBullsEyeData);
            } else {
                long currentTime = System.currentTimeMillis();
                long lastChargeTime = playerBullsEyeData.getLastChargeTime();


                if (playerBullsEyeData.getCasterCharge().getCharge() >= 1.0f && currentTime - lastChargeTime <= 1000) {
                    playerBullsEyeData.spawnFocusingParticles();
                    continue;
                }

                playerBullsEyeData.decayCharge(decayRate);

                if (playerBullsEyeData.getCasterCharge().getCharge() <= 0) {
                    iterator.remove();
                }

                if (playerBullsEyeData.getTargetFocused() != null && playerBullsEyeData.getTarget() != null && playerBullsEyeData.getTarget().isValid() && playerBullsEyeData.getCasterCharge().getCharge() > 0) {
                    playerBullsEyeData.spawnFocusingParticles();
                }
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
                    Collection<LivingEntity> nearbyEntities = arrow.getLocation().getNearbyLivingEntities((getCurveDistance(getLevel(player)) * playerBullsEyeData.getTargetFocused().getCharge()) + getBonusCurveDistance(getLevel(player)));
                    if (!arrow.isValid() || playerBullsEyeData.getTarget() == null || !playerBullsEyeData.getTarget().isValid()) {
                        this.cancel();
                        return;
                    }
                    if (nearbyEntities.contains(playerBullsEyeData.getTarget())) {

                        Particle.DustOptions dustOptions = new Particle.DustOptions(playerBullsEyeData.getColor(), 1);
                        new ParticleBuilder(Particle.REDSTONE)
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
            int playerLevel = getLevel(damager);
            double charge = bullsEyeData.get(damager.getUniqueId()).getCasterCharge().getCharge();
            bullsEyeData.keySet().removeIf(playerUUID -> damager == Bukkit.getPlayer(playerUUID));
            double damage = (getBonusDamage(playerLevel) * charge) + (event.getDamage());
            event.setDamage(damage);
            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 2f, 1.2f);
            UtilMessage.simpleMessage(damagee, getName(), "<alt2>" + damager.getName() + "</alt2> hit you with <alt>" + getName());
            UtilMessage.simpleMessage(damager, getName(), "You hit <alt2>" + damagee.getName() + "</alt2> with <alt>" + getName() + "</alt> for <alt>" + String.format("%.1f", damage) + "</alt> damage");

            //apply cooldown
            championsManager.getCooldowns().removeCooldown(damager, getName(), true);
            championsManager.getCooldowns().use(damager,
                    getName(),
                    getCooldown(playerLevel),
                    true,
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);
        }
    }

    private LivingEntity getPotentialTarget(Player player) {
        // Perform a ray trace with a hitbox size
        int level = getLevel(player);
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                getChargeDistance(level),
                hitboxSize,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        // Check if the ray trace hit an entity
        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        return null;
    }

    private void focusTarget(BullsEyeData playerBullsEyeData) {
        Player caster = playerBullsEyeData.getCaster();

        LivingEntity potentialTarget = getPotentialTarget(caster);

        if (potentialTarget == null) {
            playerBullsEyeData.decayCharge(decayRate);
            return;
        }

        if (!playerBullsEyeData.hasTarget()) {
            playerBullsEyeData.setTarget(potentialTarget);
            playerBullsEyeData.setTargetFocused(new ChargeData(0.5f));
            bullsEyeData.put(caster.getUniqueId(), playerBullsEyeData);
        }

        if (playerBullsEyeData.getTarget() == null || playerBullsEyeData.getTargetFocused() == null) {
            playerBullsEyeData.decayCharge(decayRate);
            return;
        }

        if (potentialTarget == playerBullsEyeData.getTarget()) {
            playerBullsEyeData.getTargetFocused().tick();
            playerBullsEyeData.getCasterCharge().tick();
            playerBullsEyeData.getTargetFocused().tickSound(caster);
            playerBullsEyeData.updateColor();
        } else {
            playerBullsEyeData.decayCharge(decayRate);
        }
    }

    @Override
    public void loadSkillConfig() {
        bonusCurveDistance = getConfig("bonusCurveDistance", 2.5, Double.class);
        bonusCurveDistanceIncreasePerLevel = getConfig("bonusCurveDistanceIncreasePerLevel", 0.5, Double.class);

        baseBonusDamage = getConfig("baseBonusDamage", 2.0, Double.class);
        baseBonusDamageIncreasePerLevel = getConfig("baseBonusDamageIncreasePerLevel", 1.0, Double.class);

        baseCurveDistance = getConfig("baseCurveDistance", 0.5, Double.class);
        decayRate = getConfig("decayRate", 0.01, Double.class);
        hitboxSize = getConfig("hitboxSize", 1.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        chargeDistance = getConfig("chargeDistance", 20, Integer.class);
        chargeDistanceIncreasePerLevel = getConfig("chargeDistanceIncreasePerLevel", 0, Integer.class);
    }
}
