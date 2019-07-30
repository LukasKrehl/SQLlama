package me.lukiminer.specialquerygenerator.ui.bukkit;

import net.miginfocom.swing.MigLayout;
import org.bukkit.inventory.ItemStack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

class ItemStackMenu extends JFrame {

	private ItemStack callbackItemStack;

	private Consumer<ItemStack> callback;

	ItemStackMenu(ItemStack currentItemStack, Consumer<ItemStack> callback) {
		initComponents();
		this.itemStackCreator.conformTo(currentItemStack);
		this.callbackItemStack = currentItemStack;
		this.callback = callback;
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
	}


	private void saveButtonClicked(ActionEvent e) {
		this.callbackItemStack = itemStackCreator.getCreatedItemStack();
		close();
	}

	private void removeButtonClicked(ActionEvent e) {
		this.callbackItemStack = null;
		close();
	}

	private void onClose() {
		callback.accept(callbackItemStack);
	}

	private void close() {
		this.onClose();
		this.setVisible(false);
		this.dispose();
	}

	private void initComponents() {
		itemStackCreator = new ItemStackCreator();
		JPanel panel2 = new JPanel();
		JButton button1 = new JButton();
		JButton button2 = new JButton();

		setTitle("ItemStack");
		Container contentPane = getContentPane();
		contentPane.setLayout(new MigLayout(
			"hidemode 3",
			"[grow,fill]",
			"[grow,fill]" +
			"[35!,grow,fill]"));
		contentPane.add(itemStackCreator, "cell 0 0");

		{
			panel2.setLayout(new MigLayout(
				"hidemode 3",
				"[grow,fill]" +
				"[grow,fill]",
				"[25!,grow,fill]"));

			button1.setText("Save");
			button1.setBackground(Color.green);
			button1.setForeground(new Color(0, 153, 0));
			button1.addActionListener(this::saveButtonClicked);
			panel2.add(button1, "cell 0 0");

			button2.setText("Remove");
			button2.setForeground(new Color(153, 0, 0));
			button2.setBackground(new Color(255, 102, 102));
			button2.addActionListener(this::removeButtonClicked);
			panel2.add(button2, "cell 1 0");
		}
		contentPane.add(panel2, "cell 0 1");
		setSize(955, 780);
		setLocationRelativeTo(getOwner());
	}

	private ItemStackCreator itemStackCreator;
}
