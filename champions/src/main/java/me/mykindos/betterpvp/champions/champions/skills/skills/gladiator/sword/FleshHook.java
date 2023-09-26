package me.mykindos.betterpvp.champions.champions.skills.skills.gladiator.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
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
import me.mykindos.betterpvp.core.utilities.*;
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

import java.util.*;

@Singleton
@BPvPListener
public class FleshHook extends ChannelSkill implements InteractSkill, CooldownSkill {

    private final List<ChargeData> charges = new ArrayList<>();
    private final WeakHashMap<Player, Long> delay = new WeakHashMap<>();

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
                "",
                "Higher Charge time = faster hook",
                "",
                "Cooldown: <val>" + getCooldown(level),
        };
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
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
                        Location itemLocation = loc.clone();

                        Location infront = player.getEyeLocation().add(player.getLocation().getDirection());
                        for (int x = -20; x <= 20; x += 5) {
                            Item item = player.getWorld().dropItem(infront, new ItemStack(Material.TRIPWIRE_HOOK));
                            ThrowableItem throwable = new ThrowableItem(item, player, getName(), 10000L, true, true);
                            throwable.setCollideGround(true);
                            championsManager.getThrowables().addThrowable(throwable);

                            itemLocation.setYaw(loc.getYaw() + x);
                            Vector v = itemLocation.getDirection();

                            UtilVelocity.velocity(item, v,
                                    base + (data.getCharge() / 20f) * (0.25D * base), false, 0.0D, 0.2D, 20.0D, false);


                        }


                        UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + "</alt>.");
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0F, 0.8F);


                        iterator.remove();

                        championsManager.getCooldowns().removeCooldown(player, getName(), true);
                        championsManager.getCooldowns().add(player, getName(), getCooldown(level), showCooldownFinished());
                    }
                }
            }

        }
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent event) {
        if (event.getThrowable().getName().equalsIgnoreCase(getName())) {
            LivingEntity collide = event.getCollision();

            UtilVelocity.velocity(collide, UtilVelocity.getTrajectory(collide.getLocation(), event.getThrowable().getThrower().getLocation()), 2.0D, false, 0.0D, 0.8D, 1.0D, true);
            event.getThrowable().getItem().remove();

            CustomDamageEvent ev = new CustomDamageEvent(collide, event.getThrowable().getThrower(), null, DamageCause.CUSTOM, 2, false, getName());
            UtilDamage.doCustomDamage(ev);

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
}
