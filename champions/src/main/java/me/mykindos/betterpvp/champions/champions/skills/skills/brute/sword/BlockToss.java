package me.mykindos.betterpvp.champions.champions.skills.skills.brute.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.BlockTossObject;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BlockToss extends ChannelSkill implements Listener, InteractSkill, CooldownSkill, DamageSkill {

    private final WeakHashMap<Player, BoulderChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, List<BlockTossObject>> boulders = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this,
            charging,
            gamer -> true);

    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseSpeed;
    private double speedIncreasePerLevel;
    private double size;
    private double sizePerLevel;
    private double hitBoxSize;

    @Inject
    public BlockToss(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Block Toss";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Hold your Sword to activate",
                "",
                "Throw a boulder forward that",
                "deals " + getValueString(this::getDamage, level) + " damage to all nearby",
                "enemies.",
                "",
                "Boulder size increases at a rate",
                "of " + getValueString(this::getChargePerSecond, level) + " per level.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    private double getRadius(int level) {
        return baseRadius + radiusIncreasePerLevel * (level - 1);
    }

    private double getDamage(int level) {
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    private double getSpeed(int level) {
        return baseSpeed + (level - 1) * speedIncreasePerLevel;
    }

    private double getSize(int level) {
        return level * sizePerLevel;
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (level - 1) * chargeIncreasePerLevel;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    @Override
    public void activate(Player player, int level) {
        final Location feetLocation = player.getLocation();

        // Clone the blocks under the player to add realism
        final List<BlockData> clonedBlocks = new ArrayList<>();
        for (double x = -baseRadius; x < baseRadius; x++) {
            for (double z = -baseRadius; z < baseRadius; z++) {
                final Block block = feetLocation.clone().add(x, -1.0, z).getBlock();
                if (UtilBlock.solid(block)) {
                    clonedBlocks.add(block.getBlockData());
                }
            }
        }

        if (clonedBlocks.isEmpty()) {
            clonedBlocks.add(Bukkit.createBlockData(Material.DIRT));
            clonedBlocks.add(Bukkit.createBlockData(Material.COBBLESTONE));
            clonedBlocks.add(Bukkit.createBlockData(Material.STONE));
        }

        if (charging.containsKey(player)) {
            return;
        }

        final BlockTossObject boulder = new BlockTossObject(clonedBlocks, this, player);
        boulder.spawn(size);

        final BoulderChargeData chargeData = new BoulderChargeData((float) getChargePerSecond(level) / 100, boulder);
        charging.put(player, chargeData);
        boulders.computeIfAbsent(player, key -> new ArrayList<>()).add(boulder);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseCharge = getConfig("baseCharge", 55.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 15.0, Double.class);
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 2.0, Double.class);
        baseRadius = getConfig("baseRadius", 4.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        baseSpeed = getConfig("baseSpeed", 1.4, Double.class);
        speedIncreasePerLevel = getConfig("speedIncreasePerLevel", 0.1, Double.class);
        size = getConfig("size", 0.6, Double.class);
        sizePerLevel = getConfig("sizePerLevel", 0.2, Double.class);
        hitBoxSize = getConfig("hitBoxSize", 1.0, Double.class);
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            BoulderChargeData chargeData = charging.get(player);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            if (isHolding(player) && gamer.isHoldingRightClick()) {
                chargeData.tick();
                chargeData.tickSound(player);

                if (chargeData.getCharge() < 1) {
                    chargeData.boulder.setSize(chargeData.boulder.getSize() + getSize(level) / 20);
                }
                continue;
            }

            iterator.remove();
            final float charge = chargeData.getCharge(); // 0 - 1
            chargeData.boulder.throwBoulder(getSpeed(level) * charge, getRadius(level), getDamage(level) * charge);
            championsManager.getCooldowns().removeCooldown(player, getName(), true);
            championsManager.getCooldowns().use(player,
                    getName(),
                    getCooldown(level),
                    showCooldownFinished(),
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player player)) {
            return;
        }

        final List<BlockTossObject> boulderList = boulders.get(player);
        if (boulderList == null) {
            return;
        }

        for (BlockTossObject boulder : boulderList) {
            if (arrow.equals(boulder.getReferenceEntity())) {
                boulder.impact(player);
                break;
            }
        }

        event.setCancelled(true);
        arrow.remove();
    }

    @UpdateEvent
    public void collisionCheck() {
        final Iterator<Map.Entry<Player, List<BlockTossObject>>> iterator = boulders.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BlockTossObject>> entry = iterator.next();
            final Player caster = entry.getKey();

            final ListIterator<BlockTossObject> boulderEntries = entry.getValue().listIterator();
            while (boulderEntries.hasNext()) {
                final BlockTossObject boulder = boulderEntries.next();
                if (boulder.isThrown() && UtilTime.elapsed(boulder.getCastTime(), 10_000)) { // expire after 10 secs
                    boulderEntries.remove();
                    boulder.despawn();
                    continue;
                }

                final Arrow referenceEntity = boulder.getReferenceEntity();
                if (boulder.isImpacted()) {
                    if (boulder.getImpactTicks() > 30) {
                        boulder.despawn();
                        boulderEntries.remove();
                        continue;
                    }
                } else if (boulder.isThrown()) {
                    final List<Entity> nearby = referenceEntity.getNearbyEntities(hitBoxSize, hitBoxSize, hitBoxSize);
                    nearby.remove(caster);
                    nearby.removeIf(entity -> !(entity instanceof LivingEntity) || entity instanceof ArmorStand);
                    if (!nearby.isEmpty() || !referenceEntity.getLocation().getBlock().isPassable()) {
                        boulder.impact(caster);
                    }
                }

                boulder.tick();
            }

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Getter
    private static class BoulderChargeData extends ChargeData {

        private final BlockTossObject boulder;

        public BoulderChargeData(float chargePerSecond, BlockTossObject boulder) {
            super(chargePerSecond);
            this.boulder = boulder;
        }
    }

}
