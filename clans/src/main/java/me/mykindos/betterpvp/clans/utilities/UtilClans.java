package me.mykindos.betterpvp.clans.utilities;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class UtilClans {

    public static int[][] getClaimLayout(Player player, Clan clan) {

        List<ClanTerritory> territoryChunks = clan.getTerritory();

        // Logic to get the width and length of the 2d array.
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        for (ClanTerritory territoryChunk : territoryChunks) {
            Chunk c = UtilWorld.stringToChunk(territoryChunk.getChunk());
            maxX = Math.max(maxX, c.getX());
            maxZ = Math.max(maxZ, c.getZ());
            minX = Math.min(minX, c.getX());
            minZ = Math.min(minZ, c.getZ());
        }

        // Logic to map out the clans territory in a 2d array.
        int[][] territoryGrid = new int[Math.abs(maxX - minX + 1)][Math.abs(maxZ - minZ + 1)];

        for (ClanTerritory territoryChunk : territoryChunks) {
            Chunk c = UtilWorld.stringToChunk(territoryChunk.getChunk());
            if (!(c.getX() == player.getChunk().getX() && c.getZ() == player.getChunk().getZ())) {
                territoryGrid[c.getX() - minX][c.getZ() - minZ] = 1;
            }
        }

        return territoryGrid;
    }

    private static final int[] dx = {1, -1, 0, 0};
    private static final int[] dy = {0, 0, 1, -1};

    /**
     * @param layout The chunk layout of the territory
     * @return True if a claim is required to connect other claims together
     */
    public static boolean isClaimRequired(int[][] layout) {
        int n = layout.length;
        int m = layout[0].length;

        boolean[][] visited = new boolean[n][m];

        int startX = -1, startY = -1;
        outerLoop:
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (layout[i][j] == 1) {
                    startX = i;
                    startY = j;
                    break outerLoop;
                }
            }
        }

        if (startX == -1 || startY == -1) {
            return false;
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0];
            int y = curr[1];

            for (int k = 0; k < 4; k++) {
                int nx = x + dx[k];
                int ny = y + dy[k];
                if (nx >= 0 && nx < n && ny >= 0 && ny < m && !visited[nx][ny] && layout[nx][ny] == 1) {
                    visited[nx][ny] = true;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (layout[i][j] == 1 && !visited[i][j]) {
                    return true;
                }
            }
        }

        return false;
    }
}
