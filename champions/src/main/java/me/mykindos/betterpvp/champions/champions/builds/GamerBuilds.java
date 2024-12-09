package me.mykindos.betterpvp.champions.champions.builds;

import lombok.Data;
import me.mykindos.betterpvp.core.components.champions.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
public class GamerBuilds {

    private final String uuid;
    private final List<RoleBuild> builds = new ArrayList<>();
    private final HashMap<String, RoleBuild> activeBuilds = new HashMap<>();

    public void clearActiveBuilds(){
        for (Role role : Role.values()) {
            activeBuilds.put(role.getName(), new RoleBuild(UUID.randomUUID().toString(), role, -1));
        }
    }

    public Optional<RoleBuild> getBuild(Role role, int id){
        return builds.stream().filter(build -> build.getRole() == role && build.getId() == id).findFirst();
    }

    public void setBuild(RoleBuild newRoleBuild, Role role, int id) {
        builds.replaceAll(roleBuild -> {
            if (roleBuild.getRole() == role && roleBuild.getId() == id){
                return newRoleBuild;
            }
            return roleBuild;
        });
    }
}
