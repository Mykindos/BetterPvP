package me.mykindos.betterpvp.clans.world.navigation;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class NavigatorNPC extends ModeledNPC implements Actor {

    private final List<Island> destinations;
    private ActiveModel model;

    public NavigatorNPC(SceneObjectFactory factory, List<Island> destinations) {
        super(factory);
        this.destinations = destinations;
    }

    @Override
    protected void onInit() {
        super.onInit();
        this.model = ModelEngineAPI.createActiveModel("scene_market_1");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_stand_2", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint("skin_navigator"));

        BoneTagAnchor.addNameplate(this,
                this.model,
                "head",
                "Nigel",
                Component.text("Navigator", NamedTextColor.YELLOW));
    }

    @Override
    public void act(Player runner) {
        // Open
        new NavigationMenu(destinations).show(runner);

        // VFX
        this.model.getAnimationHandler().playAnimation("vendor_stand_1_interact", 0.2, 0.1, 1.0, false);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
