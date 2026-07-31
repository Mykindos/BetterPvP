package me.mykindos.betterpvp.core.trade.menu.button;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.trade.TradeCurrency;
import me.mykindos.betterpvp.core.trade.TradeOffer;
import me.mykindos.betterpvp.core.trade.TradeSession;
import me.mykindos.betterpvp.core.trade.TradeState;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * The viewer's own stake of one currency. Left click sets an amount, right click takes it back off
 * the table.
 * <p>
 * Unlike items, a currency is not escrowed - only promised - so the amount is clamped to what the
 * player holds when they set it, and checked again at settlement.
 */
@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor
public class CurrencyOfferButton extends AbstractItem {

    private final TradeSession session;
    private final TradeOffer offer;
    private final TradeCurrency currency;
    private final Gamer gamer;
    private final Consumer<Component> errorSink;

    @Override
    public ItemProvider getItemProvider() {
        final int offered = offer.getCurrency(currency);
        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(currency.getIcon())
                .displayName(currency.getDisplayName().decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(Translations.component("core.trade.button.currency.offering").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(offered, NamedTextColor.WHITE)))
                .lore(Translations.component("core.trade.button.currency.holding").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(currency.getBalance(gamer), NamedTextColor.WHITE)));

        if (session.getState() == TradeState.NEGOTIATING) {
            builder.action(ClickActions.LEFT, Translations.component("core.trade.button.currency.set"));
            if (offered > 0) {
                builder.action(ClickActions.RIGHT, Translations.component("core.trade.button.currency.clear"));
            }
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (session.getState() != TradeState.NEGOTIATING) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        if (clickType.isRightClick()) {
            offer.setCurrency(currency, 0);
            session.markChanged();
            new SoundEffect(Sound.UI_BUTTON_CLICK, 0.8f, 1.0f).play(player);
            return;
        }

        player.showDialog(Dialog.create(this::createDialog));
    }

    private void createDialog(RegistryBuilderFactory<@NotNull Dialog, ? extends DialogRegistryEntry.Builder> factory) {
        final DialogRegistryEntry.Builder builder = factory.empty();
        final TextDialogInput input = DialogInput.text("amount", currency.getDisplayName()).maxLength(12).build();
        builder.base(DialogBase.builder(Translations.component("core.trade.dialog.currency.title"))
                .inputs(List.of(input))
                .build());

        builder.type(DialogType.confirmation(
                ActionButton.builder(Translations.component("core.trade.dialog.currency.confirm"))
                        .action(DialogAction.customClick((response, audience) -> applyAmount(response.getText("amount")),
                                ClickCallback.Options.builder().build()))
                        .build(),
                ActionButton.builder(Translations.component("core.menu.button.cancel.name")).build()));
    }

    /**
     * Applies a typed amount, refusing anything that is not a number the player can actually cover.
     * A rejected amount leaves the offer exactly as it was - a silent clamp would put a number on the
     * table that the player did not choose.
     */
    private void applyAmount(String text) {
        final int amount;
        try {
            amount = text == null || text.isBlank() ? 0 : Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            errorSink.accept(Translations.component("core.trade.error.invalid-amount"));
            return;
        }

        if (amount < 0) {
            errorSink.accept(Translations.component("core.trade.error.invalid-amount"));
            return;
        }

        if (amount > currency.getBalance(gamer)) {
            errorSink.accept(Translations.component("core.trade.error.insufficient-funds"));
            return;
        }

        offer.setCurrency(currency, amount);
        session.markChanged();
    }
}
