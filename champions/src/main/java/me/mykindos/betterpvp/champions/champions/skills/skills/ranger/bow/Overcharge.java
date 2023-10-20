package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.CrossbowMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Overcharge extends Skill implements InteractSkill, Listener {

    private final WeakHashMap<Player, OverchargeData> data = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Integer> bonus = new WeakHashMap<>();
    private final List<Arrow> arrows = new ArrayList<>();
    private final Set<UUID> charging = new HashSet<>();

    private int damageIncrement;

    private double durationIncrement;

    @Inject
    public Overcharge(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overcharge";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Bow to use",
                "",
                "Draw back harder on your bow, giving",
                "<stat>" + damageIncrement + "</stat> bonus damage per <stat>" + durationIncrement + "</stat> seconds",
                "",
                "Maximum Damage: <val>" + (3 + level)

        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        charging.remove(event.getPlayer().getUniqueId());

    }


    @EventHandler
    public void onPlayerShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        charging.remove(player.getUniqueId());
        if (hasSkill(player)) {
            OverchargeData overchargeData = data.get(player);
            if (overchargeData != null) {
                bonus.put(arrow, overchargeData.getCharge());
                data.remove(player);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (bonus.containsKey(arrow)) {
            event.setDamage(event.getDamage() + bonus.get(arrow));
            event.setReason(getName());
        }
    }

    @UpdateEvent
    public void updateOvercharge() {
        Iterator<Map.Entry<Player, OverchargeData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            OverchargeData data = iterator.next().getValue();
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null) {

                if (!charging.contains(player.getUniqueId())) {
                    iterator.remove();
                    continue;
                }

                if (!UtilPlayer.isHoldingItem(player, SkillWeapons.BOWS)) {
                    iterator.remove();
                    continue;
                }

                Material mainhand = player.getInventory().getItemInMainHand().getType();
                if (mainhand == Material.BOW && player.getActiveItem().getType() == Material.AIR) {
                    iterator.remove();
                    continue;
                }

                if (mainhand == Material.CROSSBOW && player.getActiveItem().getType() == Material.AIR) {
                    CrossbowMeta meta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
                    if (!meta.hasChargedProjectiles()) {
                        iterator.remove();
                    }
                    continue;
                }

                if (UtilBlock.isInLiquid(player)) {
                    iterator.remove();
                    continue;
                }

                if (UtilTime.elapsed(data.getLastCharge(), (long) (durationIncrement * 1000))) {
                    if (data.getCharge() < data.getMaxCharge()) {
                        data.addCharge();
                        UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <yellow>+%d<gray> Bonus Damage", getName(), data.getCharge());
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.0F + 0.05F * data.getCharge());
                    }
                }
            }
        }

        arrows.removeIf(arrow -> arrow.isOnGround() || !arrow.isValid() || arrow.isInsideVehicle());
    }


    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        if (!data.containsKey(player)) {
            data.put(player, new OverchargeData(player.getUniqueId(), damageIncrement, (3 + level)));
            charging.add(player.getUniqueId());

        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    @Data
    private static class OverchargeData {
        private final UUID uuid;
        private final int increment;
        private final int maxCharge;

        private int charge;
        private long lastCharge;

        public OverchargeData(UUID uuid, int increment, int maxCharge) {
            this.uuid = uuid;
            this.charge = 0;
            this.lastCharge = System.currentTimeMillis();
            this.increment = increment;
            this.maxCharge = maxCharge;

        }

        public void addCharge() {
            if (getCharge() <= getMaxCharge()) {
                setCharge(getCharge() + getIncrement());
                lastCharge = System.currentTimeMillis();
            }
        }

    }
    public void loadSkillConfig() {
        damageIncrement = getConfig("damageIncrement", 1, Integer.class);
        durationIncrement = getConfig("durationIncrement", 0.5, Double.class);
    }
}
