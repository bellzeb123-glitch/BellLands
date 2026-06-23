package pl.bell.lands.integration;

import pl.bell.lands.model.Land;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Laczy sasiadujace chunki w STREFY (spojne grupy) per wlasciciel/swiat i liczy obrys
 * scalonej strefy. Dzieki temu np. 14 polaczonych chunkow to 1 strefa, a nie 14 claimow.
 */
public final class ClaimZoneBuilder {

    public record Zone(UUID owner, String world, int chunkCount,
                       double centerX, double centerZ, double[][] ring, int[][] chunks) {}

    private ClaimZoneBuilder() {}

    public static List<Zone> compute(Collection<Land> lands) {
        // Grupowanie chunkow wg (owner, world)
        Map<String, Map<Long, int[]>> groups = new LinkedHashMap<>();
        Map<String, UUID> ownerOf = new HashMap<>();
        Map<String, String> worldOf = new HashMap<>();
        for (Land l : lands) {
            String key = l.getOwner() + "|" + l.getWorldName();
            groups.computeIfAbsent(key, k -> new HashMap<>())
                  .put(cell(l.getChunkX(), l.getChunkZ()), new int[]{l.getChunkX(), l.getChunkZ()});
            ownerOf.putIfAbsent(key, l.getOwner());
            worldOf.putIfAbsent(key, l.getWorldName());
        }

        List<Zone> zones = new ArrayList<>();
        for (var e : groups.entrySet()) {
            Map<Long, int[]> cells = e.getValue();
            Set<Long> remaining = new HashSet<>(cells.keySet());
            while (!remaining.isEmpty()) {
                // BFS po sasiadach (4-kierunkowo) -> jedna spojna strefa
                Long startKey = remaining.iterator().next();
                List<int[]> component = new ArrayList<>();
                ArrayDeque<Long> queue = new ArrayDeque<>();
                queue.add(startKey); remaining.remove(startKey);
                while (!queue.isEmpty()) {
                    Long ck = queue.poll();
                    int[] c = cells.get(ck);
                    component.add(c);
                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        Long nk = cell(c[0] + d[0], c[1] + d[1]);
                        if (remaining.remove(nk)) queue.add(nk);
                    }
                }
                zones.add(buildZone(ownerOf.get(e.getKey()), worldOf.get(e.getKey()), component));
            }
        }
        return zones;
    }

    private static Zone buildZone(UUID owner, String world, List<int[]> cellsList) {
        double sumX = 0, sumZ = 0;
        for (int[] c : cellsList) { sumX += (c[0] << 4) + 8; sumZ += (c[1] << 4) + 8; }
        double[][] ring = ringFor(cellsList.toArray(new int[0][]));
        int[][] chunks = cellsList.toArray(new int[0][]);
        return new Zone(owner, world, cellsList.size(),
                sumX / cellsList.size(), sumZ / cellsList.size(), ring, chunks);
    }

    /** Obrys scalonej grupy chunków (np. dla nazwanej działki Pro). chunks = pary [chunkX,chunkZ]. */
    public static double[][] ringFor(int[][] chunks) {
        Set<Long> set = new HashSet<>();
        for (int[] c : chunks) set.add(cell(c[0], c[1]));
        Map<Long, List<long[]>> adj = new HashMap<>();
        for (int[] c : chunks) {
            int x0 = c[0] << 4, z0 = c[1] << 4, x1 = x0 + 16, z1 = z0 + 16;
            if (!set.contains(cell(c[0]-1, c[1]))) edge(adj, x0, z0, x0, z1);
            if (!set.contains(cell(c[0]+1, c[1]))) edge(adj, x1, z0, x1, z1);
            if (!set.contains(cell(c[0], c[1]-1))) edge(adj, x0, z0, x1, z0);
            if (!set.contains(cell(c[0], c[1]+1))) edge(adj, x0, z1, x1, z1);
        }
        return traceLargestRing(adj);
    }

    /** Sledzi krawedzie w pierscienie i zwraca najwiekszy (zewnetrzny obrys). */
    private static double[][] traceLargestRing(Map<Long, List<long[]>> adj) {
        Set<String> usedEdges = new HashSet<>();
        List<List<long[]>> rings = new ArrayList<>();
        for (Long start : adj.keySet()) {
            for (long[] nb : adj.get(start)) {
                if (usedEdges.contains(edgeKey(start, key(nb[0], nb[1])))) continue;
                List<long[]> ring = new ArrayList<>();
                long curK = start;
                long[] cur = unkey(start);
                long prevK = -1;
                ring.add(cur);
                while (true) {
                    long[] next = null; long nextK = -1;
                    for (long[] cand : adj.getOrDefault(curK, List.of())) {
                        long candK = key(cand[0], cand[1]);
                        if (candK == prevK) continue;
                        if (usedEdges.contains(edgeKey(curK, candK))) continue;
                        next = cand; nextK = candK; break;
                    }
                    if (next == null) break;
                    usedEdges.add(edgeKey(curK, nextK));
                    ring.add(next);
                    prevK = curK; curK = nextK;
                    if (nextK == start) break;
                }
                if (ring.size() >= 4) rings.add(ring);
            }
        }
        List<long[]> best = null;
        for (List<long[]> r : rings) if (best == null || r.size() > best.size()) best = r;
        if (best == null) return new double[0][];
        double[][] out = new double[best.size()][2];
        for (int i = 0; i < best.size(); i++) { out[i][0] = best.get(i)[0]; out[i][1] = best.get(i)[1]; }
        return out;
    }

    private static void edge(Map<Long, List<long[]>> adj, int x1, int z1, int x2, int z2) {
        adj.computeIfAbsent(key(x1, z1), k -> new ArrayList<>()).add(new long[]{x2, z2});
        adj.computeIfAbsent(key(x2, z2), k -> new ArrayList<>()).add(new long[]{x1, z1});
    }

    private static long cell(int cx, int cz) { return ((long) cx << 32) | (cz & 0xffffffffL); }
    private static long key(long x, long z) { return (x << 32) | (z & 0xffffffffL); }
    private static long[] unkey(long k) { return new long[]{(int)(k >> 32), (int)(k & 0xffffffffL)}; }
    private static String edgeKey(long a, long b) { return a < b ? a + ":" + b : b + ":" + a; }
}
