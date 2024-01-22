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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
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
    private Map<Player, List<Block>> playerCreatedBlocks = new HashMap<>();
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
        playerCreatedBlocks.put(player, new ArrayList<>());

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

        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();

        Location playerLocation = player.getLocation();
        Location startLocation = playerLocation.add(direction).getBlock().getLocation().add(0.5, -1, 0.5);
        startLocation.setDirection(direction);

        int totalHeightChange = 0;
        Block lastBlock = null;

        double angle = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));
        angle = Math.abs(angle % 90);
        if (angle > 45) {
            angle = 90 - angle;
        }

        double cardinalFactor = 1.05;
        double diagonalFactor = Math.sqrt(2) + 0.05;
        double scalingFactor = cardinalFactor + (diagonalFactor - cardinalFactor) * (angle / 45.0);

        outerLoop:
        for (int i = 0; i < fissureDistance; i++) {
            Location locationToCheck = startLocation.clone().add(direction.clone().multiply(i * scalingFactor));
            //fix this
            locationToCheck.add(0, totalHeightChange, 0);

            Location locationAbove = locationToCheck.clone().add(0, 1, 0);
            int height = Math.min(3, i / 2 + 1);
            boolean blockFound = false;

            for (int j = 0; j < height + 1; j++) {
                Block currentBlock = locationToCheck.getBlock();

                if (currentBlock.equals(lastBlock)) {
                    continue;
                }

                if (isForbiddenBlockType(currentBlock.getType()) || currentBlock.getType().name().toLowerCase().contains("door") || currentBlock.getType().name().toLowerCase().contains("slab")) {
                    continue;
                }

                if (currentBlock.isSolid() && !UtilBlock.airFoliage(locationAbove.getBlock())) {
                    locationAbove.setY(locationToCheck.getY() + 2);
                    locationToCheck.add(0, 1, 0);
                    totalHeightChange++;
                } else if (!currentBlock.isSolid()) {
                    locationAbove.setY(locationToCheck.getY());
                    locationToCheck.add(0, -1, 0);
                    totalHeightChange--;
                } else if (currentBlock.isSolid() && UtilBlock.airFoliage(locationAbove.getBlock())) {
                    fissurePath.add(currentBlock);
                    lastBlock = currentBlock;
                    blockFound = true;
                    break;
                }
            }

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
        List<Block> createdBlocks = playerCreatedBlocks.get(player);
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
        List<Block> createdBlocks = playerCreatedBlocks.get(player);
        if (createdBlocks != null) {
            for (Block block : createdBlocks) {
                block.setType(Material.AIR);
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
