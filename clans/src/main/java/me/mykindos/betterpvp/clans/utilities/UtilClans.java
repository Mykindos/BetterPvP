package me.mykindos.betterpvp.clans.utilities;

import java.util.LinkedList;
import java.util.Queue;

public class UtilClans {
    public static boolean isClaimLegal(int[][] layout) {
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

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

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
