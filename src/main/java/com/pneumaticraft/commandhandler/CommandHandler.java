package com.pneumaticraft.commandhandler;

import com.lithium3141.OpenWarp.OpenWarp;
import com.lithium3141.shellparser.ShellParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommandHandler {

    protected OpenWarp plugin;

    protected List<Command> allCommands;

    public CommandHandler(OpenWarp plugin) {
        this.plugin = plugin;

        this.allCommands = new ArrayList<Command>();
    }

    public boolean locateAndRunCommand(CommandSender sender, List<String> args) {
        return this.locateAndRunCommand(sender, args, true); // Notify sender by default
    }

    public boolean locateAndRunCommand(CommandSender sender, List<String> args, boolean notifySender) {
        List<String> parsedArgs = parseAllQuotedStrings(args);
        CommandKey key = null;

        Iterator<Command> iterator = this.allCommands.iterator();
        Command foundCommand = null;
        // Initialize a list of all commands that match:
        List<Command> foundCommands = new ArrayList<Command>();
        List<CommandKey> foundKeys = new ArrayList<CommandKey>();

        while (iterator.hasNext()) {
            foundCommand = iterator.next();
            key = foundCommand.getKey(parsedArgs);
            if (key != null) {
                foundCommands.add(foundCommand);
                foundKeys.add(key);
            }
        }

        processFoundCommands(foundCommands, foundKeys, sender, parsedArgs, notifySender);
        return true;
    }

    /**
     * The purpose of this method is to determine the most specific command matching the args and execute it.
     *
     * @param foundCommands A list of all matching commands.
     * @param foundKeys     A list of the key that was matched the command.
     * @param sender        The sender of the original command
     * @param parsedArgs    The arguments who have been combined, ie: "The world" is one argument
     * @param notifySender  Whether to send optional messages to the command sender
     */
    private void processFoundCommands(List<Command> foundCommands, List<CommandKey> foundKeys, CommandSender sender, List<String> parsedArgs, boolean notifySender) {

        if (foundCommands.size() == 0) {
            return;
        }
        Command bestMatch = null;
        CommandKey matchingKey = null;
        int bestMatchInt = 0;

        for (int i = 0; i < foundCommands.size(); i++) {
            List<String> parsedCopy = new ArrayList<String>(parsedArgs);
            foundCommands.get(i).removeKeyArgs(parsedCopy, foundKeys.get(i).getKey());

            if (foundCommands.get(i).getNumKeyArgs(foundKeys.get(i).getKey()) > bestMatchInt) {
                bestMatch = foundCommands.get(i);
                matchingKey = foundKeys.get(i);
                bestMatchInt = bestMatch.getNumKeyArgs(matchingKey.getKey());
            } else if (foundCommands.get(i).getNumKeyArgs(foundKeys.get(i).getKey()) == bestMatchInt && (foundKeys.get(i).hasValidNumberOfArgs(parsedCopy.size()))) {
                // If the number of matched items was the same as a previous one
                // AND the new one has a valid number of args, it will be accepted
                // and will replace the previous one as the best command.
                bestMatch = foundCommands.get(i);
                matchingKey = foundKeys.get(i);
            }
        }

        if (bestMatch != null) {
            bestMatch.removeKeyArgs(parsedArgs, matchingKey.getKey());
            // Special case:
            // If the ONLY param is a '?' show them the usage.
            if (parsedArgs.size() == 1 && parsedArgs.get(0).equals("?") && plugin.hasAnyPermission(sender, bestMatch.getAllPermissionStrings(), bestMatch.isOpRequired())) {
                bestMatch.showHelp(sender);
            } else {
                checkAndRunCommand(sender, parsedArgs, bestMatch, notifySender);
            }
        }
    }

    public void registerCommand(Command command) {
        this.allCommands.add(command);
    }

    /**
     * Combines all quoted strings
     */
    private List<String> parseAllQuotedStrings(List<String> args) {
        String arg = null;
        if (args.size() == 0) {
            arg = "";
        } else {
            arg = args.get(0);
            for (int i = 1; i < args.size(); i++) {
                arg = arg + " " + args.get(i);
            }
        }

        List<String> result = ShellParser.safeParseString(arg);
        if (result == null) {
            return new ArrayList<String>();
        } else {
            return result;
        }
    }

    private void checkAndRunCommand(CommandSender sender, List<String> parsedArgs, Command foundCommand, boolean notifySender) {
        if (plugin.hasAnyPermission(sender, foundCommand.getAllPermissionStrings(), foundCommand.isOpRequired())) {
            if (foundCommand.checkArgLength(parsedArgs)) {
                foundCommand.runCommand(sender, parsedArgs);
            } else {
                foundCommand.showHelp(sender);
            }
        } else {
            if (notifySender) {
                sender.sendMessage("You do not have any of the required permission(s):");
                for (String perm : foundCommand.getAllPermissionStrings()) {
                    sender.sendMessage(" - " + ChatColor.GREEN + perm);
                }
            }
        }
    }
}
