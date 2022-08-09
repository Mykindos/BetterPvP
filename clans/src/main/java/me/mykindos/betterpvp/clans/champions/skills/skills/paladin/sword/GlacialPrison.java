package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.clans.combat.throwables.events.ThrowableHitEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class GlacialPrison extends Skill implements InteractSkill, CooldownSkill, Listener {

    @Inject
    @Config(path="skills.paladin.glacialprison.sphereSize", defaultValue = "5")
    private int sphereSize;

    @Inject
    @Config(path="skills.paladin.glacialprison.duration", defaultValue = "4")
    private double duration;

    @Inject
    @Config(path="skills.paladin.glacialprison.speed", defaultValue = "1.5")
    private double speed;

    private final WorldBlockHandler blockHandler;

    @Inject
    public GlacialPrison(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory, WorldBlockHandler blockHandler) {
        super(clans, championsManager, configFactory);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Glacial Prison";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to Activate",
                "",
                "Launches an orb, trapping any players",
                "within 5 blocks of it in a prison of ice for 5 seconds",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @Override
    public double getCooldown(int level) {

        return getSkillConfig().getCooldown() - ((level - 1) * 2);
    }

    @EventHandler
    public void onThrowableHit(ThrowableHitEvent event) {
        if (!event.getThrowable().getSkillName().equals(getName())) return;
        event.getThrowable().getItem().remove();
        for (Location loc : UtilMath.sphere(event.getThrowable().getItem().getLocation(), sphereSize, true)) {
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
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setVelocity(player.getLocation().getDirection().multiply(speed));
        championsManager.getThrowables().addThrowable(item, player, getName(), 5000);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
