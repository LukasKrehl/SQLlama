package me.lukiminer.specialquerygenerator.commands;

import me.lukiminer.specialquerygenerator.SpecialQueryGenerator;
import me.lukiminer.specialquerygenerator.ui.MainMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class ReopenCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!(sender instanceof ConsoleCommandSender)) {
			sender.sendMessage(ChatColor.RED + "This is a console only command.");
			return true;
		}

		MainMenu mainMenu = SpecialQueryGenerator.getMainMenu();
		if (mainMenu == null) {
			sender.sendMessage(ChatColor.RED + "Main Menu does not exist.");
			return true;
		}

		if (mainMenu.isVisible()) {
			sender.sendMessage(ChatColor.RED + "Main Menu is visible!");
			return true;
		}

		mainMenu.setVisible(true);
		sender.sendMessage(ChatColor.GREEN + "Main Menu is visible again.");
		return true;
	}

}
