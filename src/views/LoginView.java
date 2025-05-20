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

// 로그인 화면을 구성하는 JFrame 클래스
public class LoginView extends JFrame {
	private JTextField usernameField; // 사용자 이름 입력 필드
	private JPasswordField passwordField; // 비밀번호 입력 필드
	private JButton loginButton; // 로그인 버튼
	private JButton signUpButton; // 회원가입 버튼

	// 생성자: 서버 연결 후 UI 초기화
	public LoginView() {
		ClientSock.connect(); // 서버와 소켓 연결
		initializeUI(); // UI 구성 메서드 호출
	}

	// UI 초기화 메서드
	private void initializeUI() {
		setTitle("J.S.Repo - Login"); // 창 제목 설정
		setSize(500, 400); // 창 크기 설정
		setResizable(false); // 창 크기 조정 불가
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null); // 화면 중앙에 위치

		// 메인 패널: GridBagLayout 사용하여 요소 배치
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 20, 10, 20); // 요소 간 여백
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// 상단 타이틀
		JLabel titleLabel = new JLabel("J.S.Repository");
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		mainPanel.add(titleLabel, gbc);

		// 사용자 이름 입력 라벨과 필드
		gbc.gridy++;
		JLabel userLabel = new JLabel("사용자 이름");
		userLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(userLabel, gbc);

		gbc.gridy++;
		usernameField = Style.createStyledTextField(); // 사용자 정의 스타일 적용
		mainPanel.add(usernameField, gbc);

		// 비밀번호 입력 라벨과 필드
		gbc.gridy++;
		JLabel passLabel = new JLabel("비밀번호");
		passLabel.setFont(Style.LABEL_FONT);
		mainPanel.add(passLabel, gbc);

		gbc.gridy++;
		passwordField = Style.createStyledPasswordField(); // 사용자 정의 스타일 적용
		mainPanel.add(passwordField, gbc);

		// 로그인/회원가입 버튼 패널
		gbc.gridy++;
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		loginButton = Style.createStyledButton("로그인", Style.PRIMARY_COLOR, Color.WHITE);
		signUpButton = Style.createStyledButton("회원가입", new Color(127, 140, 141), Color.WHITE);
		buttonPanel.add(loginButton);
		buttonPanel.add(signUpButton);
		mainPanel.add(buttonPanel, gbc);

		add(mainPanel); // 메인 패널을 JFrame에 추가

		// 버튼 클릭 이벤트 연결
		loginButton.addActionListener(e -> handleLogin());
		signUpButton.addActionListener(e -> openSignUpView());

		// 엔터 키 입력 시 로그인 동작
		getRootPane().setDefaultButton(loginButton);
	}

	// 로그인 처리 메서드
	private void handleLogin() {
		String username = usernameField.getText().trim();
		String password = new String(passwordField.getPassword()).trim();

		// 입력값 검증
		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this, "사용자 이름과 비밀번호를 입력해주세요.");
			return;
		}

		try {
			// 서버에 로그인 명령 전송
			ClientSock.sendCommand(":c:login");
			ClientSock.sendCommand(username);
			ClientSock.sendCommand(password);
			String response = ClientSock.receiveResponse(); // 서버 응답 수신

			// 로그인 성공
			if (response != null && response.startsWith("/#/info")) {
				User user = new User();
				user.setUsername(username); // 로그인한 사용자 정보 저장

				JOptionPane.showMessageDialog(this, "로그인 성공");
				new MainView(user).setVisible(true); // 메인 화면으로 이동
				this.dispose(); // 로그인 창 닫기

				// 로그인 실패 - 오류 메시지
			} else if (response != null && response.startsWith("/#/error")) {
				String msg = response.replace("/#/error", "").trim();
				showErrorDialog(msg + ": 사용자 이름 또는 비밀번호가 일치하지 않습니다.");

				// 기타 알 수 없는 응답
			} else {
				showErrorDialog(response);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "서버 연결 실패");
		}
	}

	// 회원가입 화면 열기
	private void openSignUpView() {
		new SignUpView().setVisible(true); // 회원가입 창 열기
		this.dispose(); // 로그인 창 닫기
	}

	// 프로그램 진입점 (main)
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel()); // Nimbus 테마 적용
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> new LoginView().setVisible(true)); // 로그인 창 실행
	}

	// 오류 메시지를 다이얼로그로 표시
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}
}
