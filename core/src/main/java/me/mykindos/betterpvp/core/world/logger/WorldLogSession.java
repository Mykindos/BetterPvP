package me.mykindos.betterpvp.core.world.logger;

import lombok.Data;
import me.mykindos.betterpvp.core.database.query.Statement;

import java.util.List;

@Data
public class WorldLogSession {

    public Statement statement;
    public WorldLogQueryType queryType = WorldLogQueryType.BLOCK;
    public int pages;
    public int currentPage = 1;
    public List<WorldLog> data;

}
