package pl.bell.lands.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.ServicePriority;
import pl.bell.lands.BellLands;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.model.Land;
import pl.bell.suite.api.BellModule;
import pl.bell.suite.api.MapFilter;
import pl.bell.suite.api.MapMarker;
import pl.bell.suite.api.Stat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Most BellLands -> panel BellSuite. Tylko ODCZYT z istniejacych managerow — nie zmienia
 * logiki pluginu. Ladowany leniwie tylko gdy BellSuite jest obecny (klasy API z jara BellSuite).
 */
public final class BellSuiteModule implements BellModule {

    private final LandManager lands;
    private final WarpManager warps;

    private BellSuiteModule(LandManager lands, WarpManager warps) {
        this.lands = lands;
        this.warps = warps;
    }

    /** Rejestruje modul w ServicesManager — BellSuite odkryje go automatycznie. */
    public static void register(BellLands plugin, LandManager lands, WarpManager warps) {
        Bukkit.getServicesManager().register(
                BellModule.class, new BellSuiteModule(lands, warps), plugin, ServicePriority.Normal);
    }

    @Override public String id() { return "belllands"; }
    @Override public String displayName() { return "BellLands"; }
    @Override public String icon() { return "map-pin"; }
    @Override public String permission() { return "bellsuite.module.belllands"; }

    @Override
    public List<Stat> dashboard() {
        // Strefy = spojne grupy chunkow (14 polaczonych chunkow = 1 strefa)
        int zones = ClaimZoneBuilder.compute(lands.getAllLands()).size();

        // Domy NIE naleza do Free — dostarcza je modul BellLandsPro. Tu liczymy tylko warpy.
        int warpCount = 0;
        for (UUID owner : warps.getAllWarpOwners()) warpCount += warps.getWarpNames(owner).size();

        Set<UUID> owners = new HashSet<>();
        for (Land l : lands.getAllLands()) owners.add(l.getOwner());

        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("Claimy", Integer.toString(zones), "cyan"));
        stats.add(new Stat("Warpy", Integer.toString(warpCount), "violet"));
        stats.add(new Stat("Wlasciciele", Integer.toString(owners.size()), "gold"));
        return stats;
    }

    @Override
    public List<MapMarker> markers(MapFilter filter) {
        List<MapMarker> out = new ArrayList<>();

        // Claimy jako STREFY (scalone grupy chunkow) z jednym obrysem na strefe
        if (filter.wants("claims")) {
            java.util.Map<String, Integer> perOwner = new java.util.HashMap<>();
            for (ClaimZoneBuilder.Zone z : ClaimZoneBuilder.compute(lands.getAllLands())) {
                if (!filter.wantsWorld(z.world())) continue;
                if (z.ring().length < 3) continue;
                String owner = ownerName(z.owner());
                int idx = perOwner.merge(owner, 1, Integer::sum);
                out.add(new MapMarker("claims", "zone",
                        owner + " · strefa #" + idx, z.world(),
                        z.centerX(), 64, z.centerZ(), "#3FC9FF", z.ring(),
                        java.util.Map.of("owner", owner, "zone", owner + "#" + idx,
                                "chunks", Integer.toString(z.chunkCount()))));
            }
        }

        // Warpy dzialek jako punkty. Domy NIE sa tutaj — to osobny system w BellLandsPro,
        // ktory wnosi warstwe "homes" wlasnym modulem.
        if (filter.wants("warps")) {
            for (UUID owner : warps.getAllWarpOwners()) {
                for (String name : warps.getWarpNames(owner)) {
                    Location loc = warps.getWarp(owner, name);
                    if (loc == null || loc.getWorld() == null) continue;
                    if (!filter.wantsWorld(loc.getWorld().getName())) continue;
                    out.add(new MapMarker("warps", "point",
                            ownerName(owner) + " · " + name, loc.getWorld().getName(),
                            loc.getX(), loc.getY(), loc.getZ(),
                            "#8A5CF6", new double[0][],
                            java.util.Map.of("owner", ownerName(owner), "name", name)));
                }
            }
        }
        return out;
    }

    private String ownerName(UUID uuid) {
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : uuid.toString().substring(0, 8);
    }
}
