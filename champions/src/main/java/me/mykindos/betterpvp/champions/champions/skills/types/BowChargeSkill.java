package me.mykindos.betterpvp.champions.champions.skills.types;

import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.CrossbowMeta;

import java.util.Iterator;
import java.util.WeakHashMap;

@CustomLog
public abstract class BowChargeSkill extends ChargeSkill {
    private final DisplayObject<Component> actionBarComponent = ChargeData.getActionBar(this, charging);
    private final WeakHashMap<Player, Long> timeSinceChargeStart = new WeakHashMap<>();

    public BowChargeSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        if (!UtilItem.isRanged(player.getInventory().getItemInMainHand())) return;

        int level = getLevel(player);
        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            return;
        }

        if (level > 0) {
            timeSinceChargeStart.put(player, System.currentTimeMillis());
        }
    }

    public abstract void onShootBow(EntityShootBowEvent event);

    @Override
    boolean shouldCancelCharge(Player player, ChargeData chargeData, int level) {
        Material mainhand = player.getInventory().getItemInMainHand().getType();
        if (mainhand == Material.BOW && player.getActiveItem().getType() == Material.AIR) {
            return true;
        }

        if (mainhand == Material.CROSSBOW && player.getActiveItem().getType() == Material.AIR) {
            CrossbowMeta meta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
            return !meta.hasChargedProjectiles();
        }
        return false;
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<Player> iterator = timeSinceChargeStart.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (player == null || !player.isOnline() || !player.isConnected()) {
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            if (shouldCancelCharge(player, null, level)) {
                iterator.remove();
                continue;
            }


            long timeSinceStart = System.currentTimeMillis() - timeSinceChargeStart.get(player);
            if (timeSinceStart >= 1000) {
                activate(player, level);
                iterator.remove();
            }
        }
    }
    @Override
    public boolean isHolding(Player player) {
        return hasSkill(player) && UtilItem.isRanged(player.getInventory().getItemInMainHand());
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(901, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }
}
