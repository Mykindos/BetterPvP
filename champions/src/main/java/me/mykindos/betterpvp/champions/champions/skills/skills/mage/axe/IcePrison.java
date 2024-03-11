package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class IcePrison extends Skill implements InteractSkill, CooldownSkill, Listener, ThrowableListener {

    private final WorldBlockHandler blockHandler;
    private int sphereSize;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double speed;

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
        return baseDuration + level * durationIncreasePerLevel;
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
                    blockHandler.addRestoreBlock(player, loc.getBlock(), Material.ICE, (long) (getDuration(level) * 1000), true);
                }
                loc.getBlock().setType(Material.ICE);
            }
        }
    }


    @Override
    public void activate(Player player, int level) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.ICE));
        item.setVelocity(player.getLocation().getDirection().multiply(speed));
        ThrowableItem throwableItem = new ThrowableItem(this, item, player, getName(), 10000, true);
        throwableItem.setCollideGround(true);
        championsManager.getThrowables().addThrowable(throwableItem);
    }

    @Override
    public void loadSkillConfig(){
        sphereSize = getConfig("sphereSize", 4, Integer.class);
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        speed = getConfig("speed", 1.5, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

}
