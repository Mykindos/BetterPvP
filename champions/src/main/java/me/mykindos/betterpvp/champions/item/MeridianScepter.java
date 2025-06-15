package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.BlackHoleAbility;
import me.mykindos.betterpvp.champions.item.ability.MeridianBeamAbility;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = true)
public class MeridianScepter extends WeaponItem implements Listener, ReloadHook {

    private static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Material.MUSIC_DISC_CHIRP.key())
            .customModelData(1)
            .build()
            .get();

    private final BlackHoleAbility blackHoleAbility;
    private final MeridianBeamAbility meridianBeamAbility;

    @Inject
    private MeridianScepter(Champions champions, 
                           BlackHoleAbility blackHoleAbility, 
                           MeridianBeamAbility meridianBeamAbility) {
        super(champions, "Meridian Scepter", model, ItemRarity.LEGENDARY);
        this.blackHoleAbility = blackHoleAbility;
        this.meridianBeamAbility = meridianBeamAbility;
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(blackHoleAbility)
                .ability(meridianBeamAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        
        // Black Hole configuration
        blackHoleAbility.setRadius(config.getConfig("blackHoleRadius", 0.5, Double.class));
        blackHoleAbility.setSpeed(config.getConfig("blackHoleSpeed", 3.0, Double.class));
        blackHoleAbility.setHitbox(config.getConfig("blackHoleHitbox", 0.5, Double.class));
        blackHoleAbility.setPullStrength(config.getConfig("blackHolePullStrength", 0.12, Double.class));
        blackHoleAbility.setPullRadius(config.getConfig("blackHolePullRadius", 3.5, Double.class));
        blackHoleAbility.setAliveSeconds(config.getConfig("blackHoleAliveSeconds", 1.3, Double.class));
        blackHoleAbility.setExpandSeconds(config.getConfig("blackHoleExpandSeconds", 0.75, Double.class));
        blackHoleAbility.setTravelSeconds(config.getConfig("blackHoleTravelSeconds", 2.0, Double.class));
        blackHoleAbility.setCooldown(config.getConfig("blackHoleCooldown", 10.0, Double.class));
        
        // Meridian Beam configuration
        meridianBeamAbility.setCooldown(config.getConfig("beamCooldown", 1.0, Double.class));
        meridianBeamAbility.setDamage(config.getConfig("beamDamage", 4.0, Double.class));
        meridianBeamAbility.setSpeed(config.getConfig("beamSpeed", 4.0, Double.class));
        meridianBeamAbility.setHitbox(config.getConfig("beamHitbox", 0.5, Double.class));
        meridianBeamAbility.setTravelSeconds(config.getConfig("beamTravelSeconds", 0.3, Double.class));
    }

    @UpdateEvent(priority = 100)
    public void processBlackHoles() {
        blackHoleAbility.processBlackHoles();
    }

    @UpdateEvent
    public void processBeams() {
        meridianBeamAbility.processBeams();
    }
} 