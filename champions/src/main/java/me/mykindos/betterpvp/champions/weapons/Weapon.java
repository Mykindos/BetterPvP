package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Singleton
public abstract class Weapon implements IWeapon {

    private final Material material;
    private final Component name;
    private final List<Component> lore;

    public Weapon(Material material, Component name){
        this(material, name, new ArrayList<>());
    }

}
