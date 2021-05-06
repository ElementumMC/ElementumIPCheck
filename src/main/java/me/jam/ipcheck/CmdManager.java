package me.jam.ipcheck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CmdManager implements CommandExecutor {
    private DatabaseManager db = ElementumIPCheck.plugin.getDatabase();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Correcet usage is: /ipc <player | UUID | IP>");
            return true;
        }

        String param = args[0];
        if(args.length > 1 && param.equalsIgnoreCase("clear")) {
            if(!sender.hasPermission("ipcheck.ipc.clear")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission for this!");
                return true;
            }

            param = args[1];
        }

        final String arg = param;

        String f = "username";
        if(arg.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) f = "UUID";
        else if(arg.matches("^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!$)|$)){4}$")) f = "IP";

        final String field = f;

        new BukkitRunnable(){

            @Override
            public void run() {
                Set<UUID> set;

                if(field.equals("IP")) {
                    try {
                        set = searchWithGivenIP(arg);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        sender.sendMessage(ElementumIPCheck.prefix + ChatColor.RED + "An SQL error has occurred: " + e.getErrorCode());
                        return;
                    }
                } else {
                    OfflinePlayer player = field.equals("UUID")
                            ? Bukkit.getOfflinePlayer(UUID.fromString(arg))
                            : Bukkit.getOfflinePlayer(arg);

                    try {
                        set = searchWithGivenUUID(player.getUniqueId().toString());
                    } catch (SQLException e) {
                        e.printStackTrace();
                        sender.sendMessage(ElementumIPCheck.prefix + ChatColor.RED + "An SQL error has occurred: " + e.getErrorCode());
                        return;
                    }
                }

                if(set.size() == 0) {
                    sender.sendMessage(ElementumIPCheck.prefix + ChatColor.GREEN + "No matching records.");
                    return;
                }

                List<String> names = new LinkedList<>();
                List<String> uuids = new LinkedList<>();
                for(UUID uuid : set) {
                    names.add(Bukkit.getOfflinePlayer(uuid).getName());
                    uuids.add("'" + uuid.toString() + "'"); //UUIDs don't include ' so no sql injection possible (?)
                }

                if(args[0].equalsIgnoreCase("clear")) {
                    sender.sendMessage(ElementumIPCheck.prefix + ChatColor.DARK_RED + "Deleting " + ChatColor.YELLOW + set.size()
                            + ChatColor.DARK_RED + " record" + (set.size() == 1 ? "" : "s") + " matching " + field + " "
                            + ChatColor.YELLOW + arg + ChatColor.DARK_RED + " from the database...");

                    db.exec("DELETE FROM log WHERE UUID = " + String.join(" OR UUID = ", uuids));

                    sender.sendMessage(ElementumIPCheck.prefix + ChatColor.GREEN + "Deleted " + set.size()
                            + " record" + (set.size() == 1 ? "" : "s") + ".");
                    return;
                }

                sender.sendMessage(
                        ElementumIPCheck.prefix + ChatColor.GREEN + "Showing records with "
                                + field + " " + arg + " (" + (names.size() - 1) + ")..."
                );
                sender.sendMessage(ChatColor.STRIKETHROUGH + "-----------------------");


                ResultSet rs = db.query("SELECT UUID, IP FROM log WHERE UUID = " + String.join(" OR UUID = ", uuids) + " ORDER BY seen DESC LIMIT 1");

                try {
                    while(rs.next()) { //only once
                        sender.sendMessage(ChatColor.GOLD + "UUID: " + ChatColor.YELLOW + (field.equals("IP")
                                ? rs.getString("UUID")
                                : Bukkit.getOfflinePlayer(arg).getUniqueId()));
                        sender.sendMessage(ChatColor.DARK_RED + "Last IP: " + ChatColor.RED + rs.getString("IP"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "An error has occured while getting the last UUID & IP, limited data shown.");
                }
                sender.sendMessage(ChatColor.AQUA + "Accounts: " + ChatColor.GREEN + String.join(ChatColor.AQUA + ", " + ChatColor.GREEN, names));
            }

        }.runTaskAsynchronously(ElementumIPCheck.plugin);
        return true;
    }

    private HashSet<UUID> searchWithGivenUUID(String uuid) throws SQLException{
        return searchWithGivenUUID(uuid, new HashSet<>(), new HashSet<>());
    }

    private HashSet<UUID> searchWithGivenUUID(String uuid, HashSet<UUID> current, HashSet<String> lookedFor) throws SQLException {
        ResultSet rs = db.query("SELECT IP FROM log WHERE UUID = ? GROUP BY IP", uuid);
        while(rs.next()) {
            String ip = rs.getString("IP");
            if(lookedFor.contains(ip)) continue;
            lookedFor.add(ip);
            current = searchWithGivenIP(ip, current, lookedFor);
        }
        return current;
    }

    private HashSet<UUID> searchWithGivenIP(String ip) throws SQLException {
        return searchWithGivenIP(ip, new HashSet<>(), new HashSet<>());
    }

    private HashSet<UUID> searchWithGivenIP(String ip, HashSet<UUID> current, HashSet<String> lookedFor) throws SQLException {
        ResultSet rs = db.query("SELECT UUID FROM log WHERE IP = ? GROUP BY UUID", ip);
        while(rs.next()) {
            String uuid = rs.getString("UUID");
            if(lookedFor.contains(uuid)) continue;
            lookedFor.add(uuid);
            current.add(UUID.fromString(uuid));
            current = searchWithGivenUUID(uuid, current, lookedFor);
        }
        return current;
    }
}
