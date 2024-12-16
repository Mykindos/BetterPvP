package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CooldownComponent {

    private static final char START_BANNER = '\uE025';
    private static final char MIDDLE_BANNER = '\uE027';
    private static final char END_BANNER = '\uE026';
    private static final char SWORD_ICON = '\uE029';
    private static final char AXE_ICON = '\uE030';
    private static final char BOW_ICON = '\uE031';
    private static final char PASSIVE_ICON = '\uE028';
    private static final int ICON_HEIGHT = 8;
    private static final int BACKGROUND_HEIGHT = 12;

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
        AtomicReference<Component> component = new AtomicReference<>(Component.empty());
        components.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Enum::ordinal)))
                .forEach(entry -> component.set(component.get().append(entry.getValue())));
        bossBar.name(component.get());
    }

    private Component createNameplateComponent(SkillType type, double duration){

        char icon = switch (type.ordinal()) {
            case 0 -> SWORD_ICON;
            case 1 -> AXE_ICON;
            case 2 -> BOW_ICON;
            default -> PASSIVE_ICON;
        };

        String tempText = icon + " " + duration + "s";
        int length = (int) Math.ceil((double) tempText.length() / 2);

        Component text = Component.text(icon).append(Component.text(" " + duration + "s", NamedTextColor.WHITE));

        Component nameplate = Component.text(START_BANNER).append(Component.translatable("space.-1"));
        for (int i = 0; i < length; i++){
            nameplate = nameplate.append(Component.text(MIDDLE_BANNER).append(Component.translatable("space.-1")));
        }
        nameplate = nameplate.append(Component.text(END_BANNER));

        //Get offset to center text on nameplate, add 2 to account for start and end banners
        int spacing = getOffset(tempText, length + 2);

        //4 pixel margin
        int margin = ((length + 2) * BACKGROUND_HEIGHT - spacing) + 4;

        return nameplate.append(Component.translatable("space.-" + spacing).append(text).append(Component.translatable("space." + margin)));
    }

    private int getOffset(String text, int length){
        int totalLength = text.chars()
                .map(this::getCharLengthInPixels)
                .sum() + 1;

        return totalLength + (((length  * BACKGROUND_HEIGHT) - totalLength) / 2);
    }

    private int getCharLengthInPixels(int c) {
        if (c == '.') return 2;
        if (c == ' ') return 4;
        if (Character.isDigit(c)) return 6;
        if(Character.isLetter(c)) return 6;
        if (c == PASSIVE_ICON || c == SWORD_ICON || c == AXE_ICON || c == BOW_ICON) return (ICON_HEIGHT + 1);
        return 0;
    }
}
