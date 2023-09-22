package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.WeakHashMap;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class DefensiveStance extends ChannelSkill implements InteractSkill, EnergySkill {

    private final WeakHashMap<Player, Long> gap = new WeakHashMap<>();

    private double damage;
    @Inject
    public DefensiveStance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Defensive Stance";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold Block with a sword to Channel.",
                "",
                "While active, you are immune to all",
                "melee damage from attacks infront of you.",
                "",
                "Players who attack you receive <val>" + damage + "</val> damage,",
                "and get knocked back.",
                "",
                "Energy / Second: <val>" + getEnergy(level)};
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (!player.isHandRaised()) return;

        int level = getLevel(player);
        if (level > 0) {
            Vector look = player.getLocation().getDirection();
            look.setY(0);
            look.normalize();

            Vector from = UtilVelocity.getTrajectory(player, event.getDamager());
            from.normalize();
            if (player.getLocation().getDirection().subtract(from).length() > 0.6D) {
                return;
            }

            event.getDamager().setVelocity(event.getDamagee().getEyeLocation().getDirection().add(new Vector(0, 0.5, 0)).multiply(1));

            CustomDamageEvent customDamageEvent = new CustomDamageEvent(event.getDamager(), event.getDamagee(), null, DamageCause.CUSTOM, damage, false, getName());
            UtilDamage.doCustomDamage(customDamageEvent);

            event.cancel(getName());

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 2.0F);
        }

    }

    @UpdateEvent
    public void useEnergy() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!active.contains(player.getUniqueId())) continue;

            if (player.isHandRaised()) {
                int level = getLevel(player);
                if (level <= 0) {
                    active.remove(player.getUniqueId());
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 2, true)) {
                    active.remove(player.getUniqueId());
                } else if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                    active.remove(player.getUniqueId());
                } else {
                    player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 20);
                }

            } else {
                if (gap.containsKey(player)) {
                    if (UtilTime.elapsed(gap.get(player), 250)) {
                        active.remove(player.getUniqueId());
                        gap.remove(player);
                    }
                }
            }


        }
    }


    @Override
    public float getEnergy(int level) {

        return (float) energy - ((level - 1));
    }

    @Override
    public void activate(Player p, int level) {
        if (!active.contains(p.getUniqueId())) {
            active.add(p.getUniqueId());
            gap.put(p, System.currentTimeMillis());

        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
    }
}
