package pl.bell.lands.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.bell.lands.BellLands;

import java.util.UUID;

/** Optional LuckPerms hook for warp limits on offline players (admin GUI). */
public final class LuckPermsWarpHook {

    private static LuckPerms luckPerms;

    private LuckPermsWarpHook() {}

    public static void init() {
        RegisteredServiceProvider<LuckPerms> provider =
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
    }

    public static Integer resolveMaxWarps(UUID uuid) {
        if (luckPerms == null) return null;
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return null;

        var permData = user.getCachedData().getPermissionData();
        if (permData.checkPermission("belllandspro.unlimited-warps").asBoolean()
            || permData.checkPermission("belllands.warps.unlimited").asBoolean()) {
            return Integer.MAX_VALUE;
        }

        int configMax = BellLands.getInstance().getConfig().getInt("claims.max-warps", 3);
        for (int i = 50; i >= 1; i--) {
            if (permData.checkPermission("belllands.warps." + i).asBoolean()) {
                return i;
            }
        }
        return configMax;
    }
}
