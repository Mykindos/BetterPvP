package me.mykindos.betterpvp.champions.champions.skills.skills.brute.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword.StaticLazer;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class FleshHook extends ChannelSkill implements InteractSkill, CooldownSkill {
    private final List<ChargeData> charges = new ArrayList<>();
    private final WeakHashMap<Player, Long> delay = new WeakHashMap<>();

    public double damage;
    public double damageIncreasePerLevel;

    @Inject
    public FleshHook(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Flesh Hook";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Charge a hook that latches onto enemies, pulling them towards you",
                "and dealing up to <val>" + (damage + (damageIncreasePerLevel * (level-1)))+"</val> damage",
                "",
                "Higher Charge time = faster hook",
                "",
                "Cooldown: <val>" + getCooldown(level),
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }


    @UpdateEvent
    public void updateFleshHook() {
        ListIterator<ChargeData> iterator = charges.listIterator();
        while (iterator.hasNext()) {
            ChargeData data = iterator.next();
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null) {
                int level = getLevel(player);
                if (level <= 0) {
                    iterator.remove();
                    continue;
                }
                if (delay.containsKey(player)) {
                    if (!UtilTime.elapsed(delay.get(player), 250)) {
                        continue;
                    }
                }
                if (player.isHandRaised()) {

                    if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        iterator.remove();
                    }

                    if (UtilTime.elapsed(data.getLastCharge(), 400L)) {
                        if (data.getCharge() < data.getMaxCharge()) {
                            data.addCharge();
                            UtilMessage.simpleMessage(player, getClassType().getName(), getName() + ": <alt2>+ " + data.getCharge() + "% Strength");
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.0F + 0.05F * data.getCharge());
                        }
                    }
                } else {
                    if (UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        double base = 0.8D;
                        Location loc = player.getLocation();

                        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.TRIPWIRE_HOOK));
                        ThrowableItem throwable = new ThrowableItem(item, player, getName(), 10000L, true, true);
                        throwable.setCollideGround(true);
                        throwable.setChargePercent(data.getCharge());
                        championsManager.getThrowables().addThrowable(throwable);

                        Vector v = loc.getDirection();
                        UtilVelocity.velocity(item, v, base + (data.getCharge() / 20f) * (0.25D * base), false, 0.0D, 0.2D, 20.0D, false);

                        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + "</alt>.");
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0F, 0.8F);

                        iterator.remove();

                        championsManager.getCooldowns().removeCooldown(player, getName(), true);
                        championsManager.getCooldowns().use(player, getName(), getCooldown(level), showCooldownFinished());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent event) {
        if (event.getThrowable().getName().equalsIgnoreCase(getName())) {
            LivingEntity target = event.getCollision();

            Player source = (Player) event.getThrowable().getThrower();

            int chargePercent = event.getThrowable().getChargePercent();
            double scaledDamage = (damage + (damageIncreasePerLevel * getLevel(source))) * (chargePercent / 100.0);

            CustomDamageEvent ev = new CustomDamageEvent(target, source, null, DamageCause.CUSTOM, scaledDamage, false, getName());
            UtilDamage.doCustomDamage(ev);

            UtilVelocity.velocity(target, UtilVelocity.getTrajectory(target.getLocation(), event.getThrowable().getThrower().getLocation()), 2.0D, false, 0.0D, 0.8D, 1.0D, true);
            event.getThrowable().getItem().remove();
        }
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }

    @Override
    public void activate(Player player, int level) {
        charges.add(new ChargeData(player.getUniqueId(), 25, 100));
        delay.put(player, System.currentTimeMillis());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Data
    private static class ChargeData {


        private final UUID uuid;
        private int charge;
        private long lastCharge;
        private final int increment;
        private final int maxCharge;

        public ChargeData(UUID uuid, int increment, int maxCharge) {
            this.uuid = uuid;
            this.charge = 0;
            this.lastCharge = System.currentTimeMillis();
            this.increment = increment;
            this.maxCharge = maxCharge;
        }

        public void addCharge() {
            if (charge < maxCharge) {
                charge += increment;
                lastCharge = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 7.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 2.0, Double.class);
    }
}
