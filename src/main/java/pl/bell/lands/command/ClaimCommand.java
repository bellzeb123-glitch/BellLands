package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.integration.Pl3xMapHook;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cKomenda tylko dla graczy.");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();

        if (args.length == 0) {
            // Zajmowanie dzialki
            if (landManager.isClaimed(chunk)) {
                player.sendMessage("§cTen teren jest juz zajety!");
                return true;
            }

            Land land = new Land(player.getUniqueId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            landManager.claimLand(land);

            // Wyrysowanie na mapie
            Pl3xMapHook.drawLand(land);

            player.sendMessage("§aZajales teren! Chunk: " + chunk.getX() + ", " + chunk.getZ());
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "unclaim" -> {
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) {
                    player.sendMessage("§cTen teren nie jest zajety przez nikogo.");
                    return true;
                }
                Land land = opt.get();
                if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage("§cTylko wlasciciel moze usunac ten claim!");
                    return true;
                }
                landManager.unclaimLand(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                
                // Usuniecie z mapy
                Pl3xMapHook.removeLand(land);
                
                player.sendMessage("§aUsunales claim z tego terenu.");
            }
            case "trust" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUzycie: /claim trust <gracz>");
                    return true;
                }
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) {
                    player.sendMessage("§cTen teren nie nalezy do nikogo.");
                    return true;
                }
                Land land = opt.get();
                if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage("§cTylko wlasciciel moze zarzadzac zaufanymi!");
                    return true;
                }
                
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                land.addTrusted(target.getUniqueId());
                landManager.saveLand(land);
                player.sendMessage("§aDodano gracza " + target.getName() + " do zaufanych na tej dzialce.");
            }
            case "untrust" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUzycie: /claim untrust <gracz>");
                    return true;
                }
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) {
                    player.sendMessage("§cTen teren nie nalezy do nikogo.");
                    return true;
                }
                Land land = opt.get();
                if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage("§cTylko wlasciciel moze zarzadzac zaufanymi!");
                    return true;
                }
                
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!land.isTrusted(target.getUniqueId())) {
                    player.sendMessage("§cTen gracz nie jest zaufany na tej dzialce.");
                    return true;
                }
                land.removeTrusted(target.getUniqueId());
                landManager.saveLand(land);
                player.sendMessage("§aUsunieto gracza " + target.getName() + " z zaufanych na tej dzialce.");
            }
            case "flag" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUzycie: /claim flag <pvp|explosions> <true|false>");
                    return true;
                }
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) {
                    player.sendMessage("§cTen teren nie nalezy do nikogo.");
                    return true;
                }
                Land land = opt.get();
                if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage("§cTylko wlasciciel moze zmieniac flagi!");
                    return true;
                }
                String flag = args[1].toLowerCase();
                if (!flag.equals("pvp") && !flag.equals("explosions")) {
                    player.sendMessage("§cObslugiwane flagi to: pvp, explosions");
                    return true;
                }
                boolean value = Boolean.parseBoolean(args[2]);
                land.setFlag(flag, value);
                landManager.saveLand(land);
                player.sendMessage("§aFlaga " + flag + " zostala ustawiona na: " + value);
            }
            case "info" -> {
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) {
                    player.sendMessage("§eTen teren jest wolny.");
                    return true;
                }
                Land land = opt.get();
                String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
                if (ownerName == null) ownerName = "Nieznany";

                String trustedNames = land.getTrusted().stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(name -> name != null)
                    .collect(Collectors.joining(", "));
                if (trustedNames.isEmpty()) trustedNames = "Brak";

                player.sendMessage("§6=== Informacje o Dzialce ===");
                player.sendMessage("§7Wlasciciel: §f" + ownerName);
                player.sendMessage("§7Zaufani: §f" + trustedNames);
                player.sendMessage("§7PVP: " + (land.getFlag("pvp") ? "§aWlaczone" : "§cWylaczone"));
                player.sendMessage("§7Wybuchy (Explosions): " + (land.getFlag("explosions") ? "§aWlaczone" : "§cWylaczone"));
                player.sendMessage("§6==========================");
            }
            default -> {
                player.sendMessage("§6=== Komendy /claim ===");
                player.sendMessage("§e/claim §7- Zajmuje chunk, na ktorym stoisz");
                player.sendMessage("§e/claim unclaim §7- Usuwa claim");
                player.sendMessage("§e/claim info §7- Informacje o dzialce");
                player.sendMessage("§e/claim trust <gracz> §7- Dodaje zaufanego gracza");
                player.sendMessage("§e/claim untrust <gracz> §7- Usuwa zaufanego gracza");
                player.sendMessage("§e/claim flag <pvp|explosions> <true|false> §7- Zmienia flagi");
            }
        }
        return true;
    }
}