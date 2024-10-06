package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLoot;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@CustomLog
@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class ClansFishingListener implements Listener {

    private final ClanManager clanManager;
    private final WeaponManager weaponManager;

    @Inject
    public ClansFishingListener(ClanManager clanManager, WeaponManager weaponManager) {
        this.clanManager = clanManager;
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onCaughtFish(PlayerCaughtFishEvent event) {
        if (!(event.getLoot().getType() instanceof TreasureType treasureType)) return;

        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getHook().getLocation());
        if (clanOptional.isEmpty() || !clanOptional.get().getName().equalsIgnoreCase("Fields")) {
            ItemStack itemStack = new ItemStack(treasureType.getMaterial());
            itemStack.editMeta(meta -> meta.setCustomModelData(treasureType.getCustomModelData()));

            Optional<IWeapon> weaponOptional = weaponManager.getWeaponByItemStack(itemStack);
            if (weaponOptional.isPresent()) {
                IWeapon weapon = weaponOptional.get();
                if (weapon instanceof LegendaryWeapon) {
                    TreasureType newLoot = new TreasureType("legendary_stick");
                    newLoot.setMaterial(Material.STICK);
                    newLoot.setMinAmount(1);
                    newLoot.setMaxAmount(1);
                    newLoot.setCustomModelData(0);
                    newLoot.setFrequency(1);
                    event.setLoot(new FishingLoot() {
                        @Override
                        public FishingLootType getType() {
                            return null;
                        }

                        @Override
                        public void processCatch(PlayerCaughtFishEvent event) {
                            final ItemStack itemStack = new ItemStack(Material.DRAGON_HEAD);
                            Item item = (Item) event.getCaught();
                            item.setItemStack(itemStack);
                         }
                    });

                    UtilMessage.simpleMessage(event.getPlayer(), "Fishing", "You would have caught a Legendary, but you were not at Fields!");
                    UtilMessage.simpleMessage(event.getPlayer(), "Fishing", "Have this instead...");
                    log.info("{} ({}) would have caught a legendary while fishing, but they were not at fields!", event.getPlayer().getName(), event.getPlayer().getUniqueId()).submit();
                }
            }
        }
    }
}
