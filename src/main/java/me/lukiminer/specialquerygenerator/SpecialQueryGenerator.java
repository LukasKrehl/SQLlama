package me.lukiminer.specialquerygenerator;

import lombok.Getter;
import me.lukiminer.specialquerygenerator.commands.ReopenCommand;
import me.lukiminer.specialquerygenerator.ui.MainMenu;
import org.bukkit.plugin.java.JavaPlugin;

public class SpecialQueryGenerator extends JavaPlugin {

	@Getter
	private static SpecialQueryGenerator instance;

	@Getter
	private static MainMenu mainMenu = null;

	@Override
	public void onEnable() {
		instance = this;

		main(null);

		getCommand("reopen").setExecutor(new ReopenCommand());
	}

	public static void main(String[] args) {
		mainMenu = new MainMenu();
		mainMenu.setVisible(true);
	}

}
