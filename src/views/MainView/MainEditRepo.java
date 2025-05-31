package views.MainView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import utils.Style;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class MainEditRepo {
    private MainFunc mainFunc;
    private MainView mainView;

    public MainEditRepo(MainFunc mainFunc, MainView mainView) {
        this.mainFunc = mainFunc;
        this.mainView = mainView;
    }

    public JPanel createPanel() {
        JTextField nameField = Style.createStyledTextField();
        JTextArea descField = Style.createStyledTextArea(3, 20);
        JComboBox<String> visibilityComboBox = new JComboBox<>(new String[] { "private", "public" });

        JLabel titleLabel = new JLabel("저장소 생성");
        titleLabel.setForeground(Style.PRIMARY_COLOR);
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel(new MigLayout("wrap 2", "[right][grow,fill]", "[]10[]10[]10[]"));
        panel.setBackground(Style.FIELD_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(350, 335));

        JScrollPane scrollPane = new JScrollPane(descField);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel, "span 2, center, gapbottom 20");
        panel.add(new JLabel("이름:"));
        panel.add(nameField, "growx, wmin 150");
        panel.add(new JLabel("설명:"));
        panel.add(scrollPane, "growx, h 80!");
        panel.add(new JLabel("권한:"));
        panel.add(visibilityComboBox, "growx, wmin 150");

        JButton cancelButton = Style.createStyledButton("취소", Style.PRIMARY_COLOR, Style.FIELD_BACKGROUND);
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> {
            mainFunc.loadRepositories();
            nameField.setText("");
            descField.setText("");
            mainView.toggleEditRepoPanel();
        });
        JButton saveButton = Style.createStyledButton("저장", Style.WARNING_COLOR, Style.FIELD_BACKGROUND);
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String description = descField.getText().trim();
            String visibility = (String) visibilityComboBox.getSelectedItem();

            mainFunc.handleAddRepo(name, description, visibility);
            mainView.toggleEditRepoPanel();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        panel.add(buttonPanel, "span 2, center, gapy 10");

        return panel;
    }
}
