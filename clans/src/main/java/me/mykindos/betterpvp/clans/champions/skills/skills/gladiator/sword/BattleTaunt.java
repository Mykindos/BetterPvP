package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.sword;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class BattleTaunt extends ChannelSkill implements InteractSkill, CooldownSkill, EnergySkill, Listener {

    @Inject
    public BattleTaunt(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Battle Taunt";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{"Hold Block with a sword to Channel.",
                "",
                "While channelling, any enemies within " + ChatColor.GREEN + (2 + level) + ChatColor.GRAY + " blocks",
                "are slowly pulled in towards you",
                "",
                "Energy / Second: " + ChatColor.GREEN + getEnergy(level)};
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @UpdateEvent
    public void Energy() {
        Iterator<UUID> activeIterator = active.iterator();
        while (activeIterator.hasNext()) {
            UUID uuid = activeIterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (player.isHandRaised()) {
                    if (!championsManager.getEnergy().use(player, getName(), getEnergy(getLevel(player)) / 2, true)) {
                        activeIterator.remove();
                    } else if (!hasSkill(player)) {
                        activeIterator.remove();
                    } else if (!player.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
                        activeIterator.remove();
                    } else if (UtilBlock.isInLiquid(player)) {
                        activeIterator.remove();
                    } else {

                        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.DIAMOND_BLOCK);

                        for (int i = 0; i <= (2 + getLevel(player)); i++) {
                            pull(player, player.getEyeLocation().add(player.getLocation().getDirection().multiply(i)));
                        }
                    }
                }
            } else {
                activeIterator.remove();
            }
        }


    }

    private void pull(Player player, Location location) {
        for (LivingEntity other : UtilEntity.getNearbyEntities(player, location, 2.0)) {
            if (other instanceof Player target) {

                if (UtilMath.offset(player.getLocation(), other.getLocation()) >= 2.0D) {
                    UtilVelocity.velocity(other, UtilVelocity.getTrajectory(target, player), 0.3D, false, 0.0D, 0.0D, 1.0D, true);
                }

            } else {

                // TODO handle this in fetchnearbyentityevent
                //if(ShopManager.isShop(other)) continue;
                UtilVelocity.velocity(other, UtilVelocity.getTrajectory(other, player), 0.3D, false, 0.0D, 0.0D, 1.0D, true);
            }
        }
    }


    @Override
    public float getEnergy(int level) {

        return getSkillConfig().getEnergyCost() - ((level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return 0.5;
    }
}
