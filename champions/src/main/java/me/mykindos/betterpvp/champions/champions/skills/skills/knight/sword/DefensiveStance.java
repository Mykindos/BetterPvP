package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class DefensiveStance extends ChannelSkill implements CooldownSkill, InteractSkill, EnergyChannelSkill, DefensiveSkill {

    private final WeakHashMap<Player, Long> gap = new WeakHashMap<>();

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double baseDamageReduction;

    private double damageReductionPerLevel;

    private boolean blocksMelee;

    private boolean blocksArrow;

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
                "Hold right click with a Sword to channel",
                "",
                "While active, you take " + getValueString(this::getDamageReduction, level, 100, "%", 0) + " reduced damage",
                "from all melee attacks in front of you",
                "",
                "Players who attack you receive " + getValueString(this::getDamage, level) + " damage,",
                "and get knocked back",
                "",
                "Energy / Second: " + getValueString(this::getEnergy, level),
                "Cooldown: " + getValueString(this::getCooldown, level, 2),
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + (damageReductionPerLevel * (level - 1));
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
        if (blocksMelee && blocksArrow) {
            if (!(event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() != DamageCause.PROJECTILE)) return;
        } else if (blocksMelee) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        } else if (blocksArrow) {
            if (event.getCause() != DamageCause.PROJECTILE) return;
        } else return;

        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        if (!gamer.isHoldingRightClick()) return;

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

            CustomDamageEvent customDamageEvent = new CustomDamageEvent(event.getDamager(), event.getDamagee(), null, DamageCause.CUSTOM, getDamage(level), false, getName());
            UtilDamage.doCustomDamage(customDamageEvent);
            event.setDamage(event.getDamage() * (1.0 - getDamageReduction(level)));
            if (event.getDamage() <= 0) {
                event.cancel(getName());
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 2.0F);
        }

    }

    @UpdateEvent
    public void useEnergy() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()
                    || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 2, true)
                    || (level <= 0)
                    || !isHolding(player)) {

                iterator.remove();
            }
            else {
                player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 20);
            }

        }

    }


    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
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
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseDamageReduction = getConfig("baseDamageReduction", 1.0, Double.class);
        damageReductionPerLevel = getConfig("damageReductionPerLevel", 0.0, Double.class);
        blocksMelee = getConfig("blocksMelee", true, Boolean.class);
        blocksArrow = getConfig("blocksArrow", false, Boolean.class);
    }


}
