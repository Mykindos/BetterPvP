package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.RecallData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Recall extends Skill implements ToggleSkill, CooldownSkill, Listener {

    public WeakHashMap<Player, RecallData> data = new WeakHashMap<>();

    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Recall";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop Sword / Axe to Activate",
                "",
                "Teleports you back to where you ",
                "were located " + ChatColor.GREEN + (1.5 + (level)) + ChatColor.GRAY + " seconds ago",
                "Increases health by 1/4 of the health you had",
                ChatColor.GREEN.toString() + (1.5 + (level)) + ChatColor.GRAY + " seconds ago",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @EventHandler
    public void onRoleChange(RoleChangeEvent e) {
        data.remove(e.getPlayer());
    }


    @UpdateEvent(delay = 500)
    public void updateRecallData() {

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            int level = getLevel(onlinePlayer);
            if (level > 0) {
                if (data.containsKey(onlinePlayer)) {
                    RecallData recallData = data.get(onlinePlayer);
                    if (UtilTime.elapsed(recallData.getTime(), 1000)) {
                        recallData.addLocation(onlinePlayer.getLocation(), onlinePlayer.getHealth(), (1.5 + (level)));
                        recallData.setTime(System.currentTimeMillis());
                    }
                } else {
                    data.put(onlinePlayer, new RecallData());
                    data.get(onlinePlayer).addLocation(onlinePlayer.getLocation(), onlinePlayer.getHealth(), (1.5 + (level)));
                }
            }

        }
    }

    @EventHandler
    public void onSkillDequip(SkillDequipEvent event) {
        if (event.getSkill().equals(this)) {
            data.remove(event.getPlayer());
        }
    }

    @EventHandler
    public void onEquip(SkillEquipEvent e) {
        if (e.getSkill() == this) {
            if (data.containsKey(e.getPlayer())) {
                data.get(e.getPlayer()).locations.clear();
            }
        }
    }


    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }

    @Override
    public boolean canUse(Player player) {
        RecallData recallData = data.get(player);
        if (recallData != null) {
            if(recallData.locations.size() > 0) {
                if (!player.getWorld().getName().equalsIgnoreCase(recallData.getLocation().getWorld().getName())) {
                    UtilMessage.message(player, getClassType().getName(), "You can not recall into a different world");
                    return false;
                }
            }else{
                UtilMessage.message(player, getClassType().getName(), "You have nowhere to recall to.");
            }
        }

        return true;
    }

    @Override
    public void toggle(Player player, int level) {

        RecallData recallData = data.get(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);
        player.teleport(recallData.getLocation());
        UtilEntity.setHealth(player, player.getHealth() + (recallData.getHealth() / 4));

        player.getWorld().playEffect(data.get(player).getLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK);

    }
}
