package me.lukiminer.specialquerygenerator.ui.bukkit;

import me.lukiminer.specialquerygenerator.ui.EntityCreator;
import net.miginfocom.swing.MigLayout;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LLamaInventoryMenu extends JPanel implements EntityCreator {

	private ItemStack[] inventoryContents = new ItemStack[3 * 5];
	private boolean[] locks = new boolean[inventoryContents.length];

	public LLamaInventoryMenu() {
		initComponents();

	}

	private void inventoryTableMouseClicked(MouseEvent e) {
		int width = inventoryTable.getWidth();
		int height = inventoryTable.getHeight();
		int widthPerCell = width / 5;
		int heightPerCell = height / 3;
		int clickedColumn = e.getX() / widthPerCell;
		int clickedRow = e.getY() / heightPerCell;

		final int localSlot = 5 * clickedRow + clickedColumn;
		if (locks[localSlot]) {
			return;
		}
		locks[localSlot] = true;
		ItemStackMenu itemStackMenu = new ItemStackMenu(inventoryContents[localSlot], itemStack -> {
			inventoryContents[localSlot] = itemStack;
			updateTable(localSlot);
			locks[localSlot] = false;
		});
		itemStackMenu.setVisible(true);
	}

	private void updateTable(int slot) {
		String name = null;
		ItemStack itemStack = inventoryContents[slot];
		if (itemStack != null) {
			name = itemStack.getType().name();
		}
		updateTable(name, slot);
	}

	private void updateTable(String string, int slot) {
		inventoryTable.setValueAt(
				string,
				slot / 5,
				slot % 5
		);
	}

	@Override
	public String getEntityTypeString() {
		return "LLAMA";
	}

	@Override
	public String createSqlNbtValue() {
		NBTTagCompound nbtTagCompound = new NBTTagCompound();

		nbtTagCompound.setBoolean("ChestedHorse", true);
		nbtTagCompound.setBoolean("Tame", true);
		NBTTagList nbttaglist = new NBTTagList();
		for (int i = 2; i < inventoryContents.length + 2; ++i) {
			ItemStack itemStack = inventoryContents[i - 2];
			if (itemStack == null) {
				continue;
			}
			net.minecraft.server.v1_13_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

			if (!nmsStack.isEmpty()) {
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte) i);
				nmsStack.save(nbttagcompound1);
				nbttaglist.add(nbttagcompound1);
			}
		}
		nbtTagCompound.set("Items", nbttaglist);
		nbtTagCompound.setInt("Strength", 5);

		return nbtTagCompound.toString();
	}

	private void resetButtonClicked(ActionEvent e) {
		for (int i = 0; i < inventoryContents.length; i++) {
			inventoryContents[i] = null;
			updateTable(i);
		}
	}

	private void initComponents() {
		JButton resetButton = new JButton();
		inventoryTable = new JTable();

		setPreferredSize(new Dimension(1183, 600));
		setLayout(new MigLayout(
			"hidemode 3",
			"[grow,fill]",
			"[25!,grow,fill]" +
			"[75!,grow,fill]"));

		resetButton.setText("Reset");
		resetButton.addActionListener(this::resetButtonClicked);
		add(resetButton, "cell 0 0");

		inventoryTable.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null, null, null, null},
				{null, null, null, null, null},
				{null, null, null, null, null},
			},
			new String[] {
				null, null, null, null, null
			}
		) {
			boolean[] columnEditable = new boolean[] {
				false, false, false, false, false
			};
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return columnEditable[columnIndex];
			}
		});
		{
			TableColumnModel cm = inventoryTable.getColumnModel();
			cm.getColumn(0).setResizable(false);
			cm.getColumn(1).setResizable(false);
			cm.getColumn(2).setResizable(false);
			cm.getColumn(3).setResizable(false);
			cm.getColumn(4).setResizable(false);
		}
		inventoryTable.setRowHeight(60);
		inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		inventoryTable.setCellSelectionEnabled(true);
		inventoryTable.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
		inventoryTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				inventoryTableMouseClicked(e);
			}
		});
		add(inventoryTable, "cell 0 1");
	}

	private JTable inventoryTable;
}
