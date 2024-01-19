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
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
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
                "receive <effect>Slowness "+ UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> for <val>" + getSlowDuration(level) + "</val> seconds",
                "and take <val>" + getDamage(level) + "</val> damage plus an",
                "additional <val>" + getExtraDamage(level) + "</val> damage for",
                "every block fissure has travelled",
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

            int level = getLevel(player);

            double damage = getDamage(getLevel(player)) + getExtraDamage(blocksTraveled, getLevel(player));
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

        Location startLocation = player.getLocation().add(direction);

        for (int i = 0; i < fissureDistance; i++) {
            Location locationToCheck = startLocation.clone().add(direction.clone().multiply(i));

            // Round the X and Z coordinates to ensure we're working with distinct block coordinates
            int blockX = locationToCheck.getBlockX();
            int blockZ = locationToCheck.getBlockZ();
            locationToCheck.setX(blockX);
            locationToCheck.setZ(blockZ);

            int height = Math.min(3, i / 2 + 1);

            for (int j = 0; j < height; j++) {
                locationToCheck.setY(locationToCheck.getY() - 1);
                Block currentBlock = locationToCheck.getBlock();

                if (UtilBlock.solid(currentBlock)) {
                    if (!fissurePath.contains(currentBlock)) {
                        fissurePath.add(currentBlock);
                    }
                    break;
                }
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

        if (block.getType() == Material.TNT || block.isLiquid()
                || block.getType().toString().contains("BANNER")
                || block.getRelative(BlockFace.UP).getType().toString().contains("BANNER")
                || block.getType() == Material.ANVIL
                || block.getRelative(BlockFace.UP).getType() == Material.ANVIL) {
            return;
        }

        int height = Math.min(3, blocksTraveled / 2 + 1);

        for (int i = 0; i < height; i++) {
            Block up = block.getRelative(BlockFace.UP, i + 1);

            if (!UtilBlock.airFoliage(up) && up.getType() != Material.AIR) {
                continue;
            }

            up.setType(block.getType());
            player.getWorld().playEffect(up.getLocation(), Effect.STEP_SOUND, block.getType());
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
        }
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
