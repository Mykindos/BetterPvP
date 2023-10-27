package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PowerChop extends PrepareSkill implements CooldownSkill {

    private double timeToHit;

    private final WeakHashMap<Player, Long> charge = new WeakHashMap<>();

    @Inject
    public PowerChop(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Power Chop";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to prepare",
                "",

                "Your next axe attack will",
                "deal <val>" + (Math.max(1, (level + 2))) + "</val> bonus damage.",
                "",
                "The attack must be made within",
                "<stat>" + timeToHit + "</stat> seconds of being used",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }
    @Override
    public String getDefaultClassString() {
        return "knight";
    }
    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!charge.containsKey(player)) return;
        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.AXES)) return;

        int level = getLevel(player);
        if (level > 0) {
            event.setDamage(event.getDamage() + ((Math.max(0.75, (level + 2)) * 0.75)));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0F, 1.0F);
            event.setReason(getName());
            charge.remove(player);
        }
    }

    @UpdateEvent(delay = 100)
    public void removeFailures() {
        charge.entrySet().removeIf(entry -> {
            if (UtilTime.elapsed(entry.getValue(), (long) timeToHit * 1000)) {
                Player player = entry.getKey();
                UtilMessage.simpleMessage(player, "Champions", "You failed to use <green>%s<gray>.", getName());
                player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                return true;
            }

            return false;
        });
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 1.5);
    }

    @Override
    public void activate(Player player, int level) {
        charge.put(player, System.currentTimeMillis());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 2.0f, 1.8f);
    }

    @Override
    public void loadSkillConfig() {
        timeToHit = getConfig("timeToHit", 1.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
