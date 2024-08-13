package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerData;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerDataManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Iterator;

@Singleton
@BPvPListener
public class DaggerOfWind extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double blocksPerSecond;
    private double duration;
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    private double hitboxSize;
    private double xSize;
    private double ySize;
    private double zSize;


    @Inject
    public DaggerOfWind(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wind Dagger";
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
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
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

        // Remove any previous DaggerData
        DaggerDataManager daggerDataManager = DaggerDataManager.getInstance();
        daggerDataManager.removeDaggerData(player);

        ItemStack swordItem = switch (level) {
            case 1 -> new ItemStack(Material.WOODEN_SWORD);
            case 2 -> new ItemStack(Material.STONE_SWORD);
            case 3 -> new ItemStack(Material.IRON_SWORD);
            case 4 -> new ItemStack(Material.GOLDEN_SWORD);
            case 5 -> new ItemStack(Material.DIAMOND_SWORD);
            default -> new ItemStack(Material.NETHERITE_SWORD);
        };

        Location startLocation = player.getEyeLocation();
        Vector direction = startLocation.getDirection().normalize();
        Location swordLocation = startLocation.clone().add(direction.multiply(1.0));

        ItemDisplay swordDisplay = startLocation.getWorld().spawn(swordLocation, ItemDisplay.class, display -> {
            display.setItemStack(swordItem);
            display.setGlowing(false);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(xSize, ySize, zSize);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(rotationX));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(rotationY));
            transformation.getLeftRotation().rotateLocalZ((float) Math.toRadians(rotationZ));
            display.setTransformation(transformation);
        });

        long throwTime = System.currentTimeMillis();
        DaggerData data = new DaggerData(player, swordDisplay, startLocation, direction, null, throwTime, UtilBlock.isGrounded(player));

        daggerDataManager.setDaggerData(player, data);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d<gray>.", getName(), level);
    }

    @UpdateEvent
    public void checkCollide() {
        DaggerDataManager manager = DaggerDataManager.getInstance();
        Iterator<Player> iterator = manager.getAllPlayers().iterator();

        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (player == null || !player.isOnline()) {
                manager.removeDaggerData(player);
                continue;
            }

            int level = getLevel(player);

            DaggerData data = manager.getDaggerData(player);
            if (data == null) continue;

            long currentTime = System.currentTimeMillis();
            if (currentTime - data.getThrowTime() > getDuration(level) * 1000) {
                dissapear(data.getSwordDisplay());
                manager.removeDaggerData(player);
                continue;
            }

            Location previousLocation = data.getSwordDisplay().getLocation();
            Vector direction = data.getDirection();
            double distance = blocksPerSecond / 20.0;

            Location newLocation = previousLocation.clone().add(direction.clone().multiply(distance));

            // Set the interpolation duration to smoothly move the sword
            data.getSwordDisplay().setInterpolationDuration(1); // 1 tick for smooth movement
            data.getSwordDisplay().setTeleportDuration(1); // 1 tick for smooth movement
            data.getSwordDisplay().teleport(newLocation);

            Particle.SMALL_GUST.builder()
                    .count(1)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(newLocation)
                    .receivers(30)
                    .spawn();

            RayTraceResult rayTrace = previousLocation.getWorld().rayTrace(previousLocation,
                    direction,
                    distance,
                    FluidCollisionMode.NEVER,
                    true,
                    hitboxSize,
                    entity -> entity instanceof LivingEntity && entity != player);

            if (rayTrace != null && rayTrace.getHitEntity() != null) {
                LivingEntity hitEntity = (LivingEntity) rayTrace.getHitEntity();

                data.setHitLocation(rayTrace.getHitPosition().toLocation(previousLocation.getWorld()));
                if (player.getGameMode() != GameMode.CREATIVE) {
                    UtilInventory.remove(player, Material.ARROW, 1);
                }
                if(hitEntity instanceof Player targetPlayer){
                    if (targetPlayer.getGameMode() != GameMode.CREATIVE) {
                        collide(player, hitEntity);
                        dissapear(data.getSwordDisplay());
                    }
                } else {
                    collide(player, hitEntity);
                    dissapear(data.getSwordDisplay());
                }
            }
            //get block and stop sword if it goes through one
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void processDamage(CustomDamageEvent event) {
        if (!(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;
        if (!event.hasReason(getName())) return;

        Player damager = (Player) event.getDamager();
        DaggerDataManager.getInstance().removeDaggerData(damager);
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
        blocksPerSecond = getConfig("blocksPerSecond", 20.0, Double.class);
        duration = getConfig("duration", 1.0, Double.class);
        rotationX = getConfig("rotationX", 90.0, Double.class);  // Added rotationX configuration
        rotationY = getConfig("rotationY", 45.0, Double.class);   // Added rotationY configuration
        rotationZ = getConfig("rotationZ", 0.0, Double.class);   // Added rotationZ configuration
        hitboxSize = getConfig("hitboxSize", 0.4, Double.class); // Added hitboxSize configuration
        xSize = getConfig("xSize", 0.5, Double.class); // Added hitboxSize configuration
        ySize = getConfig("ySize", 0.5, Double.class); // Added hitboxSize configuration
        zSize = getConfig("zSize", 1.0, Double.class); // Added hitboxSize configuration
    }
}
