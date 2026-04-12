package me.mykindos.betterpvp.shops.npc.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionHouseNPC extends ModeledNPC implements Actor {

    private final AuctionManager auctionManager;
    private final ActiveModel model;

    public AuctionHouseNPC(NPCFactory factory, Entity entity, String roleName, String shopkeeperName, String skinBlueprint) {
        super(factory, entity);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.auctionManager = plugin.getInjector().getInstance(AuctionManager.class);

        this.model = ModelEngineAPI.createActiveModel("scene_market_4");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_books", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(roleName, NamedTextColor.YELLOW));
    }

    @Override
    public void act(Player runner) {
        // Open
        new AuctionHouseMenu(auctionManager).show(runner);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
