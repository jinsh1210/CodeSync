package views;

import javax.swing.*;
import java.awt.*;
import database.UserDAO;

public class SignUpView extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JPasswordField confirmPasswordField;
	private JTextField emailField;
	private JButton signUpButton;
	private JButton cancelButton;
	private UserDAO userDAO;

	private static final Color PRIMARY_COLOR = new Color(52, 152, 219);
	private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	private static final Color FIELD_BACKGROUND = Color.WHITE;
	private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
	private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16);

	public SignUpView() {
		userDAO = new UserDAO();
		initializeUI();
	}

	private void initializeUI() {
		setTitle("File Sharing System - Sign Up");
		setSize(500, 580);
		setResizable(false); // 🔒 창 크기 고정 (사용자가 크기 변경 불가)

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
		mainPanel.setBackground(BACKGROUND_COLOR);

		JLabel titleLabel = new JLabel("회원가입");
		titleLabel.setFont(TITLE_FONT);
		titleLabel.setForeground(PRIMARY_COLOR);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		usernameField = createStyledTextField();
		passwordField = createStyledPasswordField();
		confirmPasswordField = createStyledPasswordField();
		emailField = createStyledTextField();

		signUpButton = createStyledButton("회원가입", PRIMARY_COLOR, Color.WHITE);
		cancelButton = createStyledButton("취소", new Color(127, 140, 141), Color.WHITE);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		buttonPanel.add(signUpButton);
		buttonPanel.add(cancelButton);

		mainPanel.add(titleLabel);
		mainPanel.add(Box.createVerticalStrut(30));

		mainPanel.add(createLabelPanel("사용자 이름"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(wrapInPanel(usernameField));
		mainPanel.add(Box.createVerticalStrut(20));

		mainPanel.add(createLabelPanel("비밀번호"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(wrapInPanel(passwordField));
		mainPanel.add(Box.createVerticalStrut(20));

		mainPanel.add(createLabelPanel("비밀번호 확인"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(wrapInPanel(confirmPasswordField));
		mainPanel.add(Box.createVerticalStrut(20));

		mainPanel.add(createLabelPanel("이메일"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(wrapInPanel(emailField));
		mainPanel.add(Box.createVerticalStrut(40));

		mainPanel.add(buttonPanel);

		// 컨테이너 (배경 그라데이션)
		JPanel containerPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				int w = getWidth();
				int h = getHeight();
				GradientPaint gp = new GradientPaint(0, 0, new Color(240, 240, 255), 0, h, new Color(255, 255, 255));
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, w, h);
			}
		};
		containerPanel.setLayout(new BorderLayout());
		containerPanel.add(mainPanel, BorderLayout.CENTER);
		add(containerPanel);

		signUpButton.addActionListener(e -> handleSignUp());
		cancelButton.addActionListener(e -> handleCancel());
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
		button.setPreferredSize(new Dimension(130, 40));
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);   // 🔧 배경을 채움
	    button.setOpaque(true);              // 🔧 불투명으로 설정
		return button;
	}

	private JPanel createLabelPanel(String text) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.setBackground(BACKGROUND_COLOR);
		JLabel label = new JLabel(text);
		label.setFont(LABEL_FONT);
		label.setForeground(new Color(60, 60, 60));
		panel.add(label);
		return panel;
	}

	private JPanel wrapInPanel(JComponent component) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(BACKGROUND_COLOR);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	private void handleSignUp() {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());
		String confirmPassword = new String(confirmPasswordField.getPassword());
		String email = emailField.getText();

		if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
			showErrorDialog("정보 를 입력해주세요.");
			return;
		}

		if (!password.equals(confirmPassword)) {
			showErrorDialog("비밀번호가 일치하지 않습니다.");
			return;
		}

		if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
			showErrorDialog("유효한 이메일 주소를 입력해주세요.");
			return;
		}

		if (userDAO.createUser(username, password, email)) {
			showSuccessDialog("회원가입이 완료되었습니다.");
			openLoginView();
		} else {
			showErrorDialog("회원가입 실패: 이미 존재하는 사용자 이름이거나 이메일입니다.");
		}
	}

	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}

	private void showSuccessDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "성공", JOptionPane.INFORMATION_MESSAGE);
	}

	private void handleCancel() {
		openLoginView();
	}

	private void openLoginView() {
		LoginView loginView = new LoginView();
		loginView.setVisible(true);
		this.dispose();
	}
}
