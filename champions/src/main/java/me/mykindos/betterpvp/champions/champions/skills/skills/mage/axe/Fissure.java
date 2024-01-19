package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;


@Singleton
@BPvPListener
public class Fissure extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private int fissureDistance;
    private double fissureExpireDuration;
    private double baseExtraDamagePerBlock;
    private int effectDurationIncreasePerLevel;
    private double baseExtraDamagePerBlockIncreasePerLevel;
    private int effectDuration;
    private int slownessLevel;
    private Map<Player, ArrayList<Block>> playerFissurePaths = new HashMap<>();
    private Map<Player, HashSet<Player>> playerHits = new HashMap<>();
    private Map<Player, List<Block>> playerCreatedBlocks = new HashMap<>();

    @Inject
    public Fissure(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fissure";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Fissure the earth infront of you,",
                "creating an impassable wall",
                "",
                "Players struck by wall will",
                "receive <effect>Slowness"+ UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> for <val>" + getSlowDuration(level) + "</val> seconds",
                "and take <val>"+getDamage(level)+"</val> damage plus an",
                "additional <val>" + getExtraDamage(level) + "</val> damage for",
                "every block fissure has travelled.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    public double getExtraDamage(int blocksTraveled, int level){
        return (baseExtraDamagePerBlock + (level * baseExtraDamagePerBlockIncreasePerLevel)) * blocksTraveled;
    }

    public double getExtraDamage(int level){
        return (baseExtraDamagePerBlock + (level * baseExtraDamagePerBlockIncreasePerLevel));
    }

    public int getSlowDuration(int level){
        return effectDuration + (level * effectDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @UpdateEvent
    public void onUpdate() {

    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You can only use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int level) {
        playerFissurePaths.put(player, new ArrayList<>());
        playerHits.put(player, new HashSet<>());
        playerCreatedBlocks.put(player, new ArrayList<>());

        System.out.println("activated and put player in maps");

        createFissure(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                System.out.println("called remove fissure");
                removeFissure(player);
            }
        }.runTaskLater(champions, (long) (fissureExpireDuration * 20));
    }

    private void onPlayerCollision(Player target, int blocksTraveled, Player player) {
        HashSet<Player> playersHit = playerHits.get(player);
        if (playersHit != null && !playersHit.contains(target)) {
            System.out.println("hit player not null that wasnt already hit");
            playersHit.add(target);

            int level = getLevel(player);

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, effectDuration, slownessLevel));
            UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + getName());
            UtilMessage.simpleMessage(target, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName());

            double damage = getDamage(level) + getExtraDamage(blocksTraveled, level);
            target.damage(damage);
            System.out.println("all effects applied");
        }
    }

    public void createFissure(Player player) {
        ArrayList<Block> fissurePath = playerFissurePaths.get(player);

        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        direction.multiply(0.1); // Adjust this value as necessary

        for (int i = 0; i < fissureDistance; i++) {
            // Clone the player's location and adjust the Y-coordinate
            Location locationToCheck = player.getLocation().clone().add(direction.clone().multiply(i));
            locationToCheck.setY(locationToCheck.getY() - 1); // Adjust the Y-coordinate

            Block currentBlock = locationToCheck.getBlock();

            System.out.println("Checking block at: " + locationToCheck.toString() + " - Solid: " + UtilBlock.solid(currentBlock));

            if (!UtilBlock.solid(currentBlock)) {
                System.out.println("Not a solid block at: " + locationToCheck.toString());
                break;
            }

            if (!fissurePath.contains(currentBlock)) {
                fissurePath.add(currentBlock);
                System.out.println("Added block to fissurePath at: " + currentBlock.getLocation().toString());
            }
        }

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= fissurePath.size()) {
                    this.cancel();
                    return;
                }

                Block currentBlock = fissurePath.get(index);
                int blocksTraveled = index;

                System.out.println("called createFissurePillar");

                createFissurePillar(currentBlock, blocksTraveled, player);

                index++;
            }

        }.runTaskTimer(champions, 0L, 1L);
    }

    private void createFissurePillar(Block block, int blocksTraveled, Player player) {
        List<Block> createdBlocks = playerCreatedBlocks.get(player);
        HashSet<Player> playersHit = playerHits.get(player);

        System.out.println("inside of createFissurePillar");

        if (block.getType() == Material.TNT || block.isLiquid()
                || block.getType().toString().contains("BANNER")
                || block.getRelative(BlockFace.UP).getType().toString().contains("BANNER")
                || block.getType() == Material.ANVIL
                || block.getRelative(BlockFace.UP).getType() == Material.ANVIL) {
            return;
        }

        System.out.println("got past invalid block checks");

        int height = Math.min(3, blocksTraveled / 2 + 1);

        for (int i = 0; i < height; i++) {
            System.out.println("inside of height loop");
            Block up = block.getRelative(BlockFace.UP, i + 1);

            if (!UtilBlock.airFoliage(up)) break;

            up.setType(block.getType());
            player.getWorld().playEffect(up.getLocation(), Effect.STEP_SOUND, block.getType());

            createdBlocks.add(up);

            for (Player p : up.getWorld().getPlayers()) {
                System.out.println("checking for player at the block");
                if (up.getLocation().distance(p.getLocation()) < 1.5) {
                    if (!playersHit.contains(p)) {
                        System.out.println("added a non hit player to the hit players list and called method");
                        playersHit.add(p);
                        onPlayerCollision(p, blocksTraveled, player);
                    }
                }
            }
        }
    }


    public void removeFissure(Player player) {
        List<Block> createdBlocks = playerCreatedBlocks.get(player);
        if (createdBlocks != null) {
            for (Block block : createdBlocks) {
                System.out.println("replaced a fissure block with air");
                block.setType(Material.AIR);
            }
            createdBlocks.clear();
        }

        System.out.println("removed player from all maps");

        playerFissurePaths.remove(player);
        playerHits.remove(player);
        playerCreatedBlocks.remove(player);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig(){
        baseDamage = getConfig("baseDamage", 1.6, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.4, Double.class);
        fissureDistance = getConfig("fissureDistance", 14, Integer.class);
        fissureExpireDuration = getConfig("fissureExpireDuration", 10.0, Double.class);
        baseExtraDamagePerBlock = getConfig("baseExtraDamagePerBlock", 0.4, Double.class);
        baseExtraDamagePerBlockIncreasePerLevel = getConfig("baseExtraDamagePerBlockIncreasePerLevel", 0.2, Double.class);
        effectDuration = getConfig("effectDuration", 1, Integer.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 1, Integer.class);
        slownessLevel = getConfig("slownessLevel", 1, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
