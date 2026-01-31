package me.mykindos.betterpvp.core.supplycrate.item;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrate;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateController;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateType;
import me.mykindos.betterpvp.core.supplycrate.event.SupplyCrateDeployEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

public class DeployCrateAbility extends AbstractInteraction {

    private final SupplyCrateController controller;
    private final SupplyCrateType type;
    private final Function<Client, Location> locationSupplier;
    private final boolean announce;

    DeployCrateAbility(SupplyCrateController controller, SupplyCrateType type, String locationDescriber, Function<Client, Location> locationSupplier, boolean announce) {
        super("Deploy",
                "Order a " + type.getDisplayName() + " at " + locationDescriber + ".");
        this.controller = controller;
        this.type = type;
        this.locationSupplier = locationSupplier;
        this.announce = announce;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!actor.isPlayer() || actor.getClient() == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final Location location = locationSupplier.apply(actor.getClient());
        Preconditions.checkNotNull(location, "Location supplier returned null location");
        final Player player = (Player) actor.getEntity();
        final Location playerLocation = player.getLocation();

        if (type.isUnique() && controller.getSupplyCrates().stream().anyMatch(crate -> crate.getType() == type)) {
            UtilMessage.message(player, "Server", "A <red>" + type.getDisplayName() + "</red> is already active on this server!");
            new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.5f, 0.89f).play(player);
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);

        }

        final SupplyCrateDeployEvent event = UtilServer.callEvent(new SupplyCrateDeployEvent(player, type));
        if (event.isCancelled()) {
            new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.5f, 0.89f).play(player);
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final SupplyCrate supplyCrate = this.controller.spawnSupplyCrate(type, location, player);
        if (announce) {
            final Location spawnLocation = supplyCrate.getLocation();
            spawnLocation.getChunk().load(false);
            final int spawnX = spawnLocation.getBlockX();
            final int spawnZ = spawnLocation.getBlockZ();

            new SoundEffect(Sound.ITEM_GOAT_HORN_SOUND_1, 1.1f, Float.MAX_VALUE).play(playerLocation);
            new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 1.4f, Float.MAX_VALUE).play(playerLocation);

            UtilMessage.broadcast(Component.empty());
            UtilMessage.broadcast(Component.empty()
                    .append(Component.text("A", NamedTextColor.RED, TextDecoration.BOLD))
                    .appendSpace()
                    .append(Component.text(type.getDisplayName(), NamedTextColor.WHITE, TextDecoration.BOLD).font(SMALL_CAPS))
                    .appendSpace()
                    .append(Component.text("has been dispatched at", NamedTextColor.RED, TextDecoration.BOLD))
                    .appendSpace()
                    .append(Component.text(spawnX + ", " + spawnZ, NamedTextColor.WHITE, TextDecoration.BOLD))
            );
            UtilMessage.broadcast(Component.empty());
        } else {
            new SoundEffect(Sound.ITEM_GOAT_HORN_SOUND_1, 1.7f, 2f).play(playerLocation);
            new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 1.4f, 2f).play(playerLocation);
        }
        return InteractionResult.Success.ADVANCE;
    }
}
