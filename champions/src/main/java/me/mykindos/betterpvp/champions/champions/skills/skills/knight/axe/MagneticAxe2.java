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
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class MagneticAxe2 extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private WeakHashMap<Player, AxeData> axeDataMap = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double duration;
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    private double hitboxSize;
    private double xSize;
    private double ySize;
    private double zSize;
    private double magnitude;
    private double gravity;
    private double yawOffset;
    private Vector airDrag; // New air drag vector

    @Inject
    public MagneticAxe2(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magnetic Axe 2";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Throw a dagger that will fly for " + getValueString(this::getDuration, level) + " seconds",
                "and deal " + getValueString(this::getDamage, level) + " damage to enemies it hits",
                "",
                "The dagger will inherit anything that affects your sword",
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
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 2.0F);

        ItemStack axeItem = new ItemStack(Material.IRON_AXE);

        Location initialPosition = player.getEyeLocation();
        Vector direction = initialPosition.getDirection().normalize();
        Vector initialVelocity = direction.multiply(magnitude);

        // Treat gravity as a constant velocity vector instead of an acceleration
        Vector gravityVector = new Vector(0, -gravity, 0);

        long startTime = System.currentTimeMillis();
        float initialYaw = player.getLocation().getYaw(); // Capture the initial yaw

        // Apply yaw offset and store the adjusted yaw
        float adjustedYaw = initialYaw + (float) yawOffset;

        ItemDisplay axeDisplay = initialPosition.getWorld().spawn(initialPosition, ItemDisplay.class, display -> {
            display.setItemStack(axeItem);
            display.setGlowing(false);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(xSize, ySize, zSize);

            // Apply the adjusted yaw rotation (around Y-axis) only during creation
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-adjustedYaw));

            // Apply any additional initial rotations from config
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(rotationX));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(rotationY));
            transformation.getLeftRotation().rotateLocalZ((float) Math.toRadians(rotationZ));

            display.setTransformation(transformation);
        });

        AxeData axeData = new AxeData(axeDisplay, initialPosition, initialVelocity, gravityVector, startTime, adjustedYaw);
        axeDataMap.put(player, axeData);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d<gray>.", getName(), level);
    }


    @UpdateEvent
    public void checkCollide() {
        Iterator<Player> iterator = axeDataMap.keySet().iterator();

        while (iterator.hasNext()) {
            Player player = iterator.next();
            AxeData axeData = axeDataMap.get(player);

            if (axeData == null || player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            long currentTime = System.currentTimeMillis();
            double elapsedTime = (currentTime - axeData.getStartTime()) / 1000.0; // Convert to seconds

            if (elapsedTime > getDuration(getLevel(player))) {
                dissapear(axeData.getAxeDisplay());
                iterator.remove();
                continue;
            }

            Location newLocation = UtilVelocity.fakeVelocity(
                    axeData.getInitialPosition(), //doesnt matter
                    axeData.getInitialVelocity(), // 4.0 in initial direction
                    axeData.getGravity(), // Vector(0, -2.0, 0);
                    airDrag,  //vector (0.2, 0.2, 0.2);
                    elapsedTime  //miliseconds
            );

            // Calculate the current velocity
            Vector currentVelocity = axeData.getInitialVelocity().clone().add(axeData.getGravity().clone().multiply(elapsedTime)).subtract(airDrag.clone().multiply(elapsedTime));

            // Print the velocity at the current position
            System.out.println("Axe velocity: " + currentVelocity);

            axeData.getAxeDisplay().setInterpolationDuration(1); // 1 tick for smooth movement
            axeData.getAxeDisplay().setTeleportDuration(1); // 1 tick for smooth movement
            axeData.getAxeDisplay().teleport(newLocation);

            Transformation transformation = axeData.getAxeDisplay().getTransformation();
            int level = getLevel(player);

            // Calculate the new pitch rotation angle
            float pitch = (float) (elapsedTime / getDuration(level) * -720.0); // Calculate pitch based on elapsed time

            // Create a rotation axis rotated by 90 degrees from the X-axis
            Vector3f axis = new Vector3f(0, 0, 1); // Rotated by 90 degrees around the Y-axis

            AxisAngle4f pitchRotation = new AxisAngle4f((float) Math.toRadians(pitch), axis);

            transformation.getLeftRotation().set(pitchRotation);

            // Apply the stored initial yaw rotation (around Y-axis) without reapplying the offset
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(-axeData.getInitialYaw()));

            axeData.getAxeDisplay().setTransformation(transformation);

            // Check for collisions with entities and blocks
            Vector direction = newLocation.toVector().subtract(axeData.getInitialPosition().toVector());
            double distance = direction.length();

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
                    // If a block was hit, make the axe disappear
                    dissapear(axeData.getAxeDisplay());
                    iterator.remove();
                    continue;
                }
                dissapear(axeData.getAxeDisplay());
                iterator.remove();
            } else {
                // Update initial position for the next tick
                axeData.setInitialPosition(newLocation);
            }
        }
    }


    private void collide(Player damager, LivingEntity damagee) {
        final int level = getLevel(damager);
        double damage = getDamage(level);

        damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0F, 2.0F);
        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage, true, getName()));

        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", damagee.getName(), getName(), level);
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", damager.getName(), getName(), level);
    }

    public void returnAxe(ItemDisplay dagger){

    }

    public void dissapear(ItemDisplay dagger) {
        dagger.remove();

        Location particleLocation = dagger.getLocation();
        Particle.GUST.builder()
                .count(0)
                .extra(0)
                .offset(0.0, 0.0, 0.0)
                .location(particleLocation)
                .receivers(30)
                .spawn();
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        duration = getConfig("duration", 10.0, Double.class);
        rotationX = getConfig("rotationX", 0.0, Double.class);  // Added rotationX configuration
        rotationY = getConfig("rotationY", 0.0, Double.class);   // Added rotationY configuration
        rotationZ = getConfig("rotationZ", 0.0, Double.class);   // Added rotationZ configuration
        hitboxSize = getConfig("hitboxSize", 0.4, Double.class); // Added hitboxSize configuration
        xSize = getConfig("xSize", 0.75, Double.class); // Added xSize configuration
        ySize = getConfig("ySize", 0.75, Double.class); // Added ySize configuration
        zSize = getConfig("zSize", 0.75, Double.class); // Added zSize configuration
        magnitude = getConfig("magnitude", 4.0, Double.class); // Added magnitude configuration
        gravity = getConfig("gravity", 2.0, Double.class); // Added gravity configuration
        yawOffset = getConfig("yawOffset", 90.0, Double.class); // Added yawOffset configuration

        // Load air drag configuration
        double airDragX = getConfig("airDragX", 0.2, Double.class);
        double airDragY = getConfig("airDragY", 0.2, Double.class);
        double airDragZ = getConfig("airDragZ", 0.2, Double.class);
        airDrag = new Vector(airDragX, airDragY, airDragZ);
    }
}

