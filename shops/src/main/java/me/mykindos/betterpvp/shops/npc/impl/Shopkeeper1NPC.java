package me.mykindos.betterpvp.shops.npc.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.scene.npc.NPCFactory;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class Shopkeeper1NPC extends ModeledNPC implements Actor {

    private final ShopManager shopManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final String shopName;
    private final String shopkeeperName;
    private final String skinBlueprint;
    private final List<ItemStack> showcaseItems;
    private ActiveModel model;

    public Shopkeeper1NPC(NPCFactory factory, String shopName, String shopkeeperName, String skinBlueprint, List<ItemStack> showcaseItems) {
        super(factory);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.shopManager = plugin.getInjector().getInstance(ShopManager.class);
        this.clientManager = plugin.getInjector().getInstance(ClientManager.class);
        this.itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        this.shopName = shopName;
        this.shopkeeperName = shopkeeperName;
        this.skinBlueprint = skinBlueprint;
        this.showcaseItems = showcaseItems;
    }

    @Override
    protected void onInit() {
        super.onInit();
        this.model = ModelEngineAPI.createActiveModel("scene_market_1");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_table_1", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        setupShowcaseItems();

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(shopName, NamedTextColor.YELLOW));
    }

    private void setupShowcaseItems() {
        if (showcaseItems.isEmpty()) return;

        Location loc = getEntity().getLocation();
        int count = showcaseItems.size();
        double yawRad = Math.toRadians(loc.getYaw());

        // Direction vectors derived from the entity's yaw.
        // fwd = direction the NPC faces, right = 90° clockwise from fwd (NPC's right side).
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double baseY = loc.getY() + 1.01;

        for (int i = 0; i < count; i++) {
            // (i - (count-1)/2.0) centres the array symmetrically for both odd and even counts:
            //   odd  (e.g. 3): offsets → -0.15, 0, +0.15  (item at exact centre)
            //   even (e.g. 4): offsets → -0.225, -0.075, +0.075, +0.225  (gap at centre)
            double lateralOffset = (i - (count - 1) / 2.0) * 0.45;

            // Concave arc: items further from centre step slightly forward, creating a curve that
            // faces inward toward the player standing in front of the display.
            double distFromCenter = Math.abs(lateralOffset);
            double forwardDist = 1.1 - (Math.exp(distFromCenter) - 1.0) * 0.15;

            double x = loc.getX() + fwdX * forwardDist + rightX * lateralOffset;
            double z = loc.getZ() + fwdZ * forwardDist + rightZ * lateralOffset;
            Location displayLoc = new Location(loc.getWorld(), x, baseY, z, loc.getYaw(), 0);

            // Inward tilt: items at the edges rotate toward the centre so the display fans
            // slightly inward. Positive lateral offset (right side) → negative Y rotation (face left).
            Quaternionf rotation = new Quaternionf().rotateAxis((float) Math.toRadians(-90.0), new Vector3f(1, 0, 0));

            ItemStack item = showcaseItems.get(i);
            ItemDisplay display = loc.getWorld().spawn(displayLoc, ItemDisplay.class, d -> {
                d.setItemStack(item);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        rotation,
                        new Vector3f(0.4f),
                        new Quaternionf()
                ));
            });
            attachToLifecycle(display);
        }
    }

    @Override
    public void act(Player runner) {
        // Open shop
        this.shopManager.showShopMenu(runner, shopName, itemFactory, clientManager);

        // VFX
        this.model.getAnimationHandler().playAnimation("vendor_table_1_interact", 0.5, 3, 1.0, false);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
