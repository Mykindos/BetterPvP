package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ExcessiveForce extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();

    @Inject
    public ExcessiveForce(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Excessive Force";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate.",
                "",
                "For the next " + ChatColor.GREEN + (3 + ((level - 1) * 0.5)) + ChatColor.GRAY + " seconds",
                "your attacks deal knockback to enemies",
                "",
                "Does not ignore anti-knockback abilities.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)};
    }


    @Override
    public void activate(Player player, int level) {
        active.put(player, System.currentTimeMillis() + (2500 + (level * 500L)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1.7f);
    }


    @EventHandler
    public void setKnockback(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (active.containsKey(damager)) {
                event.setKnockback(true);
            }
        }
    }

    @UpdateEvent(delay = 125)
    public void onUpdate() {
        Iterator<Map.Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, Long> next = it.next();
            if (next.getValue() - System.currentTimeMillis() <= 0) {
                it.remove();
                continue;
            }

            if(!championsManager.getRoles().hasRole(next.getKey(), Role.ASSASSIN)){
                it.remove();
            }

        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return getSkillConfig().getCooldown() - (level * 2);
    }


}
