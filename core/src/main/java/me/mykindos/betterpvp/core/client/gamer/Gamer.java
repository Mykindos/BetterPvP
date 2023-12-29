package me.mykindos.betterpvp.core.client.gamer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import me.mykindos.betterpvp.core.utilities.model.display.ActionBar;
import me.mykindos.betterpvp.core.utilities.model.display.PlayerList;
import me.mykindos.betterpvp.core.utilities.model.display.TitleQueue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A gamer represents a clients seasonal data.
 * Such as their blocks broken, their kills, deaths, etc.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = {"uuid"})
public class Gamer extends PropertyContainer implements Invitable, Unique, IMapListener {

    private final String uuid;
    private ActionBar actionBar = new ActionBar();
    private TitleQueue titleQueue = new TitleQueue();
    private PlayerList playerList = new PlayerList();

    private long lastDamaged;
    private long lastTip;
    private String lastAdminMessenger;
    private boolean isHoldingRightClick;

    public Gamer(String uuid) {
        this.uuid = uuid;
        properties.registerListener(this);
    }

    public boolean canBlock() {
        final Player player = getPlayer();
        if (player != null) {
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            return UtilItem.isSword(main) || main.getType().equals(Material.SHIELD)
                    || UtilItem.isSword(off) || off.getType().equals(Material.SHIELD);
        }

        return false;
    }

    public boolean isHoldingRightClick() {
        if (canBlock()) {
            final Player player = getPlayer();
            return player != null && (player.isBlocking() || player.isHandRaised());
        }

        return isHoldingRightClick;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(uuid));
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public int getBalance() {
        return (int) getProperty(GamerProperty.BALANCE).orElse(0);
    }

    public int getIntProperty(Enum<?> key) {
        return (int) getProperty(key).orElse(0);
    }

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
        if (updateScoreboard) {
            Player player = Bukkit.getPlayer(UUID.fromString(getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        }

    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new GamerPropertyUpdateEvent( this, key, value)));
    }

    public void setLastTipNow() {
        setLastTip(System.currentTimeMillis());
    }

    @Override
    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }
}
