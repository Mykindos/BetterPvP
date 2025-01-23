package me.mykindos.betterpvp.hub.feature.sidebar;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.purchase.PurchaseModule;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Works only in Mineplex
 */
@SuppressWarnings("ALL")
public class MineplexSidebarBuilder implements HubSidebarBuilder {

    private final PurchaseModule purchaseModule = MineplexModuleManager.getRegisteredModule(PurchaseModule.class);

    @Override
    public void build(SidebarBuildEvent event) {
        final SidebarComponent.Builder builder = event.getBuilder();
        final Player player = Objects.requireNonNull(event.getGamer().getPlayer());
        final CompletableFuture<Long> balanceFuture = purchaseModule.getCrownBalance(player);

        builder.addBlankLine();
        builder.addStaticLine(Component.text("Crowns", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            if (balanceFuture.isDone()) {
                return Component.text(balanceFuture.resultNow(), NamedTextColor.WHITE);
            } else {
                return retrievingComponent;
            }
        });

        builder.addBlankLine();
        builder.addStaticLine(Component.text("Online", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> {
            // FIXME
            //  This is not supported by Mineplex yet
            return Component.text(player.getServer().getOnlinePlayers().size(), NamedTextColor.WHITE);
        });
        builder.addBlankLine();
    }
}
