package me.mykindos.betterpvp.champions.champions.builds.menus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

@Singleton
public class ClassSelectionMenu extends AbstractGui implements Windowed {

    @Inject
    public ClassSelectionMenu(BuildManager buildManager, ChampionsSkillManager skillManager, ArmourManager armourManager) {
        super(9, 3);

        int[] slots = new int[] {10, 11, 12, 14, 15, 16};
        final Iterator<Role> iterator = Arrays.stream(Role.values()).iterator();
        for (int slot : slots) {
            final Role role = iterator.next();
            setItem(slot, new ClassSelectionButton(buildManager, skillManager, role, armourManager, this));
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Pick a Class");
    }
}
