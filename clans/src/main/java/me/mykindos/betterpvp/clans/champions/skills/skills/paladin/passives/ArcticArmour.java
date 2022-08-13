package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private int minRadius;
    private double duration;

    @Inject
    public ArcticArmour(Clans clans, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(clans, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Arctic Armour";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop Axe/Sword to Toggle.",
                "",
                "Create a freezing area around you",
                "in a " + ChatColor.GREEN + (minRadius + level) + ChatColor.GRAY + " Block radius. Allies inside",
                "this area receive Protection I.",
                "Enemies inside this area receive",
                "Slowness I",
                "",
                "Energy / Second: " + ChatColor.GREEN + getEnergy(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
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
    public void SnowAura() {

        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player cur = Bukkit.getPlayer(uuid);
            if (cur != null) {

                int level = getLevel(cur);

                if (level <= 0) {
                    iterator.remove();
                } else if (!championsManager.getEnergy().use(cur, getName(), getEnergy(level) / 2, true)) {
                    iterator.remove();

                } else if (championsManager.getEffects().hasEffect(cur, EffectType.SILENCE)) {
                    iterator.remove();
                } else {

                    int distance = (minRadius + level);
                    HashMap<Block, Double> blocks = UtilBlock.getInRadius(cur.getLocation(), distance);

                    // TODO revisit this

                    for (var data : UtilPlayer.getNearbyPlayers(cur, distance)) {
                        Player target = data.getKey();
                        boolean friendly = data.getValue() == EntityProperty.FRIENDLY;
                        if (friendly) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, 0));
                            championsManager.getEffects().addEffect(target, EffectType.RESISTANCE, 1, 1000);
                        } else {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 0));
                        }
                    }

                    if (UtilBlock.isGrounded(cur)) {
                        for (Block block : blocks.keySet()) {
                            if (block.getLocation().getY() <= cur.getLocation().getY()) {
                                Block relDown = block.getRelative(BlockFace.DOWN);
                                if (relDown.getType() != Material.SNOW && relDown.getType() != Material.AIR
                                        && UtilBlock.shouldPlaceSnowOn(relDown)) {
                                    if (block.getType() == Material.AIR || block.getType() == Material.SNOW) {
                                        blockHandler.addRestoreBlock(block, Material.SNOW, (long) duration * 1000);
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

        return (float) (energy - ((level - 1) * 0.5));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: " + ChatColor.RED + "Off");
        } else {
            active.add(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: " + ChatColor.GREEN + "On");
        }
    }

    @Override
    public void loadSkillConfig() {
        minRadius = getConfig("minRadius", 2, Integer.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
