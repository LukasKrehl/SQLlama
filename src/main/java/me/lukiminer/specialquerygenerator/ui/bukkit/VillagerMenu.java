package me.lukiminer.specialquerygenerator.ui.bukkit;

import com.google.common.collect.Lists;
import me.lukiminer.specialquerygenerator.ui.EntityCreator;
import net.miginfocom.swing.MigLayout;
import net.minecraft.server.v1_13_R2.MerchantRecipe;
import net.minecraft.server.v1_13_R2.MerchantRecipeList;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.util.List;

public class VillagerMenu extends JPanel implements EntityCreator {

	private class Trade {
		private ItemStack ingredient1,
				ingredient2,
				result;
	}

	private List<Trade> trades = Lists.newArrayList();

	private Trade currentTrade;

	private boolean ingredient1Locked = false,
			ingredient2Locked = false,
			resultLocked = false;

	public VillagerMenu() {
		initComponents();
		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Trade 1");
		this.tradeList.setModel(listModel);
		this.trades.add(new Trade());
		tradeList.setSelectedIndices(new int[]{0});
		currentTrade = trades.get(0);
		conformButtons(currentTrade);
	}

	private void addTradeButtonClicked(ActionEvent e) {
		Trade trade = new Trade();
		trades.add(trade);
		int index = trades.size() - 1;
		tradeList.setSelectedIndices(new int[]{index});
		currentTrade = trade;

		DefaultListModel<String> model = (DefaultListModel<String>) tradeList.getModel();
		model.addElement("Trade " + (index + 1));

		conformButtons(trade);
	}

	private void tradeListValueChanged(ListSelectionEvent e) {
		int selectedTrade = tradeList.getSelectedIndex();
		if (selectedTrade < 0) {
			nullButtons();
			currentTrade = null;
			return;
		}
		if (selectedTrade >= trades.size()) {
			System.err.println("Selected trade greater than trades list!");
			return;
		}
		Trade trade = trades.get(selectedTrade);
		currentTrade = trade;
		conformButtons(trade);
	}

	private void removeTradeButtonClicked(ActionEvent e) {
		int selectedTrade = tradeList.getSelectedIndex();
		if (selectedTrade >= trades.size()) {
			System.err.println("Selected trade greater than trades list!");
			return;
		}

		ListModel<String> listModel = tradeList.getModel();
		if (!(listModel instanceof DefaultListModel)) {
			return;
		}
		DefaultListModel<String> defaultListModel = (DefaultListModel<String>) listModel;

		defaultListModel.remove(selectedTrade);
		for (int i = 0; i < defaultListModel.getSize(); i++) {
			defaultListModel.setElementAt("Trade " + (i + 1), i);
		}
		trades.remove(selectedTrade);
		currentTrade = null;
	}

	private void conformButtons(Trade trade) {
		if (trade == null) {
			nullButtons();
			return;
		}
		conformButton(ingredient1Button, trade.ingredient1);
		conformButton(ingredient2Button, trade.ingredient2);
		conformButton(resultButton, trade.result);
	}

	private void conformButton(JButton button, ItemStack itemStack) {
		String name = "<null>";
		if (itemStack != null) {
			name = itemStack.getType().name();
		}
		button.setText(name);
	}

	private void nullButtons() {
		ingredient1Button.setText("");
		ingredient2Button.setText("");
		resultButton.setText("");
	}

	private void ingredient1ButtonClicked(ActionEvent e) {
		if (currentTrade == null || ingredient1Locked) {
			return;
		}
		ingredient1Locked = true;
		final Trade trade = currentTrade;
		ItemStackMenu itemStackMenu = new ItemStackMenu(trade.ingredient1, itemStack -> {
			trade.ingredient1 = itemStack;
			ingredient1Locked = false;
			conformButton(ingredient1Button, itemStack);
		});
		itemStackMenu.setVisible(true);
	}

	private void ingredient2ButtonClicked(ActionEvent e) {
		if (currentTrade == null || ingredient2Locked) {
			return;
		}
		ingredient2Locked = true;
		final Trade trade = currentTrade;
		ItemStackMenu itemStackMenu = new ItemStackMenu(trade.ingredient2, itemStack -> {
			trade.ingredient2 = itemStack;
			ingredient2Locked = false;
			conformButton(ingredient2Button, itemStack);
		});
		itemStackMenu.setVisible(true);
	}

	private void resultButtonClicked(ActionEvent e) {
		if (currentTrade == null || resultLocked) {
			return;
		}
		resultLocked = true;
		final Trade trade = currentTrade;
		ItemStackMenu itemStackMenu = new ItemStackMenu(trade.result, itemStack -> {
			trade.result = itemStack;
			resultLocked = false;
			conformButton(resultButton, itemStack);
		});
		itemStackMenu.setVisible(true);
	}

	@Override
	public String getEntityTypeString() {
		return "VILLAGER";
	}

	@Override
	public String createSqlNbtValue() {
		MerchantRecipeList recipeList = new MerchantRecipeList();
		trades.stream()
				.filter(trade -> trade.result != null && (trade.ingredient1 != null || trade.ingredient2 != null))
				.forEach(trade -> {
					recipeList.add(
							new MerchantRecipe(
									emptyOrCopy(trade.ingredient1),
									emptyOrCopy(trade.ingredient2),
									emptyOrCopy(trade.result),
									0,
									999999
							)
					);
				});

		NBTTagCompound nbtTagCompound = new NBTTagCompound();

		if (invulnerableCheckBox.isSelected()) {
			nbtTagCompound.setBoolean("Invulnerable", true);
		}
		nbtTagCompound.set("Offers", recipeList.a());

		return nbtTagCompound.toString();
	}

	private net.minecraft.server.v1_13_R2.ItemStack emptyOrCopy(ItemStack itemStack) {
		return itemStack == null ? net.minecraft.server.v1_13_R2.ItemStack.a : CraftItemStack.asNMSCopy(itemStack);
	}

	private void initComponents() {
		JPanel tradeCreator = new JPanel();
		JScrollPane scrollPane1 = new JScrollPane();
		tradeList = new JList<>();
		JPanel panel2 = new JPanel();
		ingredient1Button = new JButton();
		ingredient2Button = new JButton();
		JLabel label1 = new JLabel();
		resultButton = new JButton();
		JPanel panel1 = new JPanel();
		JButton addTradeButton = new JButton();
		JButton removeTradeButton = new JButton();
		JLabel label2 = new JLabel();
		invulnerableCheckBox = new JCheckBox();

		setLayout(new MigLayout(
			"hidemode 3",
			"[grow,fill]",
			"[grow,fill]"));

		{
			tradeCreator.setLayout(new MigLayout(
				"hidemode 3",
				"[30%!,grow,fill]" +
				"[grow,fill]",
				"[grow,fill]" +
				"[grow,fill]" +
				"[]"));

			{

				tradeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				tradeList.addListSelectionListener(this::tradeListValueChanged);
				scrollPane1.setViewportView(tradeList);
			}
			tradeCreator.add(scrollPane1, "cell 0 0");

			{
				panel2.setLayout(new MigLayout(
					"hidemode 3",
					"[grow,fill]" +
					"[grow,fill]" +
					"[grow,fill]" +
					"[grow,fill]",
					"[grow,fill]" +
					"[grow,fill]" +
					"[grow,fill]"));

				ingredient1Button.addActionListener(this::ingredient1ButtonClicked);
				panel2.add(ingredient1Button, "cell 0 1");

				ingredient2Button.addActionListener(this::ingredient2ButtonClicked);
				panel2.add(ingredient2Button, "cell 1 1");

				label1.setText("=>");
				label1.setHorizontalAlignment(SwingConstants.CENTER);
				panel2.add(label1, "cell 2 1");

				resultButton.addActionListener(this::resultButtonClicked);
				panel2.add(resultButton, "cell 3 1");
			}
			tradeCreator.add(panel2, "cell 1 0");

			{
				panel1.setLayout(new MigLayout(
					"hidemode 3",
					"[grow,fill]" +
					"[grow,fill]",
					"[]"));

				addTradeButton.setText("New Trade");
				addTradeButton.addActionListener(this::addTradeButtonClicked);
				panel1.add(addTradeButton, "cell 0 0");

				removeTradeButton.setText("Remove Trade");
				removeTradeButton.addActionListener(this::removeTradeButtonClicked);
				panel1.add(removeTradeButton, "cell 1 0");
			}
			tradeCreator.add(panel1, "cell 0 1");

			label2.setText("Invulnerable?");
			tradeCreator.add(label2, "cell 0 2");

			invulnerableCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
			tradeCreator.add(invulnerableCheckBox, "cell 1 2");
		}
		add(tradeCreator, "cell 0 0");
	}

	private JList<String> tradeList;
	private JButton ingredient1Button;
	private JButton ingredient2Button;
	private JButton resultButton;
	private JCheckBox invulnerableCheckBox;
}
