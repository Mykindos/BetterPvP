package me.mykindos.betterpvp.champions.champions.skills.skills.gladiator.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import java.util.*;


@Singleton
@BPvPListener
public class BlockToss extends ChannelSkill implements InteractSkill, CooldownSkill {
    private final WeakHashMap<Player, Long> gap = new WeakHashMap<>();
    private final WeakHashMap<FallingBlock, Player> fallingBlocks = new WeakHashMap<>();
    private final WeakHashMap<FallingBlock, Boolean> activeBlocks = new WeakHashMap<>();
    private final WeakHashMap<FallingBlock, Set<Entity>> hasHit = new WeakHashMap<>();
    private final WeakHashMap<Player, BlockToss.BlockData> charging = new WeakHashMap<>();
    private final WorldBlockHandler blockHandler;
    private double duration;
    private double baseCharge;
    @Getter
    private double charge;
    private double baseDamage;

    @Inject
    public BlockToss(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }
    @Override
    public String getName() {
        return "Block Toss";
    }
    public Boolean checkTags(FallingBlock block, String s){
        for (String curr : block.getScoreboardTags()) {
            if (curr.equals(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Sword on a block to channel",
                "",
                "Charge a block to be thrown at the enemies dealing damage.",
                "",
                "Higher Charge time = Stronger Block and Farther Distance",
                "",
                "Cooldown: <val>" + getCooldown(level),
        };
    }
    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }
    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level*0.5);
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return false;
    }

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();
        if (player == null || !charging.containsKey(player) || !UtilPlayer.isHoldingItem(player, getItemsBySkillType())) {
            return null; // Skip if not online or not charging
        }

        final BlockToss.BlockData charge = charging.get(player);
        ProgressBar progressBar = ProgressBar.withProgress((float) charge.getCharge());
        return progressBar.build();
    });
    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;

    }
    private double getDamage(double charge, int level) {
        return (baseDamage+level)*charge;
    }
    private double getChargePerSecond(int level) {
        return (float) baseCharge + (15 * (level - 1)); // Increment of 15% per level
    }

    public void setCharge(double chargePercent){
        charge = chargePercent;
    }
    @Override
    public boolean canUse(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        return targetBlock != null && targetBlock.getBlockData().getMaterial() != Material.COBWEB;
    }
    @Override
    public void trackPlayer(Player player) {
        // Action bar
        final Optional<Gamer> gamerOpt = championsManager.getGamers().getObject(player.getUniqueId());
        gamerOpt.ifPresent(gamer -> gamer.getActionBar().add(900, actionBarComponent));
    }

    @Override
    public void invalidatePlayer(Player player) {
        // Action bar
        final Optional<Gamer> gamerOpt = championsManager.getGamers().getObject(player.getUniqueId());
        gamerOpt.ifPresent(gamer -> gamer.getActionBar().remove(actionBarComponent));
    }

    @Override
    public void activate(Player player, int level) {
        Block targetBlock = player.getTargetBlockExact(5);
        charging.put(player, new BlockToss.BlockData(level));
        gap.put(player, System.currentTimeMillis());
        FallingBlock currBlock = (player.getWorld().spawnFallingBlock(player.getEyeLocation().add(0, -0.1, 0), targetBlock.getBlockData()));
        ArmorStand temp = (player.getWorld().spawn(player.getLocation().add(0, 0.6, 0), ArmorStand.class));

        temp.setInvisible(true);
        temp.setSmall(true);
        temp.setInvulnerable(true);
        temp.addPassenger(currBlock);
        currBlock.setGravity(false);
        currBlock.setDropItem(false);

        fallingBlocks.put(currBlock, player);
        activeBlocks.put(currBlock, true); // TRUE == CURRENTLY HELD BY PLAYER
        Set<Entity> tempSet = new HashSet<>();
        tempSet.add(currBlock); // just so the set doesn't return null when called
        hasHit.put(currBlock, tempSet);

    }

    private void thrownBlock(Player player, double charge, FallingBlock block) {
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1f);
        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + "</alt>.");
        championsManager.getCooldowns().use(player, getName(), getCooldown(getLevel(player)), true, true, isCancellable(), gmr -> gmr.getPlayer() != null && UtilPlayer.isHoldingItem(gmr.getPlayer(), getItemsBySkillType()));
        block.setVelocity(player.getLocation().getDirection().multiply(0.6 + charge));
        block.setGravity(true);
        block.setCancelDrop(true);
        block.setDropItem(false);
    }

    private FallingBlock findActiveBlock(Player player) {
        for (HashMap.Entry<FallingBlock, Player> entry : fallingBlocks.entrySet()) {
            if (entry.getValue().equals(player)) {
                for (HashMap.Entry<FallingBlock, Boolean> entry2 : activeBlocks.entrySet()) {
                    if ((entry2.getKey().equals(entry.getKey()) && (entry2.getValue()))) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    @UpdateEvent
    public void deadBlock(){
        try {
            if (!fallingBlocks.isEmpty()) {
                Iterator<HashMap.Entry<FallingBlock, Player>> iterator = fallingBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry<FallingBlock, Player> entry = iterator.next();
                    FallingBlock currBlock = entry.getKey();
                    if (currBlock.isDead()) {
                        //QOL CHANGE FOR MORE CONVENIENT BLOCK TOSSES
                        if (currBlock.getLocation().subtract(0, 1, 0).getBlock().getBlockData().getMaterial() == Material.AIR && !checkTags(currBlock, "replaced")) {
                            fallingBlocks.remove(currBlock);
                            activeBlocks.remove(currBlock);
                            hasHit.remove(currBlock);
                            Player player = entry.getValue();
                            Vector blockVelocity = currBlock.getVelocity();
                            Location messedBlock = currBlock.getLocation();
                            currBlock = currBlock.getWorld().spawnFallingBlock(currBlock.getLocation().add(0, 0.1, 0), currBlock.getBlockData());
                            currBlock.setVelocity(blockVelocity);
                            currBlock.setDropItem(false);
                            currBlock.setCancelDrop(true);
                            currBlock.addScoreboardTag("replaced");
                            currBlock.getWorld().getBlockAt(messedBlock).setType(Material.AIR);
                            Set<Entity> tempSet = new HashSet<>();
                            tempSet.add(currBlock); // just so the set doesn't return null when called
                            hasHit.put(currBlock,tempSet);
                            fallingBlocks.put(currBlock, player);
                            activeBlocks.put(currBlock, false);
                            iterator.remove();
                        }
                        if (currBlock.getLocation().getBlock().getType() != Material.WATER) {
                            blockHandler.addRestoreBlock(currBlock.getLocation().getBlock(), currBlock.getBlockData().getMaterial(), (long) (duration * 1000));
                            currBlock.getLocation().getBlock().setType(currBlock.getBlockData().getMaterial());
                        }
                        fallingBlocks.remove(currBlock);
                        activeBlocks.remove(currBlock);
                        hasHit.remove(currBlock);
                    }
                }
            }
        } catch(Exception ignored) {
        }
    }

    @UpdateEvent
    public void contactBlock(){
        if (!fallingBlocks.isEmpty()) {
            for (HashMap.Entry<FallingBlock, Player> entry : fallingBlocks.entrySet()) {
                FallingBlock currBlock = entry.getKey();
                Player player = entry.getValue();
                if (player != null) {
                    if (currBlock != null) {
                        List<Entity> test = currBlock.getNearbyEntities(0.55, 0.55, 0.55);
                        for (Entity entity : test) {
                            if (!entity.equals(player) && entity instanceof LivingEntity) {
                                if (currBlock != findActiveBlock(player)) {
                                    if (!hasHit.get(currBlock).contains(entity)) {
                                        UtilDamage.doCustomDamage(new CustomDamageEvent((LivingEntity) entity, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(getCharge(), getLevel(player)), true, getName()));
                                        hasHit.get(currBlock).add(entity);
                                        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        try {
            if (!fallingBlocks.isEmpty()) {
                Iterator<HashMap.Entry<FallingBlock, Player>> iterator = fallingBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry<FallingBlock, Player> entry = iterator.next();
                    Player player = entry.getValue();
                    if (player != null) {
                        if (player.isHandRaised()) {
                            if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                                iterator.remove();
                            }
                            findActiveBlock(player).getVehicle().setVelocity(player.getLocation().add(0, 0.6, 0).toVector().subtract(findActiveBlock(player).getVehicle().getLocation().toVector()).multiply(0.5));
                        } else {
                            if (gap.containsKey(player)) {
                                findActiveBlock(player).getVehicle().remove();
                                findActiveBlock(player).remove();
                                if (UtilTime.elapsed(gap.get(player), 250)) {
                                    gap.remove(player);
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            }
              } catch(Exception ignored) {
    }
    }

    private void showCharge(Player player, BlockToss.BlockData charge) {
        // Sound
        if (!UtilTime.elapsed(charge.getLastSound(), 150)) {
            return;
        }
        player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f + (0.5f * (float) charge.getCharge()));
        charge.setLastSound(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void cancelCooldown(PlayerUseSkillEvent event) {
        if (event.getSkill() == this && charging.containsKey(event.getPlayer() )) {
            event.setCancelled(true); // Cancel cooldown or ability use if they're charging to allow them to release
        }
    }


    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
                BlockToss.BlockData charge = charging.get(player);
                if (player != null) {
                    int level = getLevel(player);
                    // Remove if they no longer have the skill
                    if (level <= 0) {
                        iterator.remove();
                        continue;
                    }
                    //Remove if they don't have a block
                    if (findActiveBlock(player)==null) {
                        iterator.remove();
                        continue;
                    }
                    // Check if they still are blocking and charge
                    if (player.isHandRaised()) {
                        championsManager.getCooldowns().removeCooldown(player, getName(), true);
                        // Check for sword hold status
                        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                            findActiveBlock(player).getVehicle().remove();
                            findActiveBlock(player).remove();
                            iterator.remove(); // Otherwise, remove
                        }
                        charge.tick();
                        // Cues
                        showCharge(player, charge);
                        continue;
                    }
                    if (UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        setCharge(charge.getCharge());
                        findActiveBlock(player).getVehicle().remove();
                        thrownBlock(player, (float) charge.getCharge(), findActiveBlock(player));
                        activeBlocks.put(findActiveBlock(player), false);
                        charging.remove(player);

                    }
            }
        }
    }

    @Override
    public void loadSkillConfig(){
        baseCharge = getConfig("baseCharge", 60.0, Double.class);
        baseDamage = getConfig("baseDamage", 8.0, Double.class);
        duration = getConfig("duration", 4.0, Double.class);

    }
    @Data
    private class BlockData {

        private long lastSound = 0;
        private long lastMessage = 0;
        private double charge = 0; // 0 -> 1
        private final int level;

        public void tick() {
            // Divide over 100 to get multiplication factor since it's in 100% scale for display
            final double chargeToGive = getChargePerSecond(level) / 100;
            this.charge = Math.min(1, this.charge + (chargeToGive / 20));
        }
    }
}
