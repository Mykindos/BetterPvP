package me.mykindos.betterpvp.shops.npc.impl.reforger;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasRegistry;
import me.mykindos.betterpvp.core.item.reforging.GuiReforge;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReforgerNPC extends ModeledNPC implements Actor {

    private final PurityReforgeBiasRegistry biasRegistry;
    private final ItemFactory itemFactory;
    private final ActiveModel model;

    public ReforgerNPC(NPCFactory factory, Entity entity, String roleName, String shopkeeperName, String skinBlueprint) {
        super(factory, entity);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.biasRegistry = plugin.getInjector().getInstance(PurityReforgeBiasRegistry.class);
        this.itemFactory = plugin.getInjector().getInstance(ItemFactory.class);

        this.model = ModelEngineAPI.createActiveModel("scene_blacksmith_1");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "idle", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(roleName, NamedTextColor.YELLOW));

        this.addBehavior(new ReforgerScriptBehavior(this, plugin, this.model));
    }

    @Override
    public void act(Player runner) {
        // Open
        new GuiReforge(runner, itemFactory, biasRegistry).show(runner);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }

}
