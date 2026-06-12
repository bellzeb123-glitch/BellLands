package pl.bell.lands.command;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;

public class ClaimCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cKomenda tylko dla graczy.");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        var landManager = BellLands.getInstance().getLandManager();

        if (landManager.isClaimed(chunk)) {
            player.sendMessage("§cTen teren jest juz zajety!");
            return true;
        }

        // Tworzymy nową działkę
        Land land = new Land(player.getUniqueId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        landManager.claimLand(land);

        player.sendMessage("§aZajales teren! Chunk: " + chunk.getX() + ", " + chunk.getZ());
        return true;
    }
}