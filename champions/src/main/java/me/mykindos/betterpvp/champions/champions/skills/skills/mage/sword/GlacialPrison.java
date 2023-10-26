package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@Singleton
@BPvPListener
public class GlacialPrison extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WorldBlockHandler blockHandler;

    private int sphereSize;
    private double duration;
    private double speed;

    @Inject
    public GlacialPrison(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Glacial Prison";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Launches an icy orb, trapping any players within <stat>" + sphereSize  + "</stat>",
                "blocks of it in a prison of ice for <stat>" + duration + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }

    @EventHandler
    public void onThrowableHit(ThrowableHitEvent event) {
        if (!event.getThrowable().getName().equals(getName())) return;

        Location center = event.getThrowable().getItem().getLocation();

        for (Location loc : UtilMath.sphere(center, sphereSize, true)) {
            if (loc.getBlockX() == center.getBlockX() &&
                    loc.getBlockZ() == center.getBlockZ() &&
                    loc.getBlockY() == center.getBlockY() + sphereSize) {
                continue;
            }

            if (loc.getBlock().getType().name().contains("REDSTONE")) continue;
            if (loc.getBlock().getType() == Material.AIR || UtilBlock.airFoliage(loc.getBlock())) {
                blockHandler.addRestoreBlock(loc.getBlock(), Material.ICE, (long) (duration * 1000));

                loc.getBlock().setType(Material.ICE);
            }
        }
    }


    @Override
    public void activate(Player player, int level) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.ICE));
        item.setVelocity(player.getLocation().getDirection().multiply(speed));
        ThrowableItem throwableItem = new ThrowableItem(item, player, getName(), 5000, true, true);
        throwableItem.setCollideGround(true);
        championsManager.getThrowables().addThrowable(throwableItem);
    }

    @Override
    public void loadSkillConfig(){
        sphereSize = getConfig("sphereSize", 5, Integer.class);
        duration = getConfig("duration", 5.0, Double.class);
        speed = getConfig("speed", 1.5, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
