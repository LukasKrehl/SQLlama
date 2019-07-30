package me.lukiminer.specialquerygenerator.ui.bukkit;

import me.lukiminer.specialquerygenerator.ui.EntityCreator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class CommandMenu extends JPanel implements EntityCreator {
	public CommandMenu() {
		initComponents();
	}

	@Override
	public String getEntityTypeString() {
		return "COMMAND_BLOCK_MINECART";
	}

	@Override
	public String createSqlNbtValue() {
		String command = commandArea.getText();

		return "{Command:\"" + command + "\"}";
	}

	private void initComponents() {
		JScrollPane scrollPane1 = new JScrollPane();
		commandArea = new JTextArea();

		setLayout(new MigLayout(
				"hidemode 3",
				"[grow,fill]",
				"[grow,fill]"));

		{
			scrollPane1.setViewportView(commandArea);
		}
		add(scrollPane1, "cell 0 0");
	}

	private JTextArea commandArea;
}
