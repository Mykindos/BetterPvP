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
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
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
import java.util.stream.Collectors;


@Singleton
@BPvPListener
public class Fissure extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WorldBlockHandler blockHandler;
    private int fissureDistance;
    private double fissureExpireDuration;
    private double damagePerBlock;
    private int effectDurationIncreasePerLevel;
    private double damagePerBlockIncreasePerLevel;
    private int effectDuration;
    private int slownessLevel;
    private final Map<Player, ArrayList<Block>> playerFissurePaths = new WeakHashMap<>();
    private final Map<Player, HashSet<Player>> playerHits = new WeakHashMap<>();

    @Inject
    public Fissure(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
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
                "<val>" + (Math.round(getDamage(level) * 10) / 10.0) + "</val> damage for every block fissure",
                "has travelled",
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
        createFissurePath(player);
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

            int currY = lastSuitableY;

            boolean suitableBlockFound = false;
            int passes = 0;
            int height = Math.min(3, i / 2 + 1);

            while (!suitableBlockFound && passes <= height + 1) {
                Block checkBlock = currentLocation.getWorld().getBlockAt(currentLocation.getBlockX(), currY, currentLocation.getBlockZ());

                if (!UtilBlock.airFoliage(checkBlock) && UtilBlock.airFoliage(checkBlock.getRelative(BlockFace.UP))) {
                    suitableBlockFound = true;
                    lastSuitableY = checkBlock.getY();
                    boolean isBlockAlreadyAdded = false;
                    for (Block block : pathBlocks) {
                        if (block.getLocation().equals(checkBlock.getLocation())) {
                            isBlockAlreadyAdded = true;
                            break;
                        }
                    }
                    if (!isBlockAlreadyAdded) {
                        pathBlocks.add(checkBlock);
                    } else {
                        i--;
                        break;
                    }
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

                        entity.getWorld().playEffect(entity.getLocation(), Effect.STEP_SOUND, block.getType());
                        entity.getWorld().playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, block.getType());

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

                    playerFissurePaths.remove(player);
                    playerHits.remove(player);

                    this.cancel();
                    return;
                }

                Block baseBlock = pathBlocks.get(currentIndex);
                if (isForbiddenBlockType(baseBlock.getType())) {
                    prepareForNextColumn();
                    return;
                }

                Block blockToPlace = baseBlock.getRelative(BlockFace.UP, currentHeight);

                if (UtilBlock.airFoliage(blockToPlace)) {
                    Material materialToSet = determineMaterialToSet(baseBlock, currentHeight, totalHeight);
                    blockHandler.addRestoreBlock(blockToPlace, materialToSet, (long)(fissureExpireDuration * 1000));
                    player.getWorld().playEffect(blockToPlace.getLocation(), Effect.STEP_SOUND, materialToSet);

                    checkForEntityCollisions(blockToPlace, currentIndex, player);
                }

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

    private Set<Material> forbiddenBlockTypes;
    private boolean gateCheckEnabled;
    private boolean doorCheckEnabled;



    private boolean isForbiddenBlockType(Material material) {
        boolean isForbidden = forbiddenBlockTypes.contains(material);
        String materialName = material.name().toUpperCase();

        boolean doorGateCheck = (doorCheckEnabled && materialName.contains("DOOR")) || (gateCheckEnabled && materialName.contains("GATE"));

        return isForbidden || doorGateCheck;
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

        List<String> forbiddenBlocksList = getConfig("fissureForbiddenBlocks", new ArrayList<String>(), List.class);
        forbiddenBlockTypes = forbiddenBlocksList.stream()
                .map(String::toUpperCase)
                .map(Material::valueOf)
                .collect(Collectors.toSet());

        gateCheckEnabled = getConfig("gateCheckEnabled", true, Boolean.class);
        doorCheckEnabled = getConfig("doorCheckEnabled", true, Boolean.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
