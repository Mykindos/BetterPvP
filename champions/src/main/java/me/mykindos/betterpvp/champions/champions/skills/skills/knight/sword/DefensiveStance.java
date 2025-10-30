package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.damage.ModifierOperation;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class DefensiveStance extends ChannelSkill implements CooldownSkill, InteractSkill, EnergyChannelSkill, DefensiveSkill {

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
                "While active, you take " + getValueString(this::getDamageReduction, level, 1, "%", 0) + " reduced damage",
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
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (blocksMelee && blocksArrow) {
            if (!(event.getBukkitCause() == DamageCause.ENTITY_ATTACK || event.getBukkitCause() != DamageCause.PROJECTILE)) return;
        } else if (blocksMelee) {
            if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        } else if (blocksArrow) {
            if (event.getBukkitCause() != DamageCause.PROJECTILE) return;
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

            event.getDamager().setVelocity(player.getEyeLocation().getDirection().add(new Vector(0, 0.5, 0)).multiply(1));

            DamageEvent DamageEvent = new DamageEvent(event.getDamager(), player, null, new SkillDamageCause(this), getDamage(level), getName());
            UtilDamage.doDamage(DamageEvent);
            event.addModifier(new SkillDamageModifier.Multiplier(this, (1.0 - getDamageReduction(level))));
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
                    || !championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)
                    || (level <= 0)
                    || !isHolding(player)) {

                iterator.remove();
            }
            else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_STEP, 0.5F, 1.0F);
            }

        }

    }

    @Override
    public boolean isShieldInvisible() {
        return false;
    }

    @Override
    public boolean shouldShowShield(Player player) {
        return !championsManager.getCooldowns().hasCooldown(player, getName());
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
        baseDamageReduction = getConfig("baseDamageReduction", 100.0, Double.class);
        damageReductionPerLevel = getConfig("damageReductionPerLevel", 0.0, Double.class);
        blocksMelee = getConfig("blocksMelee", true, Boolean.class);
        blocksArrow = getConfig("blocksArrow", false, Boolean.class);
    }


}
