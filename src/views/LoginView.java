package views;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;
import java.awt.event.*;

import models.User;
import utils.Style;
import utils.ClientSock;

public class LoginView extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton signUpButton;

	public LoginView() {
		ClientSock.connect();
		initializeUI();
	}

	private void initializeUI() {
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

		gbc.gridy++;
		JLabel userLabel = new JLabel("사용자 이름");
		userLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(userLabel, gbc);

		gbc.gridy++;
		usernameField = Style.createStyledTextField();
		mainPanel.add(usernameField, gbc);

		gbc.gridy++;
		JLabel passLabel = new JLabel("비밀번호");
		passLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(passLabel, gbc);

		gbc.gridy++;
		passwordField = Style.createStyledPasswordField();
		mainPanel.add(passwordField, gbc);

		gbc.gridy++;
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		loginButton = Style.createStyledButton("로그인", Style.PRIMARY_COLOR, Color.WHITE);
		signUpButton = Style.createStyledButton("회원가입", new Color(127, 140, 141), Color.WHITE);
		buttonPanel.add(loginButton);
		buttonPanel.add(signUpButton);
		mainPanel.add(buttonPanel, gbc);

		add(mainPanel);

		loginButton.addActionListener(e -> handleLogin());
		signUpButton.addActionListener(e -> openSignUpView());

		getRootPane().setDefaultButton(loginButton);
	}

	private void handleLogin() {
		String username = usernameField.getText().trim();
		String password = new String(passwordField.getPassword()).trim();

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this, "사용자 이름과 비밀번호를 입력해주세요.");
			return;
		}

		try {
			ClientSock.sendCommand(":c:login");
			ClientSock.sendCommand(username);
			ClientSock.sendCommand(password);
			String response = ClientSock.receiveResponse();

			if (response != null && response.startsWith("/#/info")) {
				User user = new User();
				user.setUsername(username); // 로그인 성공 시 사용자 정보 저장

				JOptionPane.showMessageDialog(this, "로그인 성공");
				new MainView(user).setVisible(true); // client 객체 전달
				this.dispose();

			} else if (response != null && response.startsWith("/#/error")) {
				String msg = response.replace("/#/error", "").trim();
				showErrorDialog(msg+": 사용자 이름 또는 비밀번호가 일치하지 않습니다.");
			} else {
				showErrorDialog(response);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "서버 연결 실패");
		}
	}

	private void openSignUpView() {
		new SignUpView().setVisible(true);
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
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}
}
