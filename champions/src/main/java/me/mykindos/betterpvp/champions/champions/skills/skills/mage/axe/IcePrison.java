package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.blocks.RestoreBlock;
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

import java.util.Collection;
import java.util.List;

@CustomLog
@Singleton
@BPvPListener
public class IcePrison extends Skill implements InteractSkill, CooldownSkill, Listener, ThrowableListener, CrowdControlSkill, WorldSkill {

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
        return new String[] {
                "Right click with an Axe to activate",
                "",
                "Launches an icy orb, trapping any players within " + getValueString(this::getSphereSize, level, 0),
                "blocks of it in a prison of ice for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Shift-click to destroy the prison early.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    private double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    private int getSphereSize(int level) {
        return sphereSize;
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

    private boolean hasActivePrison(Player player) {
        return !blockHandler.getRestoreBlocks(player, getName()).isEmpty();
    }

    public void despawn(Player player) {
        final List<RestoreBlock> blocks = blockHandler.getRestoreBlocks(player, getName());
        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(blocks.get(0).getBlock().getLocation(), 60);
        for (RestoreBlock block : blocks) {
            final Location loc = block.getBlock().getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_STEP, 0.4f, 1.8f);
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.9f);
            Particle.CLOUD.builder().location(loc).receivers(receivers).extra(0).spawn();
            block.restore();
        }
        UtilMessage.message(player, getClassType().getName(), "You destroyed your <alt>" + getName() + "</alt>.");
    }

    private void handleIcePrisonCollision(ThrowableItem throwableItem) {
        Location center = throwableItem.getItem().getLocation();
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_STEP, 1f, 1f);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);

        for (Location loc : UtilMath.sphere(center, sphereSize, true)) {
            if (loc.getBlockX() == center.getBlockX() && loc.getBlockZ() == center.getBlockZ()) {
                continue;
            }

            if (loc.getBlock().getType().name().contains("REDSTONE")) continue;
            if (loc.getBlock().getType() == Material.AIR || UtilBlock.airFoliage(loc.getBlock())) {
                int level = getLevel((Player) throwableItem.getThrower());
                if (throwableItem.getThrower() instanceof Player player) {
                    double duration = getDuration(level) + (((double) (center.getBlockY() - loc.getBlockY()) / sphereSize) * variance);
                    blockHandler.addRestoreBlock(player, loc.getBlock(), Material.ICE, (long) (duration * 1000), true, getName());
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
        throwableItem.getLastLocation().getWorld().spawnParticle(Particle.ITEM_SNOWBALL, throwableItem.getLastLocation(), 1);
    }

    @Override
    public void activate(Player player, int level) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.ICE));
        item.setVelocity(player.getLocation().getDirection().multiply(speed));
        ThrowableItem throwableItem = new ThrowableItem(this, item, player, getName(), 10000, true);
        throwableItem.setCollideGround(true);
        throwableItem.setCanHitFriendlies(true);
        championsManager.getThrowables().addThrowable(throwableItem);
        throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.ENTITY_SILVERFISH_HURT, 2f, 1f);
    }

    @Override
    public void loadSkillConfig(){
        sphereSize = getConfig("sphereSize", 4, Integer.class);
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        speed = getConfig("speed", 1.5, Double.class);
        variance = getConfig("variance", 0.5, Double.class);
    }

    @Override
    public boolean canUse(Player player) {
        if (player.isSneaking() && hasActivePrison(player)) {
            if (championsManager.getCooldowns().use(player, getName() + "Despawn", 0.2, false)) {
                despawn(player);
            }
            return false;
        }

        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

}