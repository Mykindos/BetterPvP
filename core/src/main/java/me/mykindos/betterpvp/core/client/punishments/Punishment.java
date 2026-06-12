package me.mykindos.betterpvp.core.client.punishments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Punishment {

    /**
     * The id of this punishment
     */
    private final long id;
    /**
     * The UUID of the punished player
     */
    private final long clientId;

    private final UUID clientUUID;
    /**
     * The type of punishment
     */
    private final IPunishmentType type;
    /**
     * The rule this punishment si for
     */
    private final Rule rule;
    /**
     * When this punishment was applied
     */
    private final long applyTime;
    /**
     * When this punishment should expire
     */
    private final long expiryTime;
    /**
     * Why this punishment was applied
     */
    private final String reason;
    /**
     * The UUID of the punisher
     */
    @Nullable(value = "Null if server punish")
    private final UUID punisher;
    /**
     * The UUID of the revoker
     */
    @Nullable(value = "Null if not revoked")
    private UUID revoker;
    /**
     * The type of revoke
     */
    @Nullable(value = "Null if not revoked")
    private RevokeType revokeType;
    /**
     * The time of the revoke.
     * @value -1 = not revoked
     */
    private long revokeTime = -1;
    /**
     * The revoke reason
     */
    @Nullable(value = "Null if not revoked")
    private String revokeReason;

    /**
     * See if this punishment has expired
     * @return true if expired
     */
    public boolean hasExpired() {
        return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
    }

    /**
     * See if this punishment is revoked
     * @return true if revoked
     */
    public boolean isRevoked() {
        return revokeTime != -1 && System.currentTimeMillis() > revokeTime;
    }

    /**
     * See if this punishment is active. Active means it is not revoked and not expired.
     * @return true if active
     */
    public boolean isActive() {
        return !isRevoked() && !hasExpired();
    }

    /**
     * Sets this punishment as revoked
     * @param revoker the ID of the revoker, null if servver
     * @param revokeType the type of revoke
     * @param reason the reason for the revoke
     * @return the modified punishment
     */
    @Contract(value = "_, _, _ -> this", mutates = "this")
    public Punishment setRevoked(@Nullable UUID revoker, @NotNull RevokeType revokeType, @NotNull String reason) {
        this.setRevoker(revoker);
        this.setRevokeType(revokeType);
        this.setRevokeTime(System.currentTimeMillis());
        this.setRevokeReason(reason);
        this.getType().onExpire(clientUUID, this);
        return this;
    }

    /**
     * @return The formatted information about the punishment
     */
    public Component getInformation() {
        if (!this.isActive()) {
            return Translations.component("core.punishment.information.no_longer",
                    Component.text(this.type.getChatLabel(), NamedTextColor.RED));
        }
        String formattedTime = new PrettyTime().format(new Date(this.getExpiryTime())).replace(" from now", "");
        return Translations.component("core.punishment.information.active",
                Component.text(this.type.getChatLabel(), NamedTextColor.RED),
                Component.text(formattedTime, NamedTextColor.GREEN),
                Component.text(this.getReason(), NamedTextColor.YELLOW));
    }

    /**
     * @return the formatted punishment information
     */
    public Component getPunishmentInformation(ClientManager clientManager) {
        String punisherName = "SERVER";
        if (punisher != null) {
            Optional<Client> punisherOptional = clientManager.search().offline(punisher).join();
            if(punisherOptional.isPresent()) {
                punisherName = punisherOptional.get().getName();
            }
        }

        String revokerName = "SERVER";
        if (revoker != null) {
            Optional<Client> revokerOptional = clientManager.search().offline(revoker).join();
            if(revokerOptional.isPresent()) {
                revokerName = revokerOptional.get().getName();
            }
        }

        Component currentComp = Component.empty();
        if (this.isActive()) {
            currentComp = currentComp.append(Component.text("A ", NamedTextColor.GREEN));
            if (expiryTime > 0) {
                currentComp = currentComp.append(Translations.component("core.punishment.entry.duration",
                                        Component.text(UtilTime.getTime((double) System.currentTimeMillis() - applyTime, 1), NamedTextColor.YELLOW),
                                        Component.text(UtilTime.getTime((double) expiryTime - applyTime, 1), NamedTextColor.GREEN),
                                        Component.text(UtilTime.getTime((double) expiryTime - System.currentTimeMillis(), 1), NamedTextColor.RED)));
            } else {
                currentComp = currentComp.append(Translations.component("core.punishment.entry.duration",
                        Component.text(UtilTime.getTime((double) System.currentTimeMillis() - applyTime, 1), NamedTextColor.YELLOW),
                        Component.text("PERM", NamedTextColor.GREEN),
                        Component.text("n/a", NamedTextColor.RED)));
            }

        } else if (isRevoked()) {
            currentComp = currentComp.append(Component.text("R ", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(revokerName, NamedTextColor.LIGHT_PURPLE))
                            .hoverEvent(HoverEvent.showText(Translations.component("core.punishment.entry.revoke_hover",
                                    Component.text(UtilTime.getTime((double) System.currentTimeMillis() - revokeTime, 1), NamedTextColor.GREEN),
                                    Component.text(revokeType != null ? revokeType.name() : "null", NamedTextColor.YELLOW),
                                    Component.text(revokeReason == null ? "null" : revokeReason, NamedTextColor.WHITE))));
        } else {
            currentComp = currentComp.append(Component.text("E ", NamedTextColor.RED)).append(Translations.component("core.punishment.entry.duration",
                    Component.text(UtilTime.getTime((double) System.currentTimeMillis() - applyTime, 1), NamedTextColor.YELLOW),
                    Component.text(UtilTime.getTime((double) expiryTime - applyTime, 1), NamedTextColor.GREEN),
                    Component.text("n/a", NamedTextColor.RED)));
        }
        return Component.empty().append(currentComp).appendSpace()
                .append(Component.text(this.type.getName(), NamedTextColor.WHITE)).appendSpace()
                .append(Component.text(rule.getKey(), NamedTextColor.YELLOW)).appendSpace()
                .append(Component.text(reason == null ? "No Reason" : reason, NamedTextColor.GRAY)).appendSpace()
                .append(Component.text(punisherName, NamedTextColor.AQUA));
    }

    /**
     *
     * @param punisherName the name of the punisher
     * @param revokerName the name of the revoker
     * @param showPunisher should the punisher and revoker be shown?
     * @return the ItemView of this punishment
     */
    public ItemView getItemView(@Nullable String punisherName, @Nullable String revokerName, boolean showPunisher) {

        String punisherNameVar = punisherName;
        if(punisherNameVar == null) {
            punisherNameVar = "SERVER";
        }

        String revokerNameVar = revokerName;
        if(revokerNameVar == null) {
            revokerNameVar = "SERVER";
        }

        Rule rule = this.getRule();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(UtilTime.getDateTime(this.getApplyTime()), NamedTextColor.WHITE));
        lore.add(Translations.component("core.punishment.item.ago",
                Component.text(UtilTime.getTime(System.currentTimeMillis() - this.getApplyTime(), 1), NamedTextColor.WHITE)));
        lore.add(Translations.component("core.punishment.item.type",
                Component.text(this.getType().getName(), NamedTextColor.WHITE)));
        if (this.getExpiryTime() > 0 && this.getType().hasDuration()) {
            lore.add(Translations.component("core.punishment.item.duration",
                    Component.text(UtilTime.getTime(this.getExpiryTime() - this.getApplyTime(), 1), NamedTextColor.GREEN)));
        } else if (this.getType().hasDuration()) {
            lore.add(Translations.component("core.punishment.item.duration",
                    Translations.component("core.punishment.item.permanent").color(NamedTextColor.RED)));
        }
        if (this.isActive()) {
            if (this.getExpiryTime() > 0 && this.getType().hasDuration()) {
                lore.add(Translations.component("core.punishment.item.remaining_time",
                        Component.text(UtilTime.getTime(this.getExpiryTime() - System.currentTimeMillis(), 1), NamedTextColor.RED)));
            } else if (this.getType().hasDuration()) {
                lore.add(Translations.component("core.punishment.item.permanent_caps").color(NamedTextColor.RED));
            }
        }
        lore.add(Translations.component("core.punishment.item.reason",
                Component.text(this.getReason(), NamedTextColor.WHITE)));
        if (showPunisher) {
            lore.add(Translations.component("core.punishment.item.punisher",
                    Component.text(punisherNameVar, NamedTextColor.YELLOW)));
        }

        if (this.isRevoked()) {

            lore.add(Translations.component("core.punishment.item.revoked_header").color(NamedTextColor.RED));

            lore.add(Component.text(UtilTime.getDateTime(this.getRevokeTime()), NamedTextColor.WHITE));
            lore.add(Translations.component("core.punishment.item.ago",
                    Component.text(UtilTime.getTime(System.currentTimeMillis() - this.getRevokeTime(), 1), NamedTextColor.WHITE)));
            if (showPunisher) {
                lore.add(Translations.component("core.punishment.item.revoker",
                        Component.text(revokerNameVar, NamedTextColor.YELLOW)));
            }
            lore.add(Translations.component("core.punishment.item.revoke_type",
                    Component.text(getRevokeType() != null ? this.getRevokeType().name() : "null", NamedTextColor.GREEN)));
            lore.add(Translations.component("core.punishment.item.revoke_reason",
                    Component.text(this.getRevokeReason() == null ? "null" : this.getRevokeReason(), NamedTextColor.WHITE)));
        }



        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
                .displayName(Component.text(rule.getKey(), NamedTextColor.RED))
                .material(rule.getMaterial())
                .customModelData(rule.getCustomModelData())
                .glow(isActive())
                .lore(lore)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true);
        return itemViewBuilder.build();
    }
}
