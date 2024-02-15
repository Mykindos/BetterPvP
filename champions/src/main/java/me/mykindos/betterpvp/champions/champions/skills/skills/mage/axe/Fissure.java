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
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
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
    private final Map<Player, ArrayList<Block>> playerFissurePaths = new HashMap<>();
    private final Map<Player, HashSet<Player>> playerHits = new HashMap<>();
    private final Map<Player, HashSet<Block>> playerCreatedBlocks = new HashMap<>();
    private final Map<Player, Integer> fissureExpirationTasks = new HashMap<>();


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
                "Players struck by wall will receive",
                "<effect>Slowness "+ UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> for <val>" + getSlowDuration(level) + "</val> seconds and take",
                "<val>" + getDamage(level) + "</val> damage for every block fissure",
                "fissure has travelled",
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

        if (fissureExpirationTasks.containsKey(player)) {
            int taskId = fissureExpirationTasks.get(player);
            Bukkit.getScheduler().cancelTask(taskId);
            fissureExpirationTasks.remove(player);
        }

        removeFissure(player);
        createFissurePath(player);

        int newTaskId = Bukkit.getScheduler().runTaskLater(champions, () -> {
            removeFissure(player);
            fissureExpirationTasks.remove(player);
        }, (long)(fissureExpireDuration * 20)).getTaskId();

        fissureExpirationTasks.put(player, newTaskId);
    }


    public void createFissurePath(Player player) {
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection();
        direction.setY(0);
        direction.normalize();

        Location currentLocation = playerLocation.clone().add(0, -1, 0);
        ArrayList<Block> pathBlocks = new ArrayList<>();

        int lastSuitableY = currentLocation.getBlockY() - 1;

        for (int i = 0; i < fissureDistance; i++) {
            currentLocation.add(direction);
            System.out.println("currentDistance:" + (i+1));

            int currY = lastSuitableY;

            boolean suitableBlockFound = false;
            int passes = 0;
            int height = Math.min(3, i / 2 + 1);

            while (!suitableBlockFound && passes <= height + 1) {
                Block checkBlock = currentLocation.getWorld().getBlockAt(currentLocation.getBlockX(), currY, currentLocation.getBlockZ());

                if (!UtilBlock.airFoliage(checkBlock) && UtilBlock.airFoliage(checkBlock.getRelative(BlockFace.UP))) {
                    pathBlocks.add(checkBlock);
                    suitableBlockFound = true;
                    lastSuitableY = checkBlock.getY();
                } else if (!UtilBlock.airFoliage(checkBlock) && !UtilBlock.airFoliage(checkBlock.getRelative(BlockFace.UP))) {
                    currY++;
                    if (lastSuitableY - currY > (height + 1)) {
                        break;
                    }
                }
                else if(UtilBlock.airFoliage(checkBlock)){
                    currY--;
                    if (lastSuitableY - currY < (-1 * (height + 1))) {
                        break;
                    }
                }
                passes++;
            }

            if (!suitableBlockFound) {
                break;
            }
        }

        if (!pathBlocks.isEmpty()) {
            for (Block block : pathBlocks) {
                player.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

                List<KeyValue<LivingEntity, EntityProperty>> nearbyEntities = UtilEntity.getNearbyEntities(player, block.getLocation().add(0, 1, 0), 1.5, EntityProperty.ENEMY);

                for (KeyValue<LivingEntity, EntityProperty> keyValue : nearbyEntities) {
                    LivingEntity entity = keyValue.getKey();
                    if (entity.getLocation().getBlockY() == block.getY() + 1 || entity.getLocation().getBlock().equals(block)) {
                        int level = getLevel(player);
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getSlowDuration(level) * 20, slownessLevel));
                    }
                }
            }
            createFissurePillars(pathBlocks, player);
        }

        playerFissurePaths.put(player, pathBlocks);
    }


    private void createFissurePillars(List<Block> pathBlocks, Player player) {
        new BukkitRunnable() {
            int currentIndex = 0;
            int currentHeight = 1;
            int totalHeight = Math.min(3, currentIndex / 2 + 1);

            @Override
            public void run() {
                if (currentIndex >= pathBlocks.size()) {
                    this.cancel();
                    return;
                }

                Block baseBlock = pathBlocks.get(currentIndex);
                if (isForbiddenBlockType(baseBlock.getType())) {
                    prepareForNextColumn();
                    return;
                }

                Block blockToPlace = baseBlock.getRelative(BlockFace.UP, currentHeight);
                Material materialToSet = determineMaterialToSet(baseBlock, currentHeight, totalHeight);
                blockToPlace.setType(materialToSet, false);
                player.getWorld().playEffect(blockToPlace.getLocation(), Effect.STEP_SOUND, materialToSet);

                checkForEntityCollisions(blockToPlace, currentIndex, player);

                playerCreatedBlocks.computeIfAbsent(player, k -> new HashSet<>()).add(blockToPlace);

                if (currentHeight >= totalHeight) {
                    prepareForNextColumn();
                } else {
                    currentHeight++;
                }
            }

            private void prepareForNextColumn() {
                currentIndex++;
                currentHeight = 1;
                totalHeight = Math.min(3, currentIndex / 2 + 1);
            }

            private Material determineMaterialToSet(Block baseBlock, int currentHeight, int totalHeight) {
                int depthBelow = totalHeight - currentHeight;
                Block blockBelow = baseBlock.getRelative(BlockFace.DOWN, depthBelow);
                Material materialBelow = blockBelow.getType();
                if (!isForbiddenBlockType(materialBelow) && !UtilBlock.airFoliage(blockBelow)) {
                    return materialBelow;
                } else {
                    return baseBlock.getType();
                }
            }

            private void checkForEntityCollisions(Block blockToPlace, int currentIndex, Player player) {
                List<KeyValue<LivingEntity, EntityProperty>> entities = UtilEntity.getNearbyEntities(player, blockToPlace.getLocation(), 1.0, EntityProperty.ENEMY);
                for (KeyValue<LivingEntity, EntityProperty> keyValue : entities) {
                    LivingEntity entity = keyValue.getKey();
                    onEntityCollision(entity, currentIndex, player);
                }
            }
        }.runTaskTimer(champions, 0L, 1L);
    }



    private void onEntityCollision(Entity entity, int blocksTraveled, Player player) {

        if (!(entity instanceof LivingEntity) || entity.equals(player)) {
            return;
        }

        LivingEntity ent = (LivingEntity) entity;

        playerHits.putIfAbsent(player, new HashSet<>());
        HashSet<Player> playersHit = playerHits.get(player);

        if (ent instanceof Player && !playersHit.contains(ent)) {

            int level = getLevel(player);
            double damage = getDamage(blocksTraveled, level);
            ent.damage(damage);

            playersHit.add((Player) ent);

            UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + ent.getName() + "</alt2> with <alt>" + getName());
            UtilMessage.simpleMessage(ent, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName());
        }
    }


    public void removeFissure(Player player) {
        HashSet<Block> createdBlocks = playerCreatedBlocks.get(player);
        if (createdBlocks != null) {
            for (Block block : createdBlocks) {
                Material blockMaterial = block.getType();
                if(block.getType() != Material.AIR){
                block.setType(Material.AIR);
                player.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, blockMaterial);
                }
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
                Material.ANVIL,
                Material.ENCHANTING_TABLE
        );

        if (forbiddenBlockTypes.contains(material)) {
            return true;
        }

        String materialName = material.name().toUpperCase();

        if (materialName.contains("DOOR") || materialName.contains("GATE")) {
            return true;
        }

        return false;
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