package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PunishmentItem extends AbstractItem {

    private final Punishment punishment;
    private final ClientManager clientManager;

    public PunishmentItem(Punishment punishment, ClientManager clientManager) {
        this.punishment = punishment;
        this.clientManager = clientManager;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
        CompletableFuture<String> punisherNameFuture = new CompletableFuture<>();
        if (punishment.getPunisher() != null) {
            clientManager.search().offline(punishment.getPunisher(), (clientOptional) -> {
                clientOptional.ifPresent(value -> punisherNameFuture.complete(value.getName()));
            });
        } else {
            punisherNameFuture.complete("SERVER");
        }
        CompletableFuture<String> revokerNameFuture = new CompletableFuture<>();
        if (punishment.getRevoker() != null) {
            clientManager.search().offline(punishment.getRevoker(), (clientOptional) -> {
                clientOptional.ifPresent(value -> revokerNameFuture.complete(value.getName()));
            });
        } else {
            revokerNameFuture.complete("SERVER");
        }
        Rule rule = punishment.getRule();

        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Type:</gray> <white>%s</white>",
                punishment.getType().getName()));
        if (punishment.getExpiryTime() > 0) {
            lore.add(UtilMessage.deserialize("<gray>Duration:</gray> <green>%s</green>",
                    UtilTime.getTime(punishment.getExpiryTime() - punishment.getApplyTime(), 1)));
        } else {
            lore.add(UtilMessage.deserialize("<gray>Duration:</gray> <red>%s</red>",
                    "Permanent"));
        }

        //TODO include absolute time
        lore.add(UtilMessage.deserialize("<gray>Time Punished:</gray> <yellow>%s</yellow> ago",
                UtilTime.getTime(System.currentTimeMillis() - punishment.getApplyTime(), 1)));
        if (punishment.isActive()) {
            if (punishment.getExpiryTime() > 0) {
                lore.add(UtilMessage.deserialize("<gray>Remaining Time:</gray> <red>%s</red>",
                        UtilTime.getTime(punishment.getExpiryTime() - System.currentTimeMillis(), 1)));
            } else {
                lore.add(UtilMessage.deserialize("<Red>PERMANENT</red>"));
            }
        }
        lore.add(UtilMessage.deserialize("<gray>Reason:</gray> <white>%s</white>",
                punishment.getReason()));
        lore.add(UtilMessage.deserialize("<gray>Punisher:</gray> <yellow>%s</yellow>",
                punisherNameFuture.join()));
        if (punishment.isRevoked()) {
            lore.add(UtilMessage.deserialize("<red>Revoked!</red>"));
            lore.add(UtilMessage.deserialize("<gray>Revoker:</gray> <yellow>%s</yellow>",
                    revokerNameFuture.join()));
            //TODO include absolute time
            lore.add(UtilMessage.deserialize("<gray>Time Revoked:</gray> <yellow>%s</yellow> ago",
                    UtilTime.getTime(System.currentTimeMillis() - punishment.getRevokeTime(), 1)));
            lore.add(UtilMessage.deserialize("<gray>Revoke Type:</gray> <green>%s</green>",
                    Objects.requireNonNull(punishment.getRevokeType()).name()));
            lore.add(UtilMessage.deserialize("<gray>Revoke Reason:</gray> <white>%s</white>",
                    punishment.getRevokeReason()));
        }



        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
                .displayName(Component.text(rule.getKey()))
                .material(rule.getMaterial())
                .customModelData(rule.getCustomModelData())
                .glow(punishment.isActive())
                .lore(lore);

        return itemViewBuilder.build();
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
