package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class LightningOrb extends Skill implements InteractSkill, CooldownSkill, Listener {

    private int maxTargets;
    private double spreadDistance;
    private double slowDuration;
    private double shockDuration;

    @Inject
    public LightningOrb(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Lightning Orb";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Launch a lightning orb. Directly hitting a player",
                "will strike all enemies within " + ChatColor.GREEN + (3 + (level * 0.5)) + ChatColor.GRAY + " blocks",
                "with lightning, giving them Slowness II for 4 seconds.",
                "",
                "Recharge: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level * 2));
    }


    @EventHandler
    public void onCollide(ThrowableHitEntityEvent event) {
        if (!event.getThrowable().getName().equals(getName())) return;
        if (!(event.getThrowable().getThrower() instanceof Player thrower)) return;

        int level = getLevel(thrower);
        if (level > 0) {
            int count = 0;
            for (LivingEntity ent : UtilEntity.getNearbyEnemies(thrower, event.getThrowable().getItem().getLocation(), spreadDistance + (0.5 * level))) {

                if (count >= maxTargets) continue;
                event.getThrowable().getImmunes().add(ent);
                if (ent instanceof Player target) {
                    championsManager.getEffects().addEffect(target, EffectType.SHOCK, (long) shockDuration * 1000);
                }

                ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) slowDuration * 20, 1));

                thrower.getLocation().getWorld().spigot().strikeLightning(ent.getLocation(), true);
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, event.getThrowable().getThrower(), null, DamageCause.CUSTOM, 11, false, getName()));

                ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
                count++;
            }
        }
    }


    @Override
    public void activate(Player player, int level) {
        Item orb = player.getWorld().dropItem(player.getEyeLocation().add(player.getLocation().getDirection()), new ItemStack(Material.DIAMOND_BLOCK));
        orb.setVelocity(player.getLocation().getDirection());
        ThrowableItem throwableItem = new ThrowableItem(orb, player, "Lightning Orb", 5000, true, true);
        championsManager.getThrowables().addThrowable(throwableItem);
    }

    @Override
    public void loadSkillConfig() {
        maxTargets = getConfig("maxTarget", 3, Integer.class);
        spreadDistance = getConfig("spreadDistance", 3.0, Double.class);
        slowDuration = getConfig("slowDuration", 4.0, Double.class);
        shockDuration = getConfig("shockDuration", 2.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
