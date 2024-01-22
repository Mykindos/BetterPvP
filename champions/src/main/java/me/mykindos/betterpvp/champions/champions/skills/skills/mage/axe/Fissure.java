package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;


@Singleton
@BPvPListener
public class Fissure extends Skill implements InteractSkill, CooldownSkill, Listener {

    private int fissureDistance;
    private double fissureExpireDuration;
    private double damagePerBlock;
    private int effectDurationIncreasePerLevel;
    private double damagePerBlockIncreasePerLevel;
    private int effectDuration;
    private int slownessLevel;
    private Map<Player, ArrayList<Block>> playerFissurePaths = new HashMap<>();
    private Map<Player, HashSet<Player>> playerHits = new HashMap<>();
    private Map<Player, HashSet<Block>> playerCreatedBlocks = new HashMap<>();
    private Map<Player, BukkitRunnable> fissureExpirationTasks = new HashMap<>();


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
                "receive <effect>Slowness "+ UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> and <effect> Rooting </effect> for <val>" + getSlowDuration(level) + "</val> seconds",
                "and take <val>" + getDamage(level) + "</val> damage for",
                "every block fissure has travelled",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int blocksTraveled, int level){
        return (damagePerBlock + (level * damagePerBlockIncreasePerLevel)) * blocksTraveled;
    }

    public double getDamage(int level){
        return (damagePerBlock + (level * damagePerBlockIncreasePerLevel));
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

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You can only use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    public void activate(Player player, int level) {
        removeFissure(player);

        BukkitRunnable existingTask = fissureExpirationTasks.get(player);
        if (existingTask != null) {
            existingTask.cancel();
        }

        playerFissurePaths.put(player, new ArrayList<>());
        playerHits.put(player, new HashSet<>());
        playerCreatedBlocks.put(player, new HashSet<>());

        createFissure(player);

        BukkitRunnable newTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeFissure(player);
                fissureExpirationTasks.remove(player);
            }
        };
        newTask.runTaskLater(champions, (long) (fissureExpireDuration * 20));
        fissureExpirationTasks.put(player, newTask);
    }

    private void onEntityCollision(Entity entity, int blocksTraveled, Player player) {
        if (entity instanceof LivingEntity) {
            LivingEntity ent = (LivingEntity) entity;

            if (ent.equals(player)) {
                return;
            }

            int level = getLevel(player);

            double damage = getDamage(blocksTraveled, getLevel(player));
            ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getSlowDuration(level) * 20, slownessLevel));
            ent.damage(damage);

            if (ent instanceof Player) {
                Player target = (Player) ent;
                HashSet<Player> playersHit = playerHits.get(player);

                if (playersHit != null) {
                    playersHit.add(target);

                    UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + getName());
                    UtilMessage.simpleMessage(target, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName());
                }
            }
        }
    }

    public void createFissure(Player player) {
        ArrayList<Block> fissurePath = playerFissurePaths.get(player);

        //creates the player vector and normalizes it to get the players direction, ignoring Y
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();

        //Sets the startLocation to the block below the players feet and in the center of the block
        Location playerLocation = player.getLocation();
        Location startLocation = playerLocation.add(0, -1, 0);
        System.out.println("startLocation: "+startLocation);
        startLocation.setX((int)startLocation.getX());
        startLocation.setZ((int)startLocation.getZ());
        if(startLocation.getZ() >= 0){
            startLocation = playerLocation.add(0, 0, 0.5);
        } else if(startLocation.getZ() < 0) {
            startLocation = playerLocation.add(0, 0, -0.5);
        } if(startLocation.getX() >= 0) {
            startLocation = playerLocation.add(0.5, 0, 0);
        } else if(startLocation.getX() < 0){
            startLocation = playerLocation.add(-0.5, 0, 0);
        }
        System.out.println("startLocation2: "+startLocation);

        int totalHeightChange = 0;
        Block lastBlock = null;

        //determines an angle between 0 and 45 degrees to use in calculations
        double angle = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));
        angle = Math.abs(angle % 90);
        if (angle > 45) {
            angle = 90 - angle;
        }

        //interpolates between sqrt(2) and 1
        double cardinalFactor = 1.0;
        double diagonalFactor = Math.sqrt(2);
        double scalingFactor = cardinalFactor + (diagonalFactor - cardinalFactor) * (angle / 45.0);

        double previousDistanceChecked = 0;

        //goes through fissureDistance blocks
        outerLoop:
        for (int i = 0; i < fissureDistance; i++) {
            //determines the distance traveled per iteration and applies the totalHeightChange to the Y
            double newDistChecked = previousDistanceChecked += scalingFactor;
            System.out.println("angle: " + angle);
            System.out.println("Amount moved = " +scalingFactor);
            Location locationToCheck = startLocation.clone().add(direction.clone().multiply(newDistChecked));
            System.out.println("locationToCheck: " +locationToCheck);
            locationToCheck.add(0, totalHeightChange, 0);

            //defines locations above the curr block and determines the height of the fissure
            Location locationAbove = locationToCheck.clone().add(0, 1, 0);
            int height = Math.min(3, i / 2 + 1);
            boolean blockFound = false;

            if (lastBlock != null) {
                // Check for Z direction
                if (Math.abs(lastBlock.getZ() - (int) locationToCheck.getZ()) >= 2) {
                    System.out.println("skipped a block in Z direction");
                    System.out.println("firstLocation: " + locationToCheck);
                    Vector adjustZ = new Vector(0, 0, direction.getZ() * (-1 * (Math.sqrt(2) - 1)));
                    locationToCheck = locationToCheck.clone().add(adjustZ);
                    System.out.println("adjustedLocation: " + locationToCheck);
                    i--;
                }

                // Check for X direction
                if (Math.abs(lastBlock.getX() - (int) locationToCheck.getX()) >= 2) {
                    System.out.println("skipped a block in X direction");
                    System.out.println("firstLocation: " + locationToCheck);
                    Vector adjustX = new Vector(direction.getX() * (-1 * (Math.sqrt(2) - 1)), 0, 0);
                    locationToCheck = locationToCheck.clone().add(adjustX);
                    System.out.println("adjustedLocation: " + locationToCheck);
                    i--;
                }
            }


            //loops through height + 1
            for (int j = 0; j < height + 1; j++) {
                Block currentBlock = locationToCheck.getBlock();

                //continues if the block is the same as the last block passed
                if (currentBlock.equals(lastBlock)) {
                    continue;
                }

                //continues if its not an allowed block
                if (isForbiddenBlockType(currentBlock.getType()) || currentBlock.getType().name().toLowerCase().contains("door") || currentBlock.getType().name().toLowerCase().contains("slab")) {
                    continue;
                }

                //if the block is solid and has air above it, increment the y value of the whole fissure up 1
                if (currentBlock.isSolid() && !UtilBlock.airFoliage(locationAbove.getBlock())) {
                    locationAbove.setY(locationToCheck.getY() + 2);
                    locationToCheck.add(0, 1, 0);
                    totalHeightChange++;
                //if the block is not solid, increment the y value of the whole fissure down 1
                } else if (!currentBlock.isSolid()) {
                    locationAbove.setY(locationToCheck.getY());
                    locationToCheck.add(0, -1, 0);
                    totalHeightChange--;
                //if the block is solid and has air above it, it is a suitable block so add it
                } else if (currentBlock.isSolid() && UtilBlock.airFoliage(locationAbove.getBlock())) {
                    fissurePath.add(currentBlock);
                    System.out.println("placed a block at: " + currentBlock);
                    lastBlock = currentBlock;
                    blockFound = true;
                    break;
                }
            }
            //if no block is found height + 1 up/down from the original point stop the entire fissure
            if (!blockFound) {
                break outerLoop;
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

                createFissurePillar(currentBlock, blocksTraveled, player);

                index++;
            }
        }.runTaskTimer(champions, 0L, 2L);
    }


    private void createFissurePillar(Block block, int blocksTraveled, Player player) {
        HashSet<Block> createdBlocks = playerCreatedBlocks.get(player);
        HashSet<Player> playersHit = playerHits.get(player);
        int height = Math.min(3, blocksTraveled / 2 + 1);

        new BukkitRunnable() {
            private int currentHeight = 0;

            @Override
            public void run() {
                if (currentHeight >= height) {
                    this.cancel();
                    return;
                }

                Block up = block.getRelative(BlockFace.UP, currentHeight + 1);

                if (!UtilBlock.airFoliage(up) && up.getType() != Material.AIR) {
                    currentHeight++;
                    return;
                }

                Material blockMaterial = block.getType();
                if (currentHeight < height - 1) {
                    Block below = block.getRelative(BlockFace.DOWN, height - currentHeight - 1);
                    if (below.isSolid()) {
                        blockMaterial = below.getType();
                    }
                }

                up.setType(blockMaterial);
                player.getWorld().playEffect(up.getLocation(), Effect.STEP_SOUND, blockMaterial);
                createdBlocks.add(up);

                for (Entity entity : up.getWorld().getNearbyEntities(up.getLocation(), 1.5, 1.5, 1.5)) {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        if (!playersHit.contains(target)) {
                            onEntityCollision(target, blocksTraveled, player);
                        }
                    } else {
                        onEntityCollision(entity, blocksTraveled, player);
                    }
                }

                currentHeight++;
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    public void removeFissure(Player player) {
        HashSet<Block> createdBlocks = playerCreatedBlocks.get(player);
        if (createdBlocks != null) {
            for (Block block : createdBlocks) {
                Material blockMaterial = block.getType();
                block.setType(Material.AIR);
                player.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, blockMaterial);
            }
            createdBlocks.clear();
        }

        playerFissurePaths.remove(player);
        playerHits.remove(player);
        playerCreatedBlocks.remove(player);
    }

    private boolean isForbiddenBlockType(Material material) {
        Set<Material> forbiddenBlockTypes = EnumSet.of(
                Material.TNT,
                Material.ANVIL
        );

        return forbiddenBlockTypes.contains(material);
    }

    @EventHandler
    public void onFissureBlockBreak(BlockBreakEvent event) {
        for (Map.Entry<Player, HashSet<Block>> entry : playerCreatedBlocks.entrySet()) {
            if (entry.getValue().contains(event.getBlock())) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig(){
        fissureDistance = getConfig("fissureDistance", 14, Integer.class);
        fissureExpireDuration = getConfig("fissureExpireDuration", 10.0, Double.class);
        damagePerBlock = getConfig("baseExtraDamagePerBlock", 0.6, Double.class);
        damagePerBlockIncreasePerLevel = getConfig("baseExtraDamagePerBlockIncreasePerLevel", 0.2, Double.class);
        effectDuration = getConfig("effectDuration", 1, Integer.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 1, Integer.class);
        slownessLevel = getConfig("slownessLevel", 1, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
