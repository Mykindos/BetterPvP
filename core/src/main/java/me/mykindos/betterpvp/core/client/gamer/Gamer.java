package me.mykindos.betterpvp.core.client.gamer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.chat.channels.ServerChatChannel;
import me.mykindos.betterpvp.core.chat.channels.events.PlayerChangeChatChannelEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.offhand.OffhandExecutor;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarOverlay;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarQueue;
import me.mykindos.betterpvp.core.utilities.model.display.experience.ExperienceBar;
import me.mykindos.betterpvp.core.utilities.model.display.experience.ExperienceLevel;
import me.mykindos.betterpvp.core.utilities.model.display.playerlist.PlayerList;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A gamer represents a clients seasonal data.
 * Such as their blocks broken, their kills, deaths, etc.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = {"uuid"})
public class Gamer extends PropertyContainer implements Invitable, Unique, IMapListener {

    private final long id;
    private final String uuid;
    private ActionBar actionBar = new ActionBar();
    private TitleQueue titleQueue = new TitleQueue();
    private PlayerList playerList = new PlayerList();
    private ExperienceBar experienceBar = new ExperienceBar();
    private ExperienceLevel experienceLevel = new ExperienceLevel();
    private BossBarQueue bossBarQueue = new BossBarQueue();
    private BossBarOverlay bossBarOverlay = new BossBarOverlay();
    private Sidebar sidebar = null;
    private @NotNull IChatChannel chatChannel = ServerChatChannel.getInstance();
    private OffhandExecutor offhandExecutor = null;

    private long lastDamaged = -1;
    private long lastDeath = -1;
    private long lastSafe = -1;
    private long lastTip = -1;
    private long lastBlock = -1;
    private long lastMovement = -1;
    private double lastDealtDamageValue = 0;
    private String lastAdminMessenger;

    public Gamer(long id, String uuid) {
        this.id = id;
        this.uuid = uuid;
        this.properties.registerListener(this);
    }

    public long timeSinceLastBlock() {
        if (lastBlock == -1) {
            return -1;
        }

        return System.currentTimeMillis() - lastBlock;
    }

    public boolean canBlock() {
        final Player player = getPlayer();
        if (player != null) {
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            return main.getMaxItemUseDuration(player) > 0 || off.getMaxItemUseDuration(player) > 0;
        }

        return false;
    }

    public boolean isHoldingRightClick() {
        final Player player = getPlayer();
        if (player == null) {
            return false;
        }
        if (canBlock()) {


            // If they're holding a cosmetic shield, give them a grace period for them to raise their hand
            // Otherwise, this would return false
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            if (UtilItem.isUndroppable(main) || UtilItem.isUndroppable(off)) {
                return timeSinceLastBlock() <= 250;
            }

            return player.isBlocking() || player.isHandRaised() || lastBlock != -1;
        }

        return timeSinceLastBlock() <= 250;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(uuid));
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public int getBalance() {
        return getIntProperty(GamerProperty.BALANCE);
    }

    public void setSidebar(@Nullable Sidebar newSidebar) {
        if (this.sidebar != null && this.isOnline()) {
            this.sidebar.removePlayer(Objects.requireNonNull(getPlayer()));
            sidebar.close();
        }

        this.sidebar = newSidebar;

    }

    @Override
    public void saveProperty(String key, Object object) {
        properties.put(key, object);
    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new GamerPropertyUpdateEvent(this, key, value)));
    }

    public void setLastTipNow() {
        setLastTip(System.currentTimeMillis());
    }

    public void setLastMovementNow() {
        setLastMovement(System.currentTimeMillis());
    }

    public boolean isMoving() {
        return !UtilTime.elapsed(getLastMovement(), 100);
    }

    public void setLastSafeNow() {
        setLastSafe(System.currentTimeMillis());
    }

    public void updateRemainingProtection() {
        long remainingProtection = getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        remainingProtection = remainingProtection - (System.currentTimeMillis() - getLastSafe());
        saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, remainingProtection);
    }

    @Override
    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    public void setLastDamaged(long lastDamaged) {
        if (!UtilTime.elapsed(lastDeath, 10_000)) {
            //don't set lastDamaged if the player has recently died
            return;
        }
        this.lastDamaged = lastDamaged;
    }

    public boolean isInCombat() {
        return !UtilTime.elapsed(lastDamaged, DamageLog.EXPIRY);
    }

    public void setChatChannel(@NotNull ChatChannel chatChannel) {
        Player player = getPlayer();

        if (player != null) {
            PlayerChangeChatChannelEvent event = UtilServer.callEvent(new PlayerChangeChatChannelEvent(this, chatChannel));
            if (event.isCancelled()) {
                return;
            }

            if (event.getNewChannel() == null) {
                this.chatChannel = ServerChatChannel.getInstance();
                return;
            }

            UtilMessage.simpleMessage(player, "Chat", "Channel: <green>" + event.getNewChannel().getChannel().name().toLowerCase());
            this.chatChannel = event.getNewChannel();
        }

    }

}
