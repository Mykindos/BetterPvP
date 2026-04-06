package me.mykindos.betterpvp.hub.feature.npc;

import com.google.common.base.Preconditions;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.framework.ClansServerType;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.model.Ticked;
import me.mykindos.betterpvp.hub.feature.menu.ServerTypeMenu;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class InstanceSelectorNPC extends ModeledNPC implements HubNPC {

    private final ClansServerType serverType;
    private final NetworkPlayerCountService networkPlayerCountService;
    private final HubQueueStatusRegistry queueStatusRegistry;
    private final OrchestrationGateway orchestrationGateway;

    public InstanceSelectorNPC(NPCFactory factory, Entity entity, Component tag, ClansServerType serverType,
                               NetworkPlayerCountService networkPlayerCountService, HubQueueStatusRegistry queueStatusRegistry,
                               OrchestrationGateway orchestrationGateway) {
        super(factory, entity);
        this.serverType = serverType;
        this.networkPlayerCountService = networkPlayerCountService;
        this.queueStatusRegistry = queueStatusRegistry;
        this.orchestrationGateway = orchestrationGateway;

        final Location tagLoc = entity.getLocation().add(0, 3, 0);
        attachToLifecycle(entity.getWorld().spawn(tagLoc, TextDisplay.class, display -> {
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.text(tag);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(1.5f),
                    new AxisAngle4f()
            ));
        }));

        final ActiveModel model = ModelEngineAPI.createActiveModel("roman_soldier");
        model.setScale(0.9);
        getModeledEntity().addModel(model, true);
    }

    @Override
    public void act(Player runner) {
        new ServerTypeMenu(networkPlayerCountService, serverType, queueStatusRegistry, orchestrationGateway).show(runner);
    }

    public static class Featured extends InstanceSelectorNPC implements Ticked {

        private final Component title;
        private final String gradientColors;

        public Featured(NPCFactory factory, Entity entity, Component title, TextColor[] gradient, ClansServerType serverType,
                        NetworkPlayerCountService networkPlayerCountService, HubQueueStatusRegistry queueStatusRegistry,
                        OrchestrationGateway orchestrationGateway) {
            super(factory, entity, Component.empty(), serverType, networkPlayerCountService, queueStatusRegistry, orchestrationGateway);
            Preconditions.checkArgument(gradient.length >= 2, "Gradient must have at least 2 colors");
            this.title = title;

            // build gradient
            StringBuilder gradientBuilder = new StringBuilder();
            for (TextColor color : gradient) {
                gradientBuilder.append(":");
                gradientBuilder.append(color.asHexString());
            }
            this.gradientColors = gradientBuilder.toString();

            // resize model
            getModeledEntity().getModel("roman_soldier").orElseThrow().setScale(1.2f);
            final Entity display = attached.getFirst();
            final Location location = display.getLocation();
            location.setY(attached.getFirst().getLocation().getY() + 0.7);
            display.teleport(location);
        }

        @Override
        public void tick() {
            // Get tag and rainbow
            final long time = System.currentTimeMillis();
            final TextDisplay nameTag = (TextDisplay) attached.getFirst();

            final float phase = (float) Math.sin(time / 500d);
            final String title = PlainTextComponentSerializer.plainText().serialize(this.title);
            final Component titleComponent = MiniMessage.miniMessage()
                    .deserialize("<gradient" + gradientColors + ":" + phase + ">" + title + "</gradient>")
                    .decorate(TextDecoration.BOLD);
            final Component subtitleComponent = Component.text("Featured", NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false);
            nameTag.text(titleComponent.appendNewline().append(subtitleComponent));

            // Play particles
            final Location location = entity.getLocation();
            final List<Player> viewers = new ArrayList<>(Bukkit.getOnlinePlayers());

//            final double angleValue = Math.toRadians(time / 10d % 360);
//            final double x = Math.cos(angleValue) * 1.5;
//            final double z = Math.sin(angleValue) * 1.5;
//            final double yAngle = Math.toRadians(time / 30d % 360);
//            final double y = Math.sin(yAngle) * 1.5 + 1.5;
//
//            Particle.HAPPY_VILLAGER.builder()
//                    .count(1)
//                    .location(location.clone().add(x, y, z))
//                    .extra(0)
//                    .receivers(viewers)
//                    .spawn();
//            Particle.HAPPY_VILLAGER.builder()
//                    .count(1)
//                    .location(location.clone().add(-x, y, -z))
//                    .extra(0)
//                    .receivers(viewers)
//                    .spawn();

            if (Bukkit.getCurrentTick() % 2 == 0) {
                Particle.END_ROD.builder()
                        .count(1)
                        .location(location.clone().add(0, 1.5, 0))
                        .extra(0.1)
                        .receivers(viewers)
                        .spawn();
            }
        }
    }

}
