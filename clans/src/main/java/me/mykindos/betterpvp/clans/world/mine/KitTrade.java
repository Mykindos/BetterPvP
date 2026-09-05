package me.mykindos.betterpvp.clans.world.mine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The quartermaster's exchange: a fixed pile of bloomstone for the full four pieces of one role's armour.
 * <p>
 * Sets are resolved by {@link me.mykindos.betterpvp.core.item.ItemKey} through the {@link ItemRegistry} rather than by
 * importing the armour classes, because clans depends on champions only at compile time — the armour may not be on the
 * server at all. Looking them up by name means a missing set is one absent option in this dialog rather than a plugin
 * that will not load.
 * <p>
 * The cost is re-checked when a button is pressed, not when the dialog opens. A dialog is a window the player can sit
 * on for as long as they like, and they can spend, drop or store their bloomstone while it is open.
 */
@Singleton
@CustomLog
public class KitTrade {

    private static final String HELMET = "helmet";
    private static final String CHESTPLATE = "chestplate";
    private static final String LEGGINGS = "leggings";
    private static final String BOOTS = "boots";
    private static final String[] PIECES = {HELMET, CHESTPLATE, LEGGINGS, BOOTS};

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final MineCurrency currency;

    @Inject
    public KitTrade(@NotNull ItemRegistry itemRegistry, @NotNull ItemFactory itemFactory,
                    @NotNull MineCurrency currency) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.currency = currency;
    }

    /**
     * Shows the exchange: every role whose armour is actually registered, each with its helmet as the picture of what
     * the trade buys.
     *
     * @param cost how much bloomstone one set takes
     */
    public void open(@NotNull Player player, int cost) {
        final List<Role> offered = new ArrayList<>();
        for (Role role : Role.values()) {
            if (setFor(role) != null) {
                offered.add(role);
            }
        }
        if (offered.isEmpty()) {
            UtilMessage.message(player, "Quartermaster", "clans.mine.trade.unavailable");
            return;
        }
        player.showDialog(Dialog.create(factory -> build(factory, offered, cost)));
    }

    private void build(@NotNull RegistryBuilderFactory<@NotNull Dialog, ? extends DialogRegistryEntry.Builder> factory,
                       @NotNull List<Role> offered, int cost) {
        final DialogRegistryEntry.Builder builder = factory.empty();

        final List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(Translations.component("clans.mine.trade.prompt",
                Component.text(cost, NamedTextColor.GOLD))));
        final List<ActionButton> buttons = new ArrayList<>();
        for (Role role : offered) {
            final ItemStack helmet = stackOf(keyFor(role, HELMET));
            if (helmet != null) {
                // The sprite and the button are separate elements in a dialog, so the helmet is shown as the picture of
                // the trade and the button beneath it carries the role's name.
                bodies.add(DialogBody.item(helmet).showTooltip(true).build());
            }
            buttons.add(ActionButton.builder(Component.text(role.getName(), role.getColor()))
                    .action(DialogAction.customClick(
                            (response, audience) -> grant(audience, role, cost),
                            ClickCallback.Options.builder().build()))
                    .build());
        }

        builder.base(DialogBase.builder(Translations.component("clans.mine.trade.title"))
                .body(bodies)
                .build());
        builder.type(DialogType.multiAction(buttons).build());
    }

    /**
     * Hands over a set, having re-established that the player can still pay for it and has somewhere to put it. Both
     * checks have to happen here rather than at open time: the dialog has been sitting on their screen in the meantime.
     */
    private void grant(@NotNull Audience audience, @NotNull Role role, int cost) {
        if (!(audience instanceof Player player)) {
            return;
        }
        if (currency.count(player) < cost) {
            UtilMessage.message(player, "Quartermaster", "clans.mine.trade.insufficient", Component.text(cost));
            return;
        }
        final List<ItemStack> set = setFor(role);
        if (set == null) {
            UtilMessage.message(player, "Quartermaster", "clans.mine.trade.unavailable");
            return;
        }
        if (freeSlots(player) < set.size()) {
            UtilMessage.message(player, "Quartermaster", "clans.mine.trade.no-space");
            return;
        }
        if (!currency.take(player, cost)) {
            UtilMessage.message(player, "Quartermaster", "clans.mine.trade.insufficient", Component.text(cost));
            return;
        }
        set.forEach(piece -> player.getInventory().addItem(piece));
        UtilMessage.message(player, "Quartermaster", "clans.mine.trade.success",
                Component.text(cost), Component.text(role.getName(), role.getColor()));
    }

    /** @return the four pieces of a role's armour, or null if any of them is not registered on this server */
    private @Nullable List<ItemStack> setFor(@NotNull Role role) {
        final List<ItemStack> set = new ArrayList<>(PIECES.length);
        for (String piece : PIECES) {
            final ItemStack stack = stackOf(keyFor(role, piece));
            if (stack == null) {
                return null;
            }
            set.add(stack);
        }
        return set;
    }

    private @Nullable ItemStack stackOf(@NotNull String key) {
        final BaseItem item = itemRegistry.getItem(key);
        return item == null ? null : itemFactory.create(item).getItemStack();
    }

    private static @NotNull String keyFor(@NotNull Role role, @NotNull String piece) {
        return "champions:" + role.name().toLowerCase(Locale.ROOT) + "_" + piece;
    }

    private static int freeSlots(@NotNull Player player) {
        int free = 0;
        final ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType().isAir()) {
                free++;
            }
        }
        return free;
    }
}
