package me.mykindos.betterpvp.core.components.champions;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;

@Data
@Slf4j
public class Role {
    @Getter(AccessLevel.NONE)
    private final String key;

    private boolean enabled;

    private String prefix;

    private TextColor color;

    private Double maxHealth;
    private Double arrowDamage;

    private Set<ISkill> swordSkills = new HashSet<>(9);
    private Set<ISkill> axeSkills = new HashSet<>(9);
    private Set<ISkill> passiveA = new HashSet<>(9);
    private Set<ISkill> passiveB = new HashSet<>(9);
    private Set<ISkill> global = new HashSet<>(9);
    private Set<ISkill> bow = new HashSet<>(9);

    private boolean dealKnockback;
    private boolean takeKnockback;
    private boolean takeFallDamage;

    private PotionEffect roleBuff = null;

    private Sound damageSound;
    private float damageVolume;
    private float damagePitch;

    private Material[] armor = new Material[4];

    public void loadSkills(ExtendedYamlConfiguration config, Reflections reflections, BPvPPlugin plugin) {
        String path = "class." + key + ".skills.";
        List<String> swordList = config.getOrSaveStringList(path + "sword", List.of("HiltSmash", "FleshHook"));
        List<String> axeList = config.getOrSaveStringList(path + "axe", List.of("BullsCharge", "SeismicSlam"));
        List<String> passiveAList = config.getOrSaveStringList(path + "passiveA", List.of("Bloodlust", "Cleave"));
        List<String> passiveBList = config.getOrSaveStringList(path + "passiveB", List.of("Deflection", "Stampede"));
        List<String> globalList = config.getOrSaveStringList(path + "global", List.of("BreakFall", "FastRecovery", "Swim"));
        List<String> bowList = config.getOrSaveStringList(path + "bow", List.of());
        Set<Class<? extends ISkill>> skillClasses = reflections.getSubTypesOf(ISkill.class);
        skillClasses.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        skillClasses.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));

        for (var clazz : skillClasses) {
            ISkill skill = plugin.getInjector().getInstance(clazz);
            if (skill.getType() == SkillType.SWORD && swordList.contains(skill.getName())) {
                swordSkills.add(skill);
                skill.addClass(this);
                continue;
            }
            if (skill.getType() == SkillType.AXE && axeList.contains(clazz.getName())) {
                axeSkills.add(skill);
                skill.addClass(this);
                continue;
            }
            if (skill.getType() == SkillType.PASSIVE_A && passiveAList.contains(clazz.getName())) {
                passiveA.add(skill);
                skill.addClass(this);
                continue;
            }
            if (skill.getType() == SkillType.PASSIVE_B && passiveBList.contains(clazz.getName())) {
                passiveB.add(skill);
                skill.addClass(this);
                continue;
            }
            if (skill.getType() == SkillType.GLOBAL && globalList.contains(clazz.getName())) {
                global.add(skill);
                skill.addClass(this);
                continue;
            }
            if (skill.getType() == SkillType.BOW && bowList.contains(clazz.getName())) {
                bow.add(skill);
                skill.addClass(this);
                continue;
            }
        }


    }

    public void loadConfig(ExtendedYamlConfiguration config) {
        log.error("key" + key);
        String path = "class." + key + ".";
        log.error("path" + path);
        enabled = config.getOrSaveBoolean(path + "enabled", true);
        log.error("enabled" + enabled);
        prefix = config.getOrSaveString(path + "prefix", key.substring(0, 1));

        int R = config.getOrSaveInt(path + "ColorR", 0);
        log.error("R" + R);
        int G = config.getOrSaveInt(path + "ColorG", 0);
        int B = config.getOrSaveInt(path+ "ColorB", 0);

        color = TextColor.color(R, G, B);

        maxHealth = config.getOrSaveObject(path + "maxHealth", 20.0, Double.class);
        arrowDamage = config.getOrSaveObject(path + "arrowDamage", 0.0, Double.class);

        dealKnockback = config.getOrSaveBoolean(path + "dealKnockback", true);
        takeKnockback = config.getOrSaveBoolean(path + "takeKnockback", true);
        takeFallDamage = config.getOrSaveBoolean(path + "takeFallDamage", true);

        String buffKey = config.getOrSaveString(path + "buffname", "NONE");
        int buffPower = config.getOrSaveInt(path + "buffstrength", 0);
        if (!buffKey.equals("NONE")) {
            try {
                this.roleBuff = new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(buffKey)), -1, buffPower);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid buff key: " + buffKey, e);
            }
        }

        String soundKey = config.getOrSaveString(path + "damageSound", "ENTITY_BLAZE_HURT");
        if (soundKey == null) {
            throw new IllegalArgumentException("Sound key cannot be null!");
        }
        try {
            this.damageSound = Sound.valueOf(soundKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sound key: " + soundKey, e);
        }

        damageVolume = Float.parseFloat(config.getOrSaveString(path + "soundvolume", "1.0"));
        damagePitch = Float.parseFloat(config.getOrSaveString(path + "soundpitch", "0.7"));

        String[] armorTypes = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < armorTypes.length; i++) {
            final String type = armorTypes[i];
            final String materialKey = config.getOrSaveString(path + type, "LEATHER_" + type.toUpperCase());
            if (materialKey == null) {
                throw new IllegalArgumentException(type.toUpperCase() + " material key cannot be null!");
            }
            try {
                this.armor[i] = Material.valueOf(materialKey.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid material key: " + materialKey, e);
            }
        }
    }

    public boolean hasSkill (SkillType type, String name) {
        switch (type) {
            case SWORD -> {
                return swordSkills.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }
            case AXE -> {
                return axeSkills.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }
            case PASSIVE_A -> {
                return passiveA.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }

            case PASSIVE_B -> {
                return passiveB.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }

            case GLOBAL-> {
                return global.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }

            case BOW -> {
                return bow.stream().anyMatch(o -> o.getName().equalsIgnoreCase(name));
            }
        }
        return false;
    }

    public String getName() {
        return key;
    }

    public Material getHelmet() {
        return armor[0];
    }

    public Material getChestplate() {
        return armor[1];
    }

    public Material getLeggings() {
        return armor[2];
    }

    public Material getBoots() {
        return armor[3];
    }
}
