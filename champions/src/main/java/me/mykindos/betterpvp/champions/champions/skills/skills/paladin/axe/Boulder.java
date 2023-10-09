package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.axe;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.paladin.data.BoulderObject;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
@BPvPListener
public class Boulder extends Skill implements Listener, InteractSkill, CooldownSkill {

    private final WeakHashMap<Player, List<BoulderObject>> boulders = new WeakHashMap<>();

    private double baseHeal;
    private double baseDamage;
    private double baseRadius;

    @Inject
    public Boulder(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Boulder";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Right click with an Axe to activate",
                "",
                "Throw a boulder forward that",
                "deals <val>" + getDamage(level) + "</val> damage to all nearby",
                "enemies and heals surrounding allies for",
                "<val>" + getHeal(level) + "</val> health in a radius of <val>" + getRadius(level) + "</val> blocks.",
                "",
                "Recharge: <val>" + getCooldown(level)
        };
    }

    private double getRadius(int level) {
        return baseRadius + 0.5 * (level - 1);
    }

    private double getDamage(int level) {
        return baseDamage + (level - 1);
    }

    private double getHeal(int level) {
        return baseHeal + (level - 1);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d);
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
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

        final BoulderObject boulder = new BoulderObject(champions, getHeal(level), getDamage(level), getRadius(level), clonedBlocks, this);
        boulder.spawn(player);

        boulders.computeIfAbsent(player, key -> new ArrayList<>()).add(boulder);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseHeal = getConfig("baseHeal", 2.0, Double.class);
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        baseRadius = getConfig("baseRadius", 4.0, Double.class);
    }

    @UpdateEvent
    public void collisionCheck() {
        final Iterator<Map.Entry<Player, List<BoulderObject>>> iterator = boulders.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BoulderObject>> entry = iterator.next();
            final Player caster = entry.getKey();

            final ListIterator<BoulderObject> boulderEntries = entry.getValue().listIterator();
            while (boulderEntries.hasNext()) {
                final BoulderObject boulder = boulderEntries.next();
                if (UtilTime.elapsed(boulder.getCastTime(), 10_000)) { // expire after 10 secs
                    boulderEntries.remove();
                    boulder.despawn();
                    continue;
                }

                final ArmorStand referenceEntity = boulder.getReferenceEntity();
                if (UtilBlock.isGrounded(referenceEntity) || referenceEntity.wouldCollideUsing(boulder.getBoundingBox())) {
                    boulder.impact(caster);
                    boulder.despawn();
                    boulderEntries.remove();
                }
            }

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

}
