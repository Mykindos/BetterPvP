package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class ArcticArmour extends ActiveToggleSkill implements EnergySkill {

    private final WorldBlockHandler blockHandler;

    private int baseRadius;
    private int radiusIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;

    private int resistanceStrength;

    private int slownessStrength;

    @Inject
    public ArcticArmour(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Arctic Armour";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Create a freezing area around",
                "you in a <val>" + getRadius(level )+ "</val> Block radius",
                "",
                "Allies inside this area receive <effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength + 1) + "</effect>, and",
                "enemies inside this area receive <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect>",
                "",
                "Energy / Second: <val>" + getEnergy(level)
        };
    }

    public int getRadius(int level) {
        return baseRadius + level * radiusIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player cur = Bukkit.getPlayer(uuid);
            if (cur != null) {
                cur.getWorld().playSound(cur.getLocation(), Sound.WEATHER_RAIN, 0.3F, 0.0F);
            }
        }
    }

    @UpdateEvent(delay = 125)
    public void snowAura() {

        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                int level = getLevel(player);

                if (level <= 0) {
                    iterator.remove();
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 2, true)) {
                    iterator.remove();

                } else if (championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                    iterator.remove();
                } else {

                    int distance = getRadius(level);
                    HashMap<Block, Double> blocks = UtilBlock.getInRadius(player.getLocation(), distance);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, resistanceStrength));
                    championsManager.getEffects().addEffect(player, EffectType.RESISTANCE, resistanceStrength + 1, 1000);

                    for (var data : UtilPlayer.getNearbyPlayers(player, distance)) {
                        Player target = data.getKey();
                        boolean friendly = data.getValue() == EntityProperty.FRIENDLY;
                        if (friendly) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, resistanceStrength));
                            championsManager.getEffects().addEffect(target, EffectType.RESISTANCE, resistanceStrength + 1, 1000);
                        } else {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, slownessStrength));
                        }
                    }

                    if (UtilBlock.isGrounded(player)) {
                        for (Block block : blocks.keySet()) {
                            if (block.getLocation().getY() <= player.getLocation().getY()) {
                                Block relDown = block.getRelative(BlockFace.DOWN);
                                if (relDown.getType() != Material.SNOW && relDown.getType() != Material.AIR
                                        && UtilBlock.shouldPlaceSnowOn(relDown)) {
                                    if (block.getType() == Material.AIR || block.getType() == Material.SNOW) {
                                        blockHandler.addRestoreBlock(block, Material.SNOW, (long) getDuration(level) * 1000);
                                    }

                                }
                            }
                        }
                    }
                }
            } else {
                iterator.remove();
            }
        }

    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }


    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: <red>Off");
        } else {
            active.add(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: <green>On");
        }
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 2, Integer.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1, Integer.class);
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);

        resistanceStrength = getConfig("resistanceStrength", 0, Integer.class);
        slownessStrength = getConfig("slownessStrength", 0, Integer.class);
    }
}
