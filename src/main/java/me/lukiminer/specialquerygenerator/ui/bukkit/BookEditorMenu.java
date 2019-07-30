package me.lukiminer.specialquerygenerator.ui.bukkit;

import com.google.common.collect.Lists;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

class BookEditorMenu extends JFrame {

	private Consumer<java.util.List<String>> callback;

	private List<String> pages = Lists.newArrayList();

	private int oldPageIndex = 0;

	public class DocumentSizeFilter extends DocumentFilter {
		int maxCharacters;

		DocumentSizeFilter(int maxChars) {
			maxCharacters = maxChars;
		}

		public void insertString(FilterBypass fb, int offs,
								 String str, AttributeSet a)
				throws BadLocationException {

			if ((fb.getDocument().getLength() + str.length()) <= maxCharacters) {
				super.insertString(fb, offs, str, a);
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		}

		public void replace(DocumentFilter.FilterBypass fb, int offs,
							int length,
							String str, AttributeSet a)
				throws BadLocationException {

			if ((fb.getDocument().getLength() + str.length()
					- length) <= maxCharacters) {
				super.replace(fb, offs, length, str, a);
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		}

	}

	BookEditorMenu(List<String> pages, Consumer<java.util.List<String>> callback) {
		initComponents();
		this.callback = callback;

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});

		DefaultStyledDocument document = new DefaultStyledDocument();
		document.setDocumentFilter(new DocumentSizeFilter(255));
		textArea.setDocument(document);

		if (pages != null) {
			this.pages = Lists.newArrayList(pages);
		}
		handlePageChange(0);
	}

	private void nextPageButtonClicked(ActionEvent e) {
		int page = (int) pageSpinner.getValue();
		if (page < 50) {
			page += 1;
		}
		pageSpinner.setValue(page);
		handlePageChange(page - 1);
	}

	private void prevPageButtonClicked(ActionEvent e) {
		int page = (int) pageSpinner.getValue();
		if (page > 1) {
			page -= 1;
		}
		pageSpinner.setValue(page);
		handlePageChange(page - 1);
	}

	private void pageSpinnerStateChanged(ChangeEvent e) {
		int spinnerValue = (int) pageSpinner.getValue();
		savePage(oldPageIndex);
		oldPageIndex = spinnerValue - 1;
		handlePageChange(oldPageIndex);
	}

	private void handlePageChange(int indexPage) {
		String page = "";
		if (indexPage >= pages.size()) {
			for (int i = pages.size() - 1; i < indexPage; i++) {
				pages.add("");
			}
		} else {
			page = pages.get(indexPage);
		}
		textArea.setText(page);
	}

	private void savePage(int indexPage) {
		if (pages.size() == 0) {
			return;
		}
		String page = textArea.getText();
		pages.set(indexPage, page);
	}

	private void textAreaCaretUpdate(CaretEvent e) {
		String text = textArea.getText();
		if (text == null) {
			return;
		}
		if (text.length() > 255) {
			textArea.setText(text.substring(0, 255));
		}
	}

	private void filterEmptyPages() {
		for (int i = pages.size() - 1; i > 0; i--) {
			String text = pages.get(i);
			if (text == null || text.isEmpty()) {
				pages.remove(i);
			}
		}
	}

	private void saveButtonActionClicked(ActionEvent e) {
		savePage(oldPageIndex);
		close();
	}

	private void close() {
		this.onClose();
		this.setVisible(false);
		this.dispose();
	}

	private void onClose() {
		if (callback != null) {
			callback.accept(this.pages);
		}
	}

	private void resetButtonActionClicked(ActionEvent e) {
		pageSpinner.setValue(1);
		pages = Lists.newArrayList("");
		handlePageChange(0);
	}

	private void initComponents() {
		JScrollPane scrollPane1 = new JScrollPane();
		textArea = new JTextArea();
		JPanel panel1 = new JPanel();
		JButton prevPageButton = new JButton();
		pageSpinner = new JSpinner();
		JButton nextPageButton = new JButton();
		JPanel panel2 = new JPanel();
		JButton saveButton = new JButton();
		JButton resetButton = new JButton();

		setTitle("BookEditor");
		Container contentPane = getContentPane();
		contentPane.setLayout(new MigLayout(
			"hidemode 3",
			"[fill,grow]",
			"[80%!,grow,fill]" +
			"[grow,fill]" +
			"[grow,fill]"));

		{

			textArea.setLineWrap(true);
			textArea.addCaretListener(this::textAreaCaretUpdate);
			scrollPane1.setViewportView(textArea);
		}
		contentPane.add(scrollPane1, "cell 0 0");

		{
			panel1.setLayout(new MigLayout(
				"hidemode 3",
				"[grow,fill]" +
				"[grow,fill]" +
				"[grow,fill]",
				"[]"));

			prevPageButton.setText("<=");
			prevPageButton.addActionListener(this::prevPageButtonClicked);
			panel1.add(prevPageButton, "cell 0 0");

			pageSpinner.setModel(new SpinnerNumberModel(1, 1, 50, 1));
			pageSpinner.addChangeListener(this::pageSpinnerStateChanged);
			panel1.add(pageSpinner, "cell 1 0");

			nextPageButton.setText("=>");
			nextPageButton.addActionListener(this::nextPageButtonClicked);
			panel1.add(nextPageButton, "cell 2 0");
		}
		contentPane.add(panel1, "cell 0 1");

		{
			panel2.setLayout(new MigLayout(
				"hidemode 3",
				"[grow,fill]" +
				"[grow,fill]",
				"[grow,fill]"));

			saveButton.setText("Save");
			saveButton.setForeground(new Color(0, 102, 0));
			saveButton.setBackground(new Color(0, 204, 0));
			saveButton.addActionListener(this::saveButtonActionClicked);
			panel2.add(saveButton, "cell 0 0");

			resetButton.setText("Reset");
			resetButton.setForeground(new Color(153, 0, 0));
			resetButton.setBackground(new Color(255, 51, 51));
			resetButton.addActionListener(this::resetButtonActionClicked);
			panel2.add(resetButton, "cell 1 0");
		}
		contentPane.add(panel2, "cell 0 2");
		setSize(400, 505);
		setLocationRelativeTo(getOwner());
	}

	private JTextArea textArea;
	private JSpinner pageSpinner;
}
