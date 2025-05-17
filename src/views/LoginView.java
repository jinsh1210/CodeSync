package views;

import javax.swing.*;
import java.awt.*;
import database.UserDAO;
import models.User;
import utils.Style;

public class LoginView extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton signUpButton;
	private UserDAO userDAO;

	public LoginView() {
		userDAO = new UserDAO();
		initializeUI();
	}

	private void initializeUI() {
		//전체 화면 구성
		setTitle("J.S.Repo - Login");
		setSize(500, 400);
		setResizable(false);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 20, 10, 20);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel titleLabel = new JLabel("J.S.Repository");
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);
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
		userLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(userLabel, gbc);

		gbc.gridy++;
		usernameField = Style.createStyledTextField();
		mainPanel.add(usernameField, gbc);

		// 비밀번호
		gbc.gridy++;
		JLabel passLabel = new JLabel("비밀번호");
		passLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(passLabel, gbc);

		gbc.gridy++;
		passwordField = Style.createStyledPasswordField();
		mainPanel.add(passwordField, gbc);

		// 버튼
		gbc.gridy++;
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		loginButton = Style.createStyledButton("로그인", Style.PRIMARY_COLOR, Color.WHITE);
		signUpButton = Style.createStyledButton("회원가입", new Color(127, 140, 141), Color.WHITE);
		buttonPanel.add(loginButton);
		buttonPanel.add(signUpButton);

		mainPanel.add(buttonPanel, gbc);
		add(mainPanel);

		// 기능 연결
		loginButton.addActionListener(e -> handleLogin());
		signUpButton.addActionListener(e -> openSignUpView());

		getRootPane().setDefaultButton(loginButton);
	}
	//로그인 기능
	private void handleLogin() {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this, "사용자 이름과 비밀번호를 입력해주세요.");
			return;
		}
		// 데이터베이스에서 받아오는 로직
		User user = userDAO.getUser(username, password);
		if (user != null) {
			MainView mainView = new MainView(user);
			mainView.setVisible(true);
			this.dispose();
		} else {
			JOptionPane.showMessageDialog(this, "로그인 실패: 사용자 이름 또는 비밀번호가 잘못되었습니다.");
		}
	}
	//회원가입 화면 이동
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
}
