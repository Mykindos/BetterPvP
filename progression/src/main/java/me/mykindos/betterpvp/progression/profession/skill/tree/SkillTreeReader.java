package me.mykindos.betterpvp.progression.profession.skill.tree;

import java.io.File;

public interface SkillTreeReader {

    SkillTreeLayout read(File file) throws Exception;

    /** Picks the right reader by file extension: .drawio/.xml → draw.io, otherwise YAML. */
    static SkillTreeReader forFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".drawio") || name.endsWith(".xml")) {
            return new DrawioSkillTreeReader();
        }
        return new YamlSkillTreeReader();
    }
}
