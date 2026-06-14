package pl.bell.lands.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPAManager {

    private final BellLands plugin;
    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();
    private long timeoutMs;
    private long cooldownMs;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TPAManager(BellLands plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.timeoutMs = plugin.getConfig().getLong("tpa.timeout-seconds", 60) * 1000;
        this.cooldownMs = plugin.getConfig().getLong("tpa.cooldown-seconds", 30) * 1000;
    }

    public void createRequest(Player sender, Player target) {
        LangManager lang = plugin.getLangManager();

        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(sender.getUniqueId());
        if (lastUsed != null && now - lastUsed < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUsed)) / 1000;
            sender.sendMessage(lang.component("tpa-sent", "player", target.getName()));
            return;
        }

        pendingRequests.put(target.getUniqueId(),
            new TpaRequest(sender.getUniqueId(), now + timeoutMs));
        cooldowns.put(sender.getUniqueId(), now);

        sender.sendMessage(lang.component("tpa-sent", "player", target.getName()));
        target.sendMessage(lang.component("tpa-received", "player", sender.getName()));
    }

    public void acceptRequest(Player target) {
        LangManager lang = plugin.getLangManager();
        TpaRequest request = pendingRequests.get(target.getUniqueId());

        if (request == null || System.currentTimeMillis() > request.expireTime()) {
            target.sendMessage(lang.component("tpa-no-request"));
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            sender.teleport(target.getLocation());
            sender.sendMessage(lang.component("tpa-teleported"));
            target.sendMessage(lang.component("tpa-accepted"));
        } else {
            target.sendMessage(lang.component("tpa-sender-offline"));
        }

        pendingRequests.remove(target.getUniqueId());
    }

    public void denyRequest(Player target) {
        LangManager lang = plugin.getLangManager();
        if (pendingRequests.remove(target.getUniqueId()) != null) {
            target.sendMessage(lang.component("tpa-denied"));
        } else {
            target.sendMessage(lang.component("tpa-no-deny"));
        }
    }

    public record TpaRequest(UUID senderId, long expireTime) {}
}
