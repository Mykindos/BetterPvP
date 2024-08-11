package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.AxeData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Effect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class MagneticAxe extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, List<AxeData>> axeDataMap = new WeakHashMap<>();
    private final WeakHashMap<Player, List<AxeData>> returningAxes = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double duration;
    private double hitboxSize;
    private double xSize;
    private double ySize;
    private double zSize;
    private double magnitude;
    private double gravity;
    private double yawOffset;
    private double dragConstant;
    private int rotations;
    private double initialAngle;
    private double blocksPerSecond;
    private double maxVelocity;

    @Inject
    public MagneticAxe(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magnetic Axe";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Throw your axe, dealing " + getValueString(this::getDamage, level),
                "damage to anything you hit and",
                "pulling your axe back to you",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getDuration(int level) {
        return duration;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void activate(Player player, int level) {
        if (!isHolding(player)) return;

        ItemStack axeItem = player.getInventory().getItemInMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.0F);
        player.getInventory().setItemInMainHand(null);

        Location initialPosition = player.getLocation().add(0, 1, 0);
        Vector playerDirection = player.getLocation().getDirection();

        Vector perpendicularAxis = playerDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Location rightHandPosition = initialPosition.add(perpendicularAxis.multiply(0.3));

        Vector direction = player.getLocation().getDirection().normalize();
        double angleInRadians = Math.toRadians(initialAngle);
        direction.setY(direction.getY() + Math.sin(angleInRadians));
        direction.normalize();

        Vector initialVelocity = direction.multiply(magnitude);
        Vector gravityVector = new Vector(0, -gravity, 0);
        long startTime = System.currentTimeMillis();
        float initialYaw = player.getLocation().getYaw();
        float adjustedYaw = initialYaw + (float) yawOffset;

        ItemDisplay axeDisplay = rightHandPosition.getWorld().spawn(rightHandPosition, ItemDisplay.class, display -> {
            display.setItemStack(axeItem);
            display.setGlowing(false);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(xSize, ySize, zSize);

            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-adjustedYaw));
            display.setTransformation(transformation);
        });

        AxeData axeData = new AxeData(axeDisplay, rightHandPosition, initialVelocity, gravityVector, startTime, adjustedYaw);
        axeData.setOriginalItem(axeItem);
        axeDataMap.computeIfAbsent(player, key -> new ArrayList<>()).add(axeData);

    }


    @UpdateEvent
    public void checkCollide() {
        Iterator<Map.Entry<Player, List<AxeData>>> iterator = axeDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, List<AxeData>> entry = iterator.next();
            Player player = entry.getKey();
            List<AxeData> axeList = entry.getValue();
            ListIterator<AxeData> listIterator = axeList.listIterator();

            while (listIterator.hasNext()) {
                AxeData axeData = listIterator.next();
                int level = getLevel(player);

                if (axeData == null || player == null || !player.isOnline()) {
                    listIterator.remove();
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                double elapsedTime = (currentTime - axeData.getStartTime()) / 1000.0;

                if (elapsedTime > getDuration(level)) {
                    returningAxes.computeIfAbsent(player, key -> new ArrayList<>()).add(axeData);
                    listIterator.remove();
                    continue;
                }

                Vector currentVelocity = UtilVelocity.getVelocity(
                        axeData.getInitialVelocity(),
                        axeData.getGravity(),
                        dragConstant,
                        elapsedTime
                );

                if (currentVelocity.length() > maxVelocity) {
                    currentVelocity = currentVelocity.normalize().multiply(maxVelocity);
                }

                Location newLocation = UtilVelocity.getPosition(
                        axeData.getInitialPosition(),
                        currentVelocity,
                        axeData.getGravity(),
                        dragConstant,
                        elapsedTime
                );

                axeData.getAxeDisplay().getWorld().playSound(axeData.getAxeDisplay().getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.5F, 1.0F);

                final Location particleLocation = axeData.getInitialPosition();
                Particle.CRIT.builder()
                        .count(3)
                        .extra(0)
                        .offset(0.1, 0.1, 0.1)
                        .location(particleLocation)
                        .receivers(60)
                        .spawn();

                axeData.getAxeDisplay().setInterpolationDuration(1);
                axeData.getAxeDisplay().setTeleportDuration(1);
                axeData.getAxeDisplay().teleport(newLocation);

                Transformation transformation = axeData.getAxeDisplay().getTransformation();
                float pitch = (float) (elapsedTime / getDuration(level) * ((-360.0) * rotations));
                Vector3f axis = new Vector3f(0, 0, 1);
                AxisAngle4f pitchRotation = new AxisAngle4f((float) Math.toRadians(pitch), axis);
                transformation.getLeftRotation().set(pitchRotation);
                transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-axeData.getInitialYaw()));

                axeData.getAxeDisplay().setTransformation(transformation);

                Vector direction = newLocation.toVector().subtract(axeData.getInitialPosition().toVector());
                double distance = direction.length();

                if (!NumberConversions.isFinite(direction.getX()) ||
                        !NumberConversions.isFinite(direction.getY()) ||
                        !NumberConversions.isFinite(direction.getZ())){
                    continue;
                }

                if (distance > 0) {
                    direction = direction.normalize();
                } else {
                    continue;
                }

                RayTraceResult rayTrace = newLocation.getWorld().rayTrace(newLocation,
                        direction.normalize(),
                        distance,
                        FluidCollisionMode.NEVER,
                        true,
                        hitboxSize,
                        entity -> entity instanceof LivingEntity && entity != player);

                if (rayTrace != null) {
                    if (rayTrace.getHitEntity() != null) {
                        LivingEntity hitEntity = (LivingEntity) rayTrace.getHitEntity();
                        collide(player, hitEntity);
                    } else if (rayTrace.getHitBlock() != null) {
                        rayTrace.getHitBlock().getWorld().playEffect(rayTrace.getHitBlock().getLocation(), Effect.STEP_SOUND, rayTrace.getHitBlock().getType());

                        returningAxes.computeIfAbsent(player, key -> new ArrayList<>()).add(axeData);
                        axeData.getAxeDisplay().getWorld().playSound(axeData.getAxeDisplay().getLocation(), Sound.ITEM_MACE_SMASH_AIR, 2.0F, 1.0F);
                        listIterator.remove();
                        continue;
                    }
                    returningAxes.computeIfAbsent(player, key -> new ArrayList<>()).add(axeData);
                    axeData.getAxeDisplay().getWorld().playSound(axeData.getAxeDisplay().getLocation(), Sound.ITEM_MACE_SMASH_AIR, 2.0F, 1.0F);
                    listIterator.remove();
                } else {
                    axeData.setInitialPosition(newLocation);
                }
            }

            if (axeList.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @UpdateEvent
    public void returnAxe() {
        Map<Player, List<AxeData>> updatedAxeData = new HashMap<>();
        List<AxeData> toRemoveGlobal = new ArrayList<>();
        List<Player> playersToRemove = new ArrayList<>();


        for (Map.Entry<Player, List<AxeData>> entry : returningAxes.entrySet()) {
            Player player = entry.getKey();
            List<AxeData> axeList = new ArrayList<>(entry.getValue());

            List<AxeData> updatedAxeList = new ArrayList<>();
            List<AxeData> toRemoveLocal = new ArrayList<>();

            for (AxeData axeData : axeList) {
                if (axeData == null || player == null || !player.isOnline()) {
                    toRemoveLocal.add(axeData);
                    continue;
                }

                Location axeLocation = axeData.getAxeDisplay().getLocation();
                Location playerLocation = player.getEyeLocation();

                final Location particleLocation = axeData.getInitialPosition();
                Particle.ENCHANTED_HIT.builder()
                        .count(3)
                        .extra(0)
                        .offset(0.1, 0.1, 0.1)
                        .location(particleLocation)
                        .receivers(60)
                        .spawn();

                double teleportDistancePerTick = blocksPerSecond / 20.0;
                Vector direction = playerLocation.toVector().subtract(axeLocation.toVector()).normalize();
                Vector moveVector = direction.multiply(teleportDistancePerTick);

                axeLocation.add(moveVector);
                axeData.getAxeDisplay().teleport(axeLocation);
                axeData.getAxeDisplay().getWorld().playSound(axeData.getAxeDisplay().getLocation(), Sound.ENTITY_BREEZE_INHALE, 0.2F, 2.0F);
                axeData.setInitialPosition(axeLocation);

                if (axeLocation.distance(playerLocation) < 1.0) {
                    ItemStack originalAxe = axeData.getOriginalItem();
                    int emptySlot = player.getInventory().firstEmpty();

                    if (emptySlot != -1) {
                        player.getInventory().setItem(emptySlot, originalAxe);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 2.0F);
                    } else if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                        player.getInventory().setItemInMainHand(originalAxe);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 2.0F);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), originalAxe);
                    }
                    disappear(player, axeData.getAxeDisplay());
                    toRemoveLocal.add(axeData);
                } else {
                    updatedAxeList.add(axeData);
                }
            }

            if (!toRemoveLocal.isEmpty()) {
                toRemoveGlobal.addAll(toRemoveLocal);
            }

            if (!updatedAxeList.isEmpty()) {
                updatedAxeData.put(player, updatedAxeList);
            } else {
                playersToRemove.add(player);
            }
        }

        for (AxeData axeData : toRemoveGlobal) {
            for (List<AxeData> axeList : returningAxes.values()) {
                axeList.remove(axeData);
            }
        }

        for (Player player : playersToRemove) {
            returningAxes.remove(player);
        }

        returningAxes.putAll(updatedAxeData);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        List<AxeData> axeList = axeDataMap.remove(player);

        if (axeList == null) {
            axeList = returningAxes.remove(player);
        }

        if (axeList != null) {
            for (AxeData axeData : axeList) {
                disappear(player, axeData.getAxeDisplay());
                axeData.getAxeDisplay().getWorld().dropItemNaturally(axeData.getAxeDisplay().getLocation(), axeData.getOriginalItem());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        List<AxeData> axeList = axeDataMap.remove(player);

        if (axeList == null) {
            axeList = returningAxes.remove(player);
        }

        if (axeList != null) {
            for (AxeData axeData : axeList) {
                disappear(player, axeData.getAxeDisplay());
                axeData.getAxeDisplay().getWorld().dropItemNaturally(axeData.getAxeDisplay().getLocation(), axeData.getOriginalItem());
            }
        }
    }

    private void collide(Player damager, LivingEntity damagee) {
        final int level = getLevel(damager);
        double damage = getDamage(level);

        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));


        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", damagee.getName(), getName(), level);
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", damager.getName(), getName(), level);
    }

    public void disappear(Player player, ItemDisplay axe) {
        axe.remove();
        returningAxes.getOrDefault(player, new ArrayList<>()).removeIf(axeData -> axeData.getAxeDisplay().equals(axe));
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        duration = getConfig("duration", 10.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.4, Double.class);
        xSize = getConfig("xSize", 0.75, Double.class);
        ySize = getConfig("ySize", 0.75, Double.class);
        zSize = getConfig("zSize", 0.75, Double.class);
        magnitude = getConfig("magnitude", 5.0, Double.class);
        gravity = getConfig("gravity", 1.0, Double.class);
        yawOffset = getConfig("yawOffset", 90.0, Double.class);
        rotations = getConfig("rotations", 20, Integer.class);
        dragConstant = getConfig("dragConstant", 0.2, Double.class);
        initialAngle = getConfig("initialAngle", 15.0, Double.class);
        blocksPerSecond = getConfig("blocksPerSecond", 30.0, Double.class);
        maxVelocity = getConfig("maxVelocity", 5.0, Double.class);
    }
}
