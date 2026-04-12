package me.mykindos.betterpvp.hub.feature.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class ComingSoonNPC extends ModeledNPC implements HubNPC {

    ComingSoonNPC(HubNPCFactory factory) {
        super(factory);
    }

    @Override
    protected void onInit() {
        super.onInit();

        final Location tagLoc = getEntity().getLocation().add(0, 2.8, 0);
        attachToLifecycle(getEntity().getWorld().spawn(tagLoc, TextDisplay.class, display -> {
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(1.3f),
                    new AxisAngle4f()
            ));
            display.text(Component.text("Coming Soon...", NamedTextColor.YELLOW, TextDecoration.ITALIC));
        }));

        final ActiveModel model = ModelEngineAPI.createActiveModel("npc_guard");
        getModeledEntity().addModel(model, true);
    }

    @Override
    public void act(Player runner) {
        // Intentionally passive.
    }
}
