package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.WeakHashMap;

public class CooldownComponent {

    private final char START_BANNER = '\uE025';
    private final char MIDDLE_BANNER = '\uE027';
    private final char END_BANNER = '\uE026';

    @Getter
    private final BossBar bossBar;
    private final WeakHashMap<SkillType, Component> components = new WeakHashMap<>();

    public CooldownComponent() {
        bossBar = BossBar.bossBar(Component.empty(),1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
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
            component = component.append(value).append(Component.translatable("space.4"));
        }
        bossBar.name(component);
    }

    private Component createNameplateComponent(SkillType type, double duration){

        NamedTextColor color = switch (type.ordinal()) {
            case 0 -> NamedTextColor.BLUE;
            case 1 -> NamedTextColor.RED;
            case 2 -> NamedTextColor.GREEN;
            case 3 -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };

        String tempText = "\uE028 " + duration + "s";
        int length = (int) Math.ceil((double) tempText.length() / 3);

        Component text = Component.text("\uE028", color).append(Component.text(" " + duration + "s", NamedTextColor.WHITE));

        Component nameplate = Component.text(START_BANNER).append(Component.translatable("space.-1"));
        for (int i = 0; i < length; i++){
            nameplate = nameplate.append(Component.text(MIDDLE_BANNER).append(Component.translatable("space.-1")));
        }
        nameplate = nameplate.append(Component.text(END_BANNER));


        return nameplate.append(Component.translatable("space.-" + getOffset(tempText, length)).append(text));
    }

    private int getOffset(String text, int length){
        int totalLength = text.chars().map(c -> c == '.' ? 2 : c == ' ' ? 4 : Character.isDigit(c) ? 6 : c == '\uE028' ? 9 : 0).sum();
        return totalLength + (int) Math.ceil((length - totalLength) / 2.0) + 32 + (text.length() % 2 == 1 ? 4 : -2);
    }
}
