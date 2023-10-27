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
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Recall extends Skill implements ToggleSkill, CooldownSkill, Listener {

    public WeakHashMap<Player, RecallData> data = new WeakHashMap<>();
    public double percentHealthRecovered;
    @Inject
    public Recall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getDefaultClassString() {
        return "assassin";
    }
    @Override
    public String getName() {
        return "Recall";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Teleports you back in time <val>" + (1.5 + (level)) + "</val> seconds, increasing",
                "your health by <stat>" + (percentHealthRecovered * 100) + "%</stat> of the health you had",
                "",
                "Cooldown: <val>" + getCooldown(level)
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
            if (recallData.locations.size() > 0) {
                if (!player.getWorld().getName().equalsIgnoreCase(recallData.getLocation().getWorld().getName())) {
                    UtilMessage.message(player, "Champions", "You can not recall into a different world");
                    return false;
                }
            } else {
                UtilMessage.message(player, "Champions", "You have nowhere to recall to.");
            }
        }

        return true;
    }

    @Override
    public void toggle(Player player, int level) {

        RecallData recallData = data.get(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0F, 2.0F);
        player.teleport(recallData.getLocation());
        UtilEntity.setHealth(player, player.getHealth() + (recallData.getHealth() * percentHealthRecovered));

        player.getWorld().playEffect(data.get(player).getLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK);

        UtilServer.callEvent(new EffectClearEvent(player));

    }

    @Override
    public void loadSkillConfig(){
        percentHealthRecovered = getConfig("percentHealthRecovered", 0.25, Double.class);
    }
}
