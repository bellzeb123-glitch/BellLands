package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClaimTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
        "unclaim", "trust", "untrust", "flag", "flags", "info", "help"
    );

    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            // /claim <tab> — podpowiadaj subkomendy
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "trust" -> {
                    // /claim trust <tab> — podpowiadaj nazwy graczy online
                    return filterStartsWith(getOnlinePlayerNames(player), args[1]);
                }
                case "untrust" -> {
                    // /claim untrust <tab> — podpowiadaj nazwy zaufanych na tej dzialce
                    return filterStartsWith(getTrustedNames(player), args[1]);
                }
                case "flag" -> {
                    // /claim flag <tab> — podpowiadaj nazwy flag
                    return filterStartsWith(Arrays.asList(Land.ALL_FLAGS), args[1]);
                }
            }
        }

        if (args.length == 3 && sub.equals("flag")) {
            // /claim flag <flaga> <tab> — podpowiadaj true/false
            return filterStartsWith(BOOLEAN_VALUES, args[2]);
        }

        return List.of();
    }

    /**
     * Filtruje liste podpowiedzi na podstawie wpisanego tekstu (case-insensitive).
     */
    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .collect(Collectors.toList());
    }

    /**
     * Zwraca nazwy graczy online (oprocz gracza wywolujacego).
     */
    private List<String> getOnlinePlayerNames(Player sender) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.equals(sender))
            .map(Player::getName)
            .collect(Collectors.toList());
    }

    /**
     * Zwraca nazwy zaufanych graczy na dzialce, na ktorej stoi gracz.
     */
    private List<String> getTrustedNames(Player player) {
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(player.getLocation().getChunk());
        if (opt.isEmpty()) return List.of();

        Land land = opt.get();
        List<String> names = new ArrayList<>();
        for (var uuid : land.getTrusted()) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) names.add(name);
        }
        return names;
    }
}
