package views;

import javax.swing.*;
import java.awt.*;
import database.UserDAO;
import utils.Style;

public class SignUpView extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JPasswordField confirmPasswordField;
	private JTextField emailField;
	private JButton signUpButton;
	private JButton cancelButton;
	private UserDAO userDAO;

	public SignUpView() {
		userDAO = new UserDAO();
		initializeUI();
	}

	private void initializeUI() {
		//전체 화면 구성
		setTitle("J.S.Repo - Sign Up");
		setSize(500, 580);
		setResizable(false); // 창 크기 고정

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);

		JLabel titleLabel = new JLabel("회원가입");
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		usernameField = Style.createStyledTextField();
		passwordField = Style.createStyledPasswordField();
		confirmPasswordField = Style.createStyledPasswordField();
		emailField = Style.createStyledTextField();

		signUpButton = Style.createStyledButton("회원가입", Style.PRIMARY_COLOR, Color.WHITE);
		cancelButton = Style.createStyledButton("취소", new Color(127, 140, 141), Color.WHITE);

		// 버튼
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(signUpButton);
		buttonPanel.add(cancelButton);

		// 입력 창
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

		// 컨테이너
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

		// 기능 연결
		signUpButton.addActionListener(e -> handleSignUp());
		cancelButton.addActionListener(e -> handleCancel());
	}

	private JPanel createLabelPanel(String text) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.setBackground(Style.BACKGROUND_COLOR);
		JLabel label = new JLabel(text);
		label.setFont(Style.LABEL_FONT);
		label.setForeground(new Color(60, 60, 60));
		panel.add(label);
		return panel;
	}

	private JPanel wrapInPanel(JComponent component) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(Style.BACKGROUND_COLOR);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	private void handleSignUp() {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());
		String confirmPassword = new String(confirmPasswordField.getPassword());
		String email = emailField.getText();
		
		// 오류 메시지
		// 빈 칸이 있을 경우
		if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
			showErrorDialog("정보를 입력해주세요.");
			return;
		}
		// 비밀번호 재입력에서 다를 경우
		if (!password.equals(confirmPassword)) {
			showErrorDialog("비밀번호가 일치하지 않습니다.");
			return;
		}
		// 이메일 형식이 아닐 경우
		if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
			showErrorDialog("유효한 이메일 주소를 입력해주세요.");
			return;
		}
		
		// 회원가입 시 데이터베이스에 저장
		if (userDAO.createUser(username, password, email)) {
			showSuccessDialog("회원가입이 완료되었습니다.");
			openLoginView();
		} else {
			showErrorDialog("회원가입 실패: 이미 존재하는 사용자 이름이거나 이메일입니다.");
		}
	}
	
	// 성공 및 오류 메시지 출력
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}
	private void showSuccessDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "성공", JOptionPane.INFORMATION_MESSAGE);
	}
	
	// 취소 및 성공 시 로그인 화면으로 이동
	private void handleCancel() {
		openLoginView();
	}
	private void openLoginView() {
		LoginView loginView = new LoginView();
		loginView.setVisible(true);
		this.dispose();
	}
}
