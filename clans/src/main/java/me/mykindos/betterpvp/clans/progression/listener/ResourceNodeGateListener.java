package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.clans.world.resource.event.ResourceHarvestAttemptEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enforces the profession-level gate on a resource node — the decoupled bridge that {@link ResourceHarvestAttemptEvent}
 * documents. It listens to the attempt event (fired by {@code ResourceNodeManager}) and cancels it, with a denial
 * message, when the player's level in the node's profession is below the node's required level. Without this listener
 * the attempt fires into the void and harvesting is ungated, so it is loaded only when Progression is present
 * (discovered by {@link ProgressionAdapter}) — fail-open if Progression is absent, exactly as the event specifies.
 * <p>
 * Administrating players bypass the gate so they can build and test nodes in-world, mirroring {@code ResourceNodeRule}.
 */
@Singleton
@CustomLog
@BPvPListener
@PluginAdapter("Progression")
public class ResourceNodeGateListener implements Listener {

    private final ClientManager clientManager;
    private final MiningHandler miningHandler;
    private final WoodcuttingHandler woodcuttingHandler;
    private final FishingHandler fishingHandler;

    @Inject
    public ResourceNodeGateListener(ProgressionAdapter adapter, ClientManager clientManager) {
        this.clientManager = clientManager;
        this.miningHandler = adapter.getProgression().getInjector().getInstance(MiningHandler.class);
        this.woodcuttingHandler = adapter.getProgression().getInjector().getInstance(WoodcuttingHandler.class);
        this.fishingHandler = adapter.getProgression().getInjector().getInstance(FishingHandler.class);
    }

    @EventHandler
    public void onAttempt(ResourceHarvestAttemptEvent event) {
        final String profession = event.getProfession();
        if (profession == null) {
            return;
        }
        final ProfessionHandler handler = handlerFor(profession);
        if (handler == null) {
            return;
        }
        final Player player = event.getPlayer();
        if (clientManager.search().online(player).isAdministrating()) {
            return;
        }
        final ProfessionData data = handler.getProfessionData(player.getUniqueId());
        if (data == null) {
            return;
        }
        final int level = data.getLevelFromExperience(data.getExperience());
        final int required = event.getRequiredLevel();
        if (level < required) {
            event.setCancelled(true);
            event.setDenialMessage(UtilMessage.deserialize(
                    "<red>You need <yellow>%s</yellow> level <yellow>%d</yellow> to harvest this <gray>(you are level %d)</gray>.",
                    profession, required, level));
        }
    }

    private @Nullable ProfessionHandler handlerFor(@NotNull String profession) {
        return switch (profession.toLowerCase(Locale.ROOT)) {
            case "mining" -> miningHandler;
            case "woodcutting" -> woodcuttingHandler;
            case "fishing" -> fishingHandler;
            default -> null;
        };
    }
}
