package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Leap extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double leapStrength;

    private double wallKickStrength;

    @Inject
    public Leap(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate.",
                "",
                "Take a great leap forward",
                "",
                "Activate while your back is to a wall to perform",
                "a wall-kick, which will not affect the cooldown",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void activate(Player player, int level) {
        if (!wallKick(player)) {
            doLeap(player, false);
        }
    }

    public void doLeap(Player player, boolean wallkick) {

        if (!wallkick) {
            UtilVelocity.velocity(player, leapStrength, 0.2D, 1.0D, true);
        } else {
            Vector vec = player.getLocation().getDirection();
            vec.setY(0);
            UtilVelocity.velocity(player, vec, wallKickStrength, false, 0.0D, 0.8D, 2.0D, true);
            UtilMessage.message(player, getClassType().getName(), "You used <alt>Wall Kick</alt>.");
        }

        player.getWorld().spawnEntity(player.getLocation(), EntityType.LLAMA_SPIT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 2.0F, 1.2F);
    }


    public boolean wallKick(Player player) {

        if (championsManager.getCooldowns().use(player, "Wall Kick", 0.25, false)) {
            Vector vec = player.getLocation().getDirection();

            boolean xPos = true;
            boolean zPos = true;

            if (vec.getX() < 0.0D) {
                xPos = false;
            }
            if (vec.getZ() < 0.0D) {
                zPos = false;
            }


            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if ((x != 0) || (z != 0)) {
                        if (((!xPos) || (x <= 0))
                                && ((!zPos) || (z <= 0))
                                && ((xPos) || (x >= 0)) && ((zPos) || (z >= 0))) {
                            Block back = player.getLocation().getBlock().getRelative(x, 0, z);
                            if (!UtilBlock.airFoliage(back)) {
                                if (back.getLocation().getY() == Math.floor(player.getLocation().getY())
                                        || back.getLocation().getY() == Math.floor(player.getLocation().getY() - 0.25)) {
                                    if (UtilBlock.airFoliage(back.getRelative(BlockFace.UP).getType())) {
                                        if (!UtilBlock.airFoliage(player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
                                            continue;
                                        }
                                    }
                                }
                                Block forward;

                                if (Math.abs(vec.getX()) > Math.abs(vec.getZ())) {
                                    if (xPos) {
                                        forward = player.getLocation().getBlock().getRelative(1, 0, 0);
                                    } else {
                                        forward = player.getLocation().getBlock().getRelative(-1, 0, 0);
                                    }

                                } else if (zPos) {
                                    forward = player.getLocation().getBlock().getRelative(0, 0, 1);
                                } else {
                                    forward = player.getLocation().getBlock().getRelative(0, 0, -1);
                                }

                                if (UtilBlock.airFoliage(forward)) {
                                    if (Math.abs(vec.getX()) > Math.abs(vec.getZ())) {
                                        if (xPos) {
                                            forward = player.getLocation().getBlock().getRelative(1, 1, 0);
                                        } else {
                                            forward = player.getLocation().getBlock().getRelative(-1, 1, 0);
                                        }
                                    } else if (zPos) {
                                        forward = player.getLocation().getBlock().getRelative(0, 1, 1);
                                    } else {
                                        forward = player.getLocation().getBlock().getRelative(0, 1, -1);
                                    }

                                    if (UtilBlock.airFoliage(forward)) {

                                        doLeap(player, true);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }


        return false;
    }

    @Override
    public boolean canUse(Player player) {

        return !wallKick(player);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean canUseSlowed() {
        return false;
    }

    @Override
    public void loadSkillConfig(){
        leapStrength = getConfig("leapStrength", 1.3, Double.class);
        wallKickStrength = getConfig("wallKickStrength", 0.9, Double.class);
    }
}
