package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

@Slf4j
@Singleton
@BPvPListener
public class IcePrison extends Skill implements InteractSkill, CooldownSkill, Listener, ThrowableListener {

    private final WorldBlockHandler blockHandler;
    private int sphereSize;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double speed;
    private double variance;

    @Inject
    public IcePrison(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Ice Prison";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Launches an icy orb, trapping any players within <stat>" + sphereSize  + "</stat>",
                "blocks of it in a prison of ice for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    private double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        handleIcePrisonCollision(throwableItem);
    }

    @Override
    public void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, Location location) {
        handleIcePrisonCollision(throwableItem);
    }

    private void handleIcePrisonCollision(ThrowableItem throwableItem) {
        Location center = throwableItem.getItem().getLocation();

        for (Location loc : UtilMath.sphere(center, sphereSize, true)) {
            if (loc.getBlockX() == center.getBlockX() &&
                    loc.getBlockZ() == center.getBlockZ() &&
                    loc.getBlockY() == center.getBlockY() + sphereSize) {
                continue;
            }

            if (loc.getBlock().getType().name().contains("REDSTONE")) continue;
            if (loc.getBlock().getType() == Material.AIR || UtilBlock.airFoliage(loc.getBlock())) {
                int level = getLevel((Player) throwableItem.getThrower());
                if (throwableItem.getThrower() instanceof Player player) {
                    double duration = getDuration(level) + (((double) (center.getBlockY() - loc.getBlockY()) / sphereSize) * variance);
                    blockHandler.addRestoreBlock(player, loc.getBlock(), Material.ICE, (long) (duration * 1000), true);
                }
                loc.getBlock().setType(Material.ICE);
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_STEP, 1f, 1f);
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
            }
        }
    }

    @Override
    public void onTick(ThrowableItem throwableItem) {
        throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.6f);
        throwableItem.getLastLocation().getWorld().spawnParticle(Particle.SNOW_SHOVEL, throwableItem.getLastLocation(), 1);
    }

    @Override
    public void activate(Player player, int level) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.ICE));
        item.setVelocity(player.getLocation().getDirection().multiply(speed));
        ThrowableItem throwableItem = new ThrowableItem(this, item, player, getName(), 10000, true);
        throwableItem.setCollideGround(true);
        championsManager.getThrowables().addThrowable(throwableItem);
        throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.ENTITY_SILVERFISH_HURT, 2f, 1f);

    }
    @Override
    public void loadSkillConfig(){
        sphereSize = getConfig("sphereSize", 4, Integer.class);
        baseDuration = getConfig("baseDuration", 6.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        speed = getConfig("speed", 1.5, Double.class);
        variance = getConfig("variance", 0.5, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

}
