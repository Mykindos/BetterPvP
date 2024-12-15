package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.config.Config;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.WeakHashMap;

public class CooldownComponent {

    private final char START_BANNER = '\uE025';
    private final char MIDDLE_BANNER = '\uE027';
    private final char END_BANNER = '\uE026';
    private final char SWORD_ICON = '\uE029';
    private final char AXE_ICON = '\uE030';
    private final char BOW_ICON = '\uE031';
    private final char PASSIVE_ICON = '\uE028';

    private final int iconHeight = 12;

    @Getter
    private final BossBar bossBar;
    private final WeakHashMap<SkillType, Component> components = new WeakHashMap<>();

    public CooldownComponent() {
        bossBar = BossBar.bossBar(Component.empty(),1.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
    }

    public void addComponent (SkillType type, double duration){
        components.put(type, createNameplateComponent(type, duration));
        updateBossBar();
    }

    public void removeComponent(SkillType type){
        components.remove(type);
        updateBossBar();
    }

    public void updateComponent(SkillType type, double duration){
        if(components.containsKey(type)){
            components.put(type, createNameplateComponent(type, duration));
        }
        updateBossBar();
    }

    public void updateBossBar(){
        Component component = Component.empty();
        for(Component value : components.values()){
            component = component.append(value);
        }
        bossBar.name(component);
    }

    private Component createNameplateComponent(SkillType type, double duration){

        char icon = switch (type.ordinal()) {
            case 0 -> SWORD_ICON;
            case 1 -> AXE_ICON;
            case 2 -> BOW_ICON;
            default -> PASSIVE_ICON;
        };

        String tempText = icon + " " + duration + "s";
        int length = (int) Math.ceil((double) tempText.length() / 3);

        Component text = Component.text(icon).append(Component.text(" " + duration + "s", NamedTextColor.WHITE));

        Component nameplate = Component.text(START_BANNER).append(Component.translatable("space.-1"));
        for (int i = 0; i < length; i++){
            nameplate = nameplate.append(Component.text(MIDDLE_BANNER).append(Component.translatable("space.-1")));
        }
        nameplate = nameplate.append(Component.text(END_BANNER));

        int spacing = getOffset(tempText, length);
        int margin = spacing / 4 + length;

        return nameplate.append(Component.translatable("space.-" + spacing).append(text).append(Component.translatable("space." + margin )));
    }

    private int getOffset(String text, int length){
        int totalLength = text.chars()
                .map(c -> c == '.' ? 2
                        : c == ' ' ? 4
                        : Character.isDigit(c) ? 6
                        : c == PASSIVE_ICON || c == SWORD_ICON || c == AXE_ICON || c == BOW_ICON ? (iconHeight + 1)
                        : 0)
                .sum();
        int offset = text.length() % 2 == 0 ? 0 : -1;
        return totalLength + ((length * iconHeight) / 2) + offset;
    }
}
