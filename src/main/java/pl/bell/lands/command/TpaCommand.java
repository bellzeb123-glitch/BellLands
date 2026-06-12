package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.manager.TPAManager;

public class TpaCommand implements CommandExecutor {

    private final TPAManager tpaManager;

    public TpaCommand(TPAManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (label.equalsIgnoreCase("tpa")) {
            if (args.length == 0) {
                player.sendMessage("§cUzycie: /tpa <gracz>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§cNie znaleziono gracza.");
                return true;
            }
            tpaManager.createRequest(player, target);
        } else if (label.equalsIgnoreCase("tpaccept")) {
            tpaManager.acceptRequest(player);
        } else if (label.equalsIgnoreCase("tpdeny")) {
            tpaManager.denyRequest(player);
        }
        return true;
    }
}