package pl.bell.lands.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.manager.RandomTeleportManager;

public class TprCommand implements CommandExecutor {

    private final RandomTeleportManager rtpManager;

    public TprCommand(RandomTeleportManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /tpr.");
            return true;
        }
        rtpManager.teleport(player);
        return true;
    }
}
