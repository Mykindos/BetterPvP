package me.mykindos.betterpvp.shops.npc.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class Shopkeeper1NPC extends ModeledNPC implements Actor {

    /** Distance in front of the NPC where the showcase row is placed. */
    private static final double FORWARD_DISTANCE = 0.6;
    /** Height above the entity's bounding-box top where items hover. */
    private static final double HEIGHT_ABOVE_ENTITY = 1.01;
    /** Gap between adjacent showcase items, in blocks. */
    private static final double ITEM_SEPARATION = 0.15;
    /** How strongly edge items tilt inward, in degrees per block of lateral offset. */
    private static final float INWARD_CURVE_DEG_PER_BLOCK = 30f;
    /** Render scale applied to each ItemDisplay (0.4 = 40% of full size). */
    private static final float ITEM_SCALE = 0.6f;
    /** Concave-arc depth: edge items are pushed this many blocks further forward per block of offset. */
    private static final double ARC_DEPTH_PER_BLOCK = 0.2;

    private final ShopManager shopManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ActiveModel model;
    private final String shopName;

    public Shopkeeper1NPC(NPCFactory factory, Entity entity, String shopName, String shopkeeperName, String skinBlueprint, List<ItemStack> showcaseItems) {
        super(factory, entity);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.shopManager = plugin.getInjector().getInstance(ShopManager.class);
        this.clientManager = plugin.getInjector().getInstance(ClientManager.class);
        this.itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        this.shopName = shopName;

        this.model = ModelEngineAPI.createActiveModel("scene_market_1");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_table_1", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        setupShowcaseItems(showcaseItems);

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(shopName, NamedTextColor.YELLOW));
    }

    private void setupShowcaseItems(List<ItemStack> showcaseItems) {
        if (showcaseItems.isEmpty()) return;

        Location loc = entity.getLocation();
        int count = showcaseItems.size();
        double yawRad = Math.toRadians(loc.getYaw());

        // Direction vectors derived from the entity's yaw.
        // fwd = direction the NPC faces, right = 90° clockwise from fwd (NPC's right side).
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double baseY = loc.getY() + HEIGHT_ABOVE_ENTITY;

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
