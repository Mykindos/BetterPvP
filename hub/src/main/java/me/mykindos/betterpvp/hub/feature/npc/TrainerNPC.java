package me.mykindos.betterpvp.hub.feature.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

public class TrainerNPC extends ModeledNPC implements HubNPC {

    private final CooldownManager cooldownManager;

    TrainerNPC(HubNPCFactory factory, Entity entity, CooldownManager cooldownManager) {
        super(factory, entity);
        this.cooldownManager = cooldownManager;

        final Location tagLoc = entity.getLocation().add(0, 2.8, 0);
        attachToLifecycle(entity.getWorld().spawn(tagLoc, TextDisplay.class, display -> {
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
            display.text(Component.text("FFA Arena", NamedTextColor.RED, TextDecoration.BOLD));
        }));

        final ActiveModel model = ModelEngineAPI.createActiveModel("dummy");
        this.modeledEntity.addModel(model, true);
    }

    @Override
    public void act(Player runner) {
        if (!cooldownManager.use(runner, "TrainerNPC", 2L, false)) {
            return;
        }

        // todo: teleport player
        modeledEntity.getModel("dummy").orElseThrow().getAnimationHandler().playAnimation("hit", 0, 0, 1, false);
        new SoundEffect("emaginationfallenreaper", "custom.spell.rslash", 1f, 1f).play(runner);
        Particle.SWEEP_ATTACK.builder()
                .count(5)
                .location(getEntity().getLocation().add(0, 2, 0))
                .offset(0.7, 0.7, 0.7)
                .extra(0)
                .receivers(runner)
                .spawn();
        UtilMessage.message(runner, "NPC", "<red>Coming soon...");
    }
}
