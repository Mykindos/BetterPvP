package me.mykindos.betterpvp.shops.npc.impl.attuner;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.attunement.GuiAttunement;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistributionRegistry;
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

public class AttunerNPC extends ModeledNPC implements Actor {

    private final RuneSlotDistributionRegistry runeDistributionRegistry;
    private final ItemFactory itemFactory;
    private final ActiveModel model;

    public AttunerNPC(NPCFactory factory, Entity entity, String roleName, String shopkeeperName, String skinBlueprint) {
        super(factory, entity);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.runeDistributionRegistry = plugin.getInjector().getInstance(RuneSlotDistributionRegistry.class);
        this.itemFactory = plugin.getInjector().getInstance(ItemFactory.class);

        this.model = ModelEngineAPI.createActiveModel("scene_blacksmith_5_interactive");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "idle", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(roleName, NamedTextColor.YELLOW));

        this.addBehavior(new AttunerScriptBehavior(this, plugin, this.model));
    }

    @Override
    public void act(Player runner) {
        // Open
        new GuiAttunement(runner, itemFactory, runeDistributionRegistry).show(runner);

        // VFX
        this.model.getAnimationHandler().playAnimation("interact", 0.5, 3, 1.2, false);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
