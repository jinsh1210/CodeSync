package views;

import javax.swing.*;
import java.awt.*;
import database.UserDAO;
import models.User;

public class LoginView extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton signUpButton;
	private UserDAO userDAO;

	private static final Color PRIMARY_COLOR = new Color(52, 152, 219);
	private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	private static final Color FIELD_BACKGROUND = Color.WHITE;
	private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
	private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16);

	public LoginView() {
		userDAO = new UserDAO();
		initializeUI();
	}

	private void initializeUI() {
		setTitle("J.S.Repo - Login");
		setSize(500, 400);
		setResizable(false); // 🔒 창 크기 고정 (사용자가 크기 변경 불가)

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBackground(BACKGROUND_COLOR);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 20, 10, 20);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel titleLabel = new JLabel("J.S.Repository");
		titleLabel.setFont(TITLE_FONT);
		titleLabel.setForeground(PRIMARY_COLOR);
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		mainPanel.add(titleLabel, gbc);

		gbc.gridwidth = 2;
		gbc.gridy++;

		// 사용자 이름
		JLabel userLabel = new JLabel("사용자 이름");
		userLabel.setFont(LABEL_FONT);
		mainPanel.add(userLabel, gbc);

		gbc.gridy++;
		usernameField = createStyledTextField();
		mainPanel.add(usernameField, gbc);

		// 비밀번호
		gbc.gridy++;
		JLabel passLabel = new JLabel("비밀번호");
		passLabel.setFont(LABEL_FONT);
		mainPanel.add(passLabel, gbc);

		gbc.gridy++;
		passwordField = createStyledPasswordField();
		mainPanel.add(passwordField, gbc);

		// 버튼
		gbc.gridy++;
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		loginButton = createStyledButton("로그인", PRIMARY_COLOR, Color.WHITE);
		signUpButton = createStyledButton("회원가입", new Color(127, 140, 141), Color.WHITE);
		buttonPanel.add(loginButton);
		buttonPanel.add(signUpButton);

		mainPanel.add(buttonPanel, gbc);
		add(mainPanel);

		loginButton.addActionListener(e -> handleLogin());
		signUpButton.addActionListener(e -> openSignUpView());

		getRootPane().setDefaultButton(loginButton);
	}

	private void handleLogin() {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this, "사용자 이름과 비밀번호를 입력해주세요.");
			return;
		}

		User user = userDAO.getUser(username, password);
		if (user != null) {
			MainView mainView = new MainView(user);
			mainView.setVisible(true);
			this.dispose();
		} else {
			JOptionPane.showMessageDialog(this, "로그인 실패: 사용자 이름 또는 비밀번호가 잘못되었습니다.");
		}
	}

	private void openSignUpView() {
		SignUpView signUpView = new SignUpView();
		signUpView.setVisible(true);
		this.dispose();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
	}

	private JTextField createStyledTextField() {
		JTextField field = new JTextField(20);
		field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		field.setBackground(FIELD_BACKGROUND);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return field;
	}

	private JPasswordField createStyledPasswordField() {
		JPasswordField field = new JPasswordField(20);
		field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		field.setBackground(FIELD_BACKGROUND);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return field;
	}

	private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
		JButton button = new JButton(text);
		button.setFont(BUTTON_FONT);
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);   // 🔧 배경을 채움
	    button.setOpaque(true);              // 🔧 불투명으로 설정
		button.setPreferredSize(new Dimension(130, 40));
		return button;
	}
}
