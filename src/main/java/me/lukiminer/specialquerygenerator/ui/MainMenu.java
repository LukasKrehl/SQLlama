package me.lukiminer.specialquerygenerator.ui;

import com.google.common.collect.Lists;
import me.lukiminer.specialquerygenerator.ui.bukkit.LLamaInventoryMenu;
import me.lukiminer.specialquerygenerator.ui.bukkit.VillagerMenu;
import me.lukiminer.specialquerygenerator.ui.bukkit.CommandMenu;
import net.miginfocom.swing.MigLayout;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class MainMenu extends JFrame {

	public MainMenu() {
		initComponents();
	}

	private static final String NORMAL_END = "),1)#";
	private static final String MULTI_END = ",REPLACE('*,*','*',CHAR(34)), '')),1)#";

	private static final String NORMAL_BEGIN = "FROM_BASE64(";
	private static final String MULTI_BEGIN = "FROM_BASE64(REPLACE(";

	private class IndexedSelection {
		private int startIndex = 0;
		private int endIndex = 0;
	}

	private java.util.List<IndexedSelection> indexedSelections = Lists.newArrayList();
	private int currentIndexedSelection = -1;

	private void generateButtonClick(ActionEvent e) {
		Component component = entityTab.getSelectedComponent();
		if (!(component instanceof EntityCreator)) {
			return;
		}

		String uuid = uuidField.getText();

		if(uuid == null || uuid.isEmpty()) {
			sqlField.setText("UUID Field is empty!");
			return;
		}

		if (uuid.length() <= 16) {
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
			if (offlinePlayer != null) {
				uuid = offlinePlayer.getUniqueId().toString();
			}
		}

		String sqlStart = "',1),('" + uuid + "','" + ((EntityCreator) component).getEntityTypeString() + "',";

		String sqlNbtString;
		try {
			sqlNbtString = "'" + Base64.getEncoder().encodeToString(((EntityCreator) component).createSqlNbtValue()
					.getBytes(encodingField.getText())) + "'";
		} catch (UnsupportedEncodingException e1) {
			encodingField.setForeground(Color.RED);
			return;
		}
		encodingField.setForeground(Color.BLACK);
		String composedSqlNbt = composeSqlNbtString(sqlStart.length(), sqlNbtString);

		String sql = sqlStart + composedSqlNbt +
				composeEnding(sqlStart.length() + composedSqlNbt.length());

		boolean multiLine = sql.length() > 255;
		int lastIndex = sqlNbtString.lastIndexOf("'") + sqlStart.length();
		if (multiLine) {
			lastIndex += MULTI_BEGIN.length();
		} else {
			lastIndex += NORMAL_BEGIN.length();
		}

		StringBuilder sb = new StringBuilder();
		int pageNumber = 1;
		currentIndexedSelection = -1;
		indexedSelections.clear();
		unmarkSelection();

		int indexStartOffset = 0;

		for (int i = 0; i < (multiLine ? lastIndex : sql.length()); i += 255) {
			int indexEnd = Math.min(multiLine ? lastIndex : sql.length(), i + 255);
			String page = sql.substring(i, indexEnd);

			sb.append("----------- PAGE ").append(pageNumber).append("-----------");
			sb.append("\n");
			sb.append(page);
			sb.append("\n\n\n");

			indexStartOffset += "----------- PAGE -----------".length() + String.valueOf(pageNumber).length() + 1;
			indexEnd += indexStartOffset;

			IndexedSelection selection = new IndexedSelection();
			selection.startIndex = i + indexStartOffset;
			selection.endIndex = indexEnd;
			indexedSelections.add(selection);

			indexStartOffset += 3;
			++pageNumber;
		}

		if (multiLine) {
			String page = sql.substring(lastIndex);

			indexStartOffset += "----------- PAGE -----------".length() + String.valueOf(pageNumber).length() + 1;

			IndexedSelection selection = new IndexedSelection();
			selection.startIndex = lastIndex + indexStartOffset;
			selection.endIndex = sql.length() + indexStartOffset;
			System.out.println("selection.startIndex = " + selection.startIndex);
			System.out.println("selection.endIndex = " + selection.endIndex);
			indexedSelections.add(selection);

			sb.append("----------- PAGE ").append(pageNumber).append("-----------");
			sb.append("\n");
			sb.append(page);
		}

		sqlField.setText(sb.toString());
	}

	private String composeEnding(int length) {
		if (NORMAL_END.length() + length > 255) {
			return MULTI_END;
		}
		return NORMAL_END;
	}

	private String composeSqlNbtString(int startLength, String sqlNbtString) {
		if (startLength + NORMAL_BEGIN.length() + sqlNbtString.length() + NORMAL_END.length() > 255) {
			return MULTI_BEGIN + sqlNbtString;
		}
		return NORMAL_BEGIN + sqlNbtString;
	}

	private void encodingCheckboxClick(ActionEvent e) {
		encodingField.setEnabled(encodingCheckbox.isSelected());
	}

	private void prevSelectionButtonClicked(ActionEvent e) {
		if (currentIndexedSelection <= 0) {
			return;
		}
		currentIndexedSelection -= 1;
		markAndCopySelection();
	}

	private void nextSelectionButtonClicked(ActionEvent e) {
		if (currentIndexedSelection + 1 >= indexedSelections.size()) {
			return;
		}
		currentIndexedSelection += 1;
		markAndCopySelection();
	}

	private void markAndCopySelection() {
		if (!sqlField.requestFocusInWindow()) {
			return;
		}
		IndexedSelection selection = indexedSelections.get(currentIndexedSelection);
		sqlField.select(selection.startIndex, selection.endIndex);

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection stringSelection = new StringSelection(sqlField.getSelectedText());
		clipboard.setContents(stringSelection, stringSelection);
	}

	private void unmarkSelection() {
		sqlField.select(0, 0);
	}

	private void initComponents() {
		entityTab = new JTabbedPane();
		panel1 = new LLamaInventoryMenu();
		panel2 = new VillagerMenu();
		panel3 = new CommandMenu();
		panel4 = new JPanel();
		label1 = new JLabel();
		uuidField = new JTextField();
		panel5 = new JPanel();
		label2 = new JLabel();
		encodingCheckbox = new JCheckBox();
		encodingField = new JTextField();
		generateButton = new JButton();
		scrollPane1 = new JScrollPane();
		sqlField = new JTextArea();
		panel6 = new JPanel();
		prevSelectionButton = new JButton();
		nextSelectionButton = new JButton();

		setMinimumSize(new Dimension(200, 300));
		setTitle("SQLlama");
		Container contentPane = getContentPane();
		contentPane.setLayout(new MigLayout(
			"hidemode 3",
			"[fill,grow]",
			"[grow,fill]" +
			"[fill]" +
			"[25!,fill]" +
			"[]" +
			"[25!,fill]" +
			"[30%!,grow,fill]" +
			"[]"));

		{
			entityTab.addTab("LLAMA", panel1);
			entityTab.addTab("VILLAGER", panel2);
			entityTab.addTab("COMMAND", panel3);
		}
		contentPane.add(entityTab, "cell 0 0");

		{
			panel4.setLayout(new MigLayout(
				"hidemode 3",
				"[20%!,grow,fill]" +
				"[grow,fill]",
				"[]"));

			label1.setText("UUID");
			panel4.add(label1, "cell 0 0");
			panel4.add(uuidField, "cell 1 0");
		}
		contentPane.add(panel4, "cell 0 1");

		{
			panel5.setLayout(new MigLayout(
				"hidemode 3",
				"[15%!,grow,fill]" +
				"[5%!,grow,fill]" +
				"[grow,fill]",
				"[grow,fill]"));

			label2.setText("Encoding");
			panel5.add(label2, "cell 0 0");

			encodingCheckbox.setHorizontalAlignment(SwingConstants.RIGHT);
			encodingCheckbox.addActionListener(e -> encodingCheckboxClick(e));
			panel5.add(encodingCheckbox, "cell 1 0");

			encodingField.setText("utf-8");
			encodingField.setEnabled(false);
			encodingField.setBackground(UIManager.getColor("TextField.disabledBackground"));
			panel5.add(encodingField, "cell 2 0");
		}
		contentPane.add(panel5, "cell 0 2");

		generateButton.setText("Generate!");
		generateButton.setBackground(Color.orange);
		generateButton.setForeground(new Color(153, 51, 0));
		generateButton.addActionListener(e -> generateButtonClick(e));
		contentPane.add(generateButton, "cell 0 4");

		{

			sqlField.setLineWrap(true);
			sqlField.setEditable(false);
			scrollPane1.setViewportView(sqlField);
		}
		contentPane.add(scrollPane1, "cell 0 5");

		{
			panel6.setLayout(new MigLayout(
				"hidemode 3",
				"[grow,fill]" +
				"[grow,fill]",
				"[25!,grow,fill]"));

			prevSelectionButton.setText("Prev");
			prevSelectionButton.addActionListener(e -> prevSelectionButtonClicked(e));
			panel6.add(prevSelectionButton, "cell 0 0");

			nextSelectionButton.setText("Next");
			nextSelectionButton.addActionListener(e -> nextSelectionButtonClicked(e));
			panel6.add(nextSelectionButton, "cell 1 0");
		}
		contentPane.add(panel6, "cell 0 6");
		setSize(960, 735);
		setLocationRelativeTo(getOwner());
	}

	private JTabbedPane entityTab;
	private LLamaInventoryMenu panel1;
	private VillagerMenu panel2;
	private CommandMenu panel3;
	private JPanel panel4;
	private JLabel label1;
	private JTextField uuidField;
	private JPanel panel5;
	private JLabel label2;
	private JCheckBox encodingCheckbox;
	private JTextField encodingField;
	private JButton generateButton;
	private JScrollPane scrollPane1;
	private JTextArea sqlField;
	private JPanel panel6;
	private JButton prevSelectionButton;
	private JButton nextSelectionButton;
}
