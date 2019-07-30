package me.lukiminer.specialquerygenerator.ui.bukkit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import me.lukiminer.specialquerygenerator.SpecialQueryGenerator;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ItemStackCreator extends JPanel {

	private static final java.util.List<String> SHULKER_MATERIALS;

	private static ItemStack copiedItemStack = null;

	static {
		SHULKER_MATERIALS = Arrays.stream(Material.values())
				.map(Enum::name)
				.filter(material -> !material.startsWith("LEGACY_"))
				.filter(material -> material.contains("SHULKER_BOX"))
				.collect(Collectors.toList());
	}

	@Getter
	private @Nullable
	ItemStack createdItemStack = null;

	private Map<Enchantment, Integer> enchantmentMap = Maps.newHashMap();

	private class PotionEffectCache {
		int amplifier = 0;
		int duration = 0;
		boolean particles = true;
	}

	private Map<PotionEffectType, PotionEffectCache> potionEffectCacheMap = Maps.newHashMap();

	private java.util.List<String> bookPages = null;
	private boolean bookPageLock = false;

	private ItemStack[] shulkerContents = new ItemStack[3 * 9];
	private final boolean[] editedContents = new boolean[3 * 9];

	private String[] usableMaterialsNames;

	ItemStackCreator() {
		initComponents();
		usableMaterialsNames = Arrays.stream(Material.values())
				.map(Enum::name)
				.filter(s -> !s.startsWith("LEGACY_"))
				.toArray(String[]::new);
		Arrays.stream(usableMaterialsNames)
				.forEach(materialSelector::addItem);
		DefaultListModel<ItemFlag> model = new DefaultListModel<>();
		Arrays.stream(ItemFlag.values())
				.forEach(model::addElement);
		itemFlagList.setModel(model);
		Arrays.stream(Enchantment.values())
				.map(Enchantment::getKey)
				.map(NamespacedKey::getKey)
				.sorted()
				.forEach(enchantmentSelector::addItem);
		Arrays.stream(PotionEffectType.values())
				.filter(Objects::nonNull)
				.map(PotionEffectType::getName)
				.sorted()
				.forEach(potionEffectSelector::addItem);
		handleMaterialChanged();
	}

	private void createButtonAction(ActionEvent e) {
		Object selectedMaterial = materialSelector.getSelectedItem();
		if (selectedMaterial == null) {
			return;
		}

		Material material = Material.valueOf(selectedMaterial.toString());
		ItemStack itemStack = new ItemStack(material);

		Object amountSelected = amountSpinner.getValue();
		if (!(amountSelected instanceof Integer)) {
			return;
		}

		itemStack.setAmount((Integer) amountSelected);

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			this.createdItemStack = itemStack;
			return;
		}

		String displayName = nameField.getText();
		if (displayName != null && !displayName.isEmpty()) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
		}

		String loreText = loreArea.getText();
		if (loreText != null && !loreText.isEmpty()) {
			java.util.List<String> lore = Arrays.stream(loreArea.getText().split("\n"))
					.map(s -> ChatColor.translateAlternateColorCodes('&', s))
					.collect(Collectors.toList());
			meta.setLore(lore);
		}

		itemFlagList.getSelectedValuesList()
				.forEach(meta::addItemFlags);

		enchantmentMap.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));

		if (meta instanceof BlockStateMeta && SHULKER_MATERIALS.contains(material.name())) {
			BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
			ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
			Inventory shulkerInventory = shulkerBox.getInventory();
			for (int i = 0; i < shulkerContents.length; i++) {
				shulkerInventory.setItem(i, shulkerContents[i]);
			}
			blockStateMeta.setBlockState(shulkerBox);
		}

		if (meta instanceof PotionMeta) {
			PotionMeta potionMeta = (PotionMeta) meta;
			potionEffectCacheMap.forEach((potionEffectType, potionEffectCache) -> {
				potionMeta.addCustomEffect(new PotionEffect(potionEffectType, potionEffectCache.duration,
						potionEffectCache.amplifier, false, potionEffectCache.particles, false), true);
			});
		}

		if (meta instanceof BookMeta) {
			BookMeta bookMeta = (BookMeta) meta;
			bookMeta.setPages(Lists.newArrayList(bookPages));
		}

		itemStack.setItemMeta(meta);
		this.createdItemStack = itemStack;
	}

	private void enchantmentSpinnerChange(ChangeEvent e) {
		Object selectedEnchantment = enchantmentSelector.getSelectedItem();
		if (selectedEnchantment == null) {
			return;
		}

		String enchantmentName = selectedEnchantment.toString();
		NamespacedKey key = NamespacedKey.minecraft(enchantmentName);
		Enchantment enchantment = Enchantment.getByKey(key);
		if (enchantment == null) {
			return;
		}

		Object levelValue = enchantmentLevelSpinner.getValue();
		if (!(levelValue instanceof Integer)) {
			return;
		}

		Integer level = (Integer) levelValue;
		enchantmentMap.compute(enchantment, (enchantment1, integer) -> {
			if (level == 0) {
				return null;
			}
			return level;
		});
	}

	private void shulkerTableMouseClicked(MouseEvent e) {
		int width = shulkerTable.getWidth();
		int height = shulkerTable.getHeight();
		int widthPerCell = width / 9;
		int heightPerCell = height / 3;
		int clickedColumn = e.getX() / widthPerCell;
		int clickedRow = e.getY() / heightPerCell;
		int indexedSlot = 9 * clickedRow + clickedColumn;

		if (!shulkerTable.isEnabled()) {
			return;
		}

		synchronized (editedContents) {
			if (editedContents[indexedSlot]) {
				return;
			}
			editedContents[indexedSlot] = true;
		}

		ItemStackMenu shulkerContentMenu = new ItemStackMenu(shulkerContents[indexedSlot], itemStack -> {
			handleShulkerContentChange(itemStack, indexedSlot);
			editedContents[indexedSlot] = false;
		});

		shulkerContentMenu.setVisible(true);
	}

	private void handleShulkerContentChange(ItemStack itemStack, int indexedSlot) {
		shulkerContents[indexedSlot] = itemStack;
		String name = null;
		if (itemStack != null) {
			name = itemStack.getType().name();
		}
		int row = indexedSlot / 9;
		int column = indexedSlot % 9;
		shulkerTable.getModel().setValueAt(
				name,
				row,
				column
		);
	}

	void conformTo(ItemStack itemStack) {
		if (itemStack == null) {
			itemStack = new ItemStack(Material.valueOf(usableMaterialsNames[0]));
		}

		int materialIndex = ArrayUtils.indexOf(Material.values(), itemStack.getType());
		materialSelector.setSelectedIndex(materialIndex);

		amountSpinner.setValue(itemStack.getAmount());

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			return;
		}

		String displayName = meta.getDisplayName();
		if (!displayName.isEmpty()) {
			nameField.setText(displayName.replace(ChatColor.COLOR_CHAR, '&'));
		}

		java.util.List<String> lore = meta.getLore();
		if (lore != null) {
			String loreText = lore.stream()
					.map(s -> s.replace(ChatColor.COLOR_CHAR, '&'))
					.collect(Collectors.joining("\n"));
			loreArea.setText(loreText);
		}

		itemFlagList.setSelectedIndices(findIndices(ItemFlag.values(), meta.getItemFlags()));

		enchantmentMap = Maps.newHashMap(meta.getEnchants());
		handleEnchantmentChanged();

		if (meta instanceof BlockStateMeta && SHULKER_MATERIALS.contains(itemStack.getType().name())) {
			BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
			ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
			int index = 0;
			for (ItemStack content : shulkerBox.getInventory().getContents()) {
				handleShulkerContentChange(content, index++);
			}
		}

		if (meta instanceof PotionMeta) {
			PotionMeta potionMeta = (PotionMeta) meta;
			potionMeta.getCustomEffects().forEach(potionEffect -> {
				PotionEffectCache effectCache = new PotionEffectCache();
				effectCache.amplifier = potionEffect.getAmplifier();
				effectCache.duration = potionEffect.getDuration();
				potionEffectCacheMap.put(potionEffect.getType(), effectCache);
			});
			handlePotionEffectChanged(getSelectedPotionEffect());
			setPotionEffectCreatorEnabled(true);
		} else {
			setPotionEffectCreatorEnabled(false);
		}

		if (meta instanceof BookMeta) {
			BookMeta bookMeta = (BookMeta) meta;
			this.bookPages = Lists.newArrayList(bookMeta.getPages());

			openBookEditorButton.setEnabled(true);
		} else {
			openBookEditorButton.setEnabled(false);
		}

		this.createdItemStack = itemStack;
	}

	private <T> int[] findIndices(T[] array, Collection<T> values) {
		return IntStream.range(0, array.length)
				.filter(i -> values.contains(array[i]))
				.toArray();
	}

	private void enchantmentChanged(ActionEvent e) {
		handleEnchantmentChanged();
	}

	private void handleEnchantmentChanged() {
		Object selectedEnchantment = enchantmentSelector.getSelectedItem();
		if (selectedEnchantment == null) {
			return;
		}

		NamespacedKey key = NamespacedKey.minecraft(selectedEnchantment.toString());
		Enchantment enchantment = Enchantment.getByKey(key);

		if (enchantment == null) {
			return;
		}

		int level = enchantmentMap.getOrDefault(enchantment, 0);
		enchantmentLevelSpinner.setValue(level);
	}

	private void copyButtonClicked(ActionEvent e) {
		copiedItemStack = createdItemStack;
	}

	private void pasteButtonClicked(ActionEvent e) {
		if (copiedItemStack != null) {
			conformTo(copiedItemStack);
		}
	}

	private void potionEffectSelectorActionPerformed(ActionEvent e) {
		handlePotionEffectChanged(getSelectedPotionEffect());
	}

	private PotionEffectType getSelectedPotionEffect() {
		String selectedType = (String) potionEffectSelector.getSelectedItem();
		if (selectedType == null) {
			return null;
		}

		PotionEffectType type = PotionEffectType.getByName(selectedType);
		if (type == null) {
			return null;
		}

		return type;
	}

	private void handlePotionEffectChanged(PotionEffectType type) {
		PotionEffectCache effectCache = null;
		if (type != null) {
			effectCache = potionEffectCacheMap.get(type);
		}
		if (effectCache == null) {
			potionEffectAmplifierSpinner.setEnabled(false);
		} else {
			potionEffectAmplifierSpinner.setEnabled(true);
		}
	}


	private void setPotionEffectCreatorEnabled(boolean enabled) {
		potionEffectSelector.setEnabled(enabled);
		potionEffectTicksSpinner.setEnabled(enabled);
		potionEffectPartclesCheckbox.setEnabled(enabled);

		if (!enabled) {
			potionEffectAmplifierSpinner.setValue(0);
			potionEffectTicksSpinner.setValue(0);
			potionEffectPartclesCheckbox.setSelected(true);
		} else {
			handlePotionEffectChanged(getSelectedPotionEffect());
		}
	}

	private void materialSelectorActionPerformed(ActionEvent e) {
		handleMaterialChanged();
	}

	private void handleMaterialChanged() {
		if (SpecialQueryGenerator.getInstance() == null) {
			return;
		}

		String selectedMaterial = (String) materialSelector.getSelectedItem();
		if (selectedMaterial == null || selectedMaterial.isEmpty()) {
			return;
		}

		Material material = Material.valueOf(selectedMaterial);
		ItemMeta meta = new ItemStack(material).getItemMeta();
		if (meta == null) {
			return;
		}
		setPotionEffectCreatorEnabled(meta instanceof PotionMeta);
		openBookEditorButton.setEnabled(meta instanceof BookMeta);
		shulkerTable.setEnabled(SHULKER_MATERIALS.contains(material.name()));
	}

	private void openBookEditorButtonClicked(ActionEvent e) {
		if (bookPageLock) {
			return;
		}
		bookPageLock = true;
		BookEditorMenu bookEditorMenu = new BookEditorMenu(bookPages, strings -> {
			this.bookPages = Lists.newArrayList(strings);
		});
		bookEditorMenu.setVisible(true);
		bookPageLock = false;
	}

	private void potionEffectTicksSpinnerStateChanged(ChangeEvent e) {
		String selectedType = (String) potionEffectSelector.getSelectedItem();
		if (selectedType == null) {
			return;
		}

		PotionEffectType potionEffectType = PotionEffectType.getByName(selectedType);
		if (potionEffectType == null) {
			return;
		}

		int duration = (int) potionEffectTicksSpinner.getValue();
		if (duration <= 0) {
			potionEffectCacheMap.remove(potionEffectType);
			potionEffectAmplifierSpinner.setEnabled(false);
		} else {
			potionEffectCacheMap.compute(potionEffectType, (potionEffectType1, potionEffectCache) -> {
				if (potionEffectCache == null) {
					potionEffectCache = new PotionEffectCache();
				}
				potionEffectCache.duration = duration;
				return potionEffectCache;
			});
			potionEffectAmplifierSpinner.setEnabled(true);
		}

		handlePotionEffectChanged(getSelectedPotionEffect());
	}

	private void initComponents() {
		JLabel label1 = new JLabel();
		materialSelector = new JComboBox<>();
		JLabel label2 = new JLabel();
		amountSpinner = new JSpinner();
		JLabel label3 = new JLabel();
		nameField = new JTextField();
		JLabel label4 = new JLabel();
		JScrollPane scrollPane1 = new JScrollPane();
		loreArea = new JTextArea();
		JLabel label5 = new JLabel();
		JScrollPane scrollPane2 = new JScrollPane();
		itemFlagList = new JList<>();
		JLabel label6 = new JLabel();
		JPanel panel1 = new JPanel();
		enchantmentSelector = new JComboBox();
		enchantmentLevelSpinner = new JSpinner();
		JLabel label10 = new JLabel();
		JPanel panel3 = new JPanel();
		potionEffectSelector = new JComboBox<>();
		JPanel panel4 = new JPanel();
		JLabel label11 = new JLabel();
		potionEffectAmplifierSpinner = new JSpinner();
		JLabel label12 = new JLabel();
		potionEffectTicksSpinner = new JSpinner();
		JLabel label13 = new JLabel();
		potionEffectPartclesCheckbox = new JCheckBox();
		JLabel label7 = new JLabel();
		shulkerTable = new JTable();
		JLabel label14 = new JLabel();
		openBookEditorButton = new JButton();
		JLabel label8 = new JLabel();
		JButton button1 = new JButton();
		JLabel label9 = new JLabel();
		JPanel panel2 = new JPanel();
		JButton copyButton = new JButton();
		JButton pasteButton = new JButton();

		setBackground(Color.white);
		setPreferredSize(new Dimension(760, 443));
		setLayout(new MigLayout(
				"novisualpadding,hidemode 3",
				"[grow,fill]" +
						"[75%!,grow,fill]",
				"[]" +
						"[]" +
						"[]" +
						"[90!,fill]" +
						"[90!,fill]" +
						"[]" +
						"[]" +
						"[fill]" +
						"[]" +
						"[25!,fill]" +
						"[]"));

		label1.setText("Material");
		add(label1, "cell 0 0");

		materialSelector.addActionListener(e -> {
			materialSelectorActionPerformed(e);
			materialSelectorActionPerformed(e);
		});
		add(materialSelector, "cell 1 0");

		label2.setText("Amount");
		add(label2, "cell 0 1");

		amountSpinner.setModel(new SpinnerNumberModel(1, 1, 64, 1));
		add(amountSpinner, "cell 1 1");

		label3.setText("Name");
		add(label3, "cell 0 2");
		add(nameField, "cell 1 2");

		label4.setText("Lore");
		add(label4, "cell 0 3");

		{
			scrollPane1.setViewportView(loreArea);
		}
		add(scrollPane1, "cell 1 3");

		label5.setText("Flags");
		add(label5, "cell 0 4");

		{
			scrollPane2.setViewportView(itemFlagList);
		}
		add(scrollPane2, "cell 1 4");

		label6.setText("Enchantments");
		add(label6, "cell 0 5");

		{
			panel1.setBackground(Color.white);
			panel1.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
			panel1.setLayout(new MigLayout(
					"hidemode 3",
					"[80%!,grow,fill]" +
							"[grow,fill]",
					"[]"));

			enchantmentSelector.addActionListener(this::enchantmentChanged);
			panel1.add(enchantmentSelector, "cell 0 0");

			enchantmentLevelSpinner.addChangeListener(this::enchantmentSpinnerChange);
			panel1.add(enchantmentLevelSpinner, "cell 1 0");
		}
		add(panel1, "cell 1 5");

		label10.setText("Potion Effects");
		add(label10, "cell 0 6");

		{
			panel3.setBackground(Color.white);
			panel3.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
			panel3.setLayout(new MigLayout(
					"hidemode 3",
					"[fill,grow]",
					"[fill,grow]" +
							"[fill,grow]"));

			potionEffectSelector.addActionListener(e -> {
				potionEffectSelectorActionPerformed(e);
				potionEffectSelectorActionPerformed(e);
			});
			panel3.add(potionEffectSelector, "cell 0 0");

			{
				panel4.setBackground(Color.white);
				panel4.setLayout(new MigLayout(
						"hidemode 3",
						"[grow,fill]" +
								"[grow,fill]",
						"[grow,fill]" +
								"[grow,fill]" +
								"[grow,fill]"));

				label11.setText("Amplifier");
				panel4.add(label11, "cell 0 0");
				panel4.add(potionEffectAmplifierSpinner, "cell 1 0");

				label12.setText("Duration (Ticks)");
				panel4.add(label12, "cell 0 1");

				potionEffectTicksSpinner.addChangeListener(this::potionEffectTicksSpinnerStateChanged);
				panel4.add(potionEffectTicksSpinner, "cell 1 1");

				label13.setText("Particles");
				panel4.add(label13, "cell 0 2");

				potionEffectPartclesCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
				potionEffectPartclesCheckbox.setBackground(Color.white);
				potionEffectPartclesCheckbox.setSelected(true);
				panel4.add(potionEffectPartclesCheckbox, "cell 1 2");
			}
			panel3.add(panel4, "cell 0 1");
		}
		add(panel3, "cell 1 6");

		label7.setText("Shulker Contents");
		add(label7, "cell 0 7");

		shulkerTable.setModel(new DefaultTableModel(
				new Object[][]{
						{null, null, null, null, null, null, null, null, null},
						{null, null, null, null, null, null, null, null, null},
						{null, null, null, null, null, null, null, null, null},
				},
				new String[]{
						null, null, null, null, null, null, null, null, null
				}
		) {
			boolean[] columnEditable = new boolean[]{
					false, false, false, false, false, false, false, false, false
			};

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return columnEditable[columnIndex];
			}
		});
		{
			TableColumnModel cm = shulkerTable.getColumnModel();
			cm.getColumn(0).setResizable(false);
			cm.getColumn(1).setResizable(false);
			cm.getColumn(2).setResizable(false);
			cm.getColumn(3).setResizable(false);
			cm.getColumn(4).setResizable(false);
			cm.getColumn(5).setResizable(false);
			cm.getColumn(6).setResizable(false);
			cm.getColumn(7).setResizable(false);
			cm.getColumn(8).setResizable(false);
		}
		shulkerTable.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
		shulkerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		shulkerTable.setRowHeight(25);
		shulkerTable.setCellSelectionEnabled(true);
		shulkerTable.setEnabled(false);
		shulkerTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				shulkerTableMouseClicked(e);
			}
		});
		add(shulkerTable, "cell 1 7");

		label14.setText("Book Pages");
		add(label14, "cell 0 8");

		openBookEditorButton.setText("Open Book Editor");
		openBookEditorButton.addActionListener(this::openBookEditorButtonClicked);
		add(openBookEditorButton, "cell 1 8");

		label8.setText("Done?");
		add(label8, "cell 0 9");

		button1.setText("Create");
		button1.addActionListener(this::createButtonAction);
		add(button1, "cell 1 9");

		label9.setText("Special");
		add(label9, "cell 0 10");

		{
			panel2.setBackground(Color.white);
			panel2.setLayout(new MigLayout(
					"hidemode 3",
					"[grow,fill]" +
							"[grow,fill]",
					"[]"));

			copyButton.setText("Copy");
			copyButton.addActionListener(this::copyButtonClicked);
			panel2.add(copyButton, "cell 0 0");

			pasteButton.setText("Paste");
			pasteButton.addActionListener(this::pasteButtonClicked);
			panel2.add(pasteButton, "cell 1 0");
		}
		add(panel2, "cell 1 10");
	}

	private JComboBox<String> materialSelector;
	private JSpinner amountSpinner;
	private JTextField nameField;
	private JTextArea loreArea;
	private JList<ItemFlag> itemFlagList;
	private JComboBox enchantmentSelector;
	private JSpinner enchantmentLevelSpinner;
	private JComboBox<String> potionEffectSelector;
	private JSpinner potionEffectAmplifierSpinner;
	private JSpinner potionEffectTicksSpinner;
	private JCheckBox potionEffectPartclesCheckbox;
	private JTable shulkerTable;
	private JButton openBookEditorButton;
}
