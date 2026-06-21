package pl.bell.lands.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(BellLands.getInstance().getLangManager().component("only-players"));
            return true;
        }

        LangManager lang = BellLands.getInstance().getLangManager();
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "warp" -> {
                if (args.length == 0) {
                    player.sendMessage(lang.component("warp-usage"));
                    yield true;
                }
                if (args[0].equalsIgnoreCase("list")) {
                    yield WarpCommands.listWarps(player);
                }
                yield WarpCommands.warp(player, args[0]);
            }
            case "setwarp" -> {
                if (args.length == 0) {
                    player.sendMessage(lang.component("warp-setwarp-usage"));
                    yield true;
                }
                yield WarpCommands.setWarp(player, args[0]);
            }
            case "delwarp" -> {
                if (args.length == 0) {
                    player.sendMessage(lang.component("warp-delwarp-usage"));
                    yield true;
                }
                yield WarpCommands.deleteWarp(player, args[0]);
            }
            case "warps" -> WarpCommands.listWarps(player);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (args.length != 1) return List.of();
        if (!cmd.equals("warp") && !cmd.equals("delwarp")) return List.of();

        return BellLands.getInstance().getWarpManager().getWarpNames(player.getUniqueId()).stream()
            .filter(n -> n.startsWith(args[0].toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList());
    }
}
