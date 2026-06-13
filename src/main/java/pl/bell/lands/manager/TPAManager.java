package pl.bell.lands.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPAManager {

    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();

    public void createRequest(Player sender, Player target) {
        // Wygasanie prośby po 60 sekundach
        pendingRequests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), System.currentTimeMillis() + 60000));
        
        sender.sendMessage("§aWyslano prosbe o teleportacje do " + target.getName());
        target.sendMessage("§e" + sender.getName() + " chce sie do Ciebie teleportowac. Wpisz /tpaccept");
    }

    public void acceptRequest(Player target) {
        TpaRequest request = pendingRequests.get(target.getUniqueId());
        
        if (request == null || System.currentTimeMillis() > request.expireTime()) {
            target.sendMessage("§cBrak aktywnych prosb o teleportacje.");
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            sender.teleport(target.getLocation());
            sender.sendMessage("§aTeleportowano!");
            target.sendMessage("§aZaakceptowano prosbe.");
        } else {
            target.sendMessage("§cGracz, ktory wyslal prosbe, jest juz offline.");
        }
        
        pendingRequests.remove(target.getUniqueId());
    }

    public void denyRequest(Player target) {
        if (pendingRequests.remove(target.getUniqueId()) != null) {
            target.sendMessage("§cOdrzucono prosbe.");
        } else {
            target.sendMessage("§cBrak prosb do odrzucenia.");
        }
    }

    public record TpaRequest(UUID senderId, long expireTime) {}
}