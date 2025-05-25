package views;

import javax.swing.*;
import java.awt.*;
import utils.Style;
import utils.ClientSock;
import utils.DarkModeManager;

// SignUpView 클래스 - 사용자 회원가입을 처리하는 UI 프레임
public class SignUpView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton signUpButton;
    private JButton cancelButton;

    // 생성자 - UI 초기화
    public SignUpView() {
        initializeUI();
        // 🌙 다크모드 적용
        DarkModeManager.apply(getContentPane());
    }

    // UI 구성 및 이벤트 등록
    private void initializeUI() {
        setTitle("J.S.Repo - Sign Up");
        setSize(400, 500);
        setResizable(false);
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

        signUpButton = Style.createStyledButton("회원가입", Style.PRIMARY_COLOR, Color.WHITE);
        cancelButton = Style.createStyledButton("취소", Style.PRIMARY_COLOR, Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(Style.BACKGROUND_COLOR);
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

        mainPanel.add(buttonPanel);

        // 배경에 그라디언트 효과 추가
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

        // 버튼 클릭 이벤트 등록
        signUpButton.addActionListener(e -> handleSignUp());
        cancelButton.addActionListener(e -> openLoginView());
    }

    // 라벨이 포함된 패널 생성
    private JPanel createLabelPanel(String text) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(Style.BACKGROUND_COLOR);
        JLabel label = new JLabel(text);
        label.setFont(Style.LABEL_FONT);
        label.setForeground(new Color(60, 60, 60));
        panel.add(label);
        return panel;
    }

    // 입력 필드를 BorderLayout에 감싸는 패널 생성
    private JPanel wrapInPanel(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Style.BACKGROUND_COLOR);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    // 회원가입 처리 로직
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("정보를 입력해주세요.");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            showErrorDialog("아이디는 영문자, 숫자만 사용할 수 있습니다.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showErrorDialog("비밀번호가 일치하지 않습니다.");
            return;
        }

        int count = 0;
        if (password.matches(".*[A-Z].*")) count++;
        if (password.matches(".*[a-z].*")) count++;
        if (password.matches(".*[^a-zA-Z0-9].*")) count++;

        if (password.length() < 8 || count < 2) {
            showErrorDialog("비밀번호는 8자 이상이며 대문자, 소문자, 특수문자 중 2종류 이상을 포함해야 합니다.");
            return;
        }

        try {
            String id = username;
            String pwd = password;

            ClientSock.sendCommand(":c:sign_up");
            ClientSock.sendCommand(id);
            ClientSock.sendCommand(pwd);

            String response = ClientSock.receiveResponse();
            if (response != null && response.startsWith("/#/info")) {
                showSuccessDialog("회원가입이 완료되었습니다.");
                openLoginView();
            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim();
                showErrorDialog("회원가입 실패: " + msg);
            } else {
                showErrorDialog(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("서버 연결 실패");
        }
    }

    // 에러 메시지 팝업 표시
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    // 성공 메시지 팝업 표시
    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "성공", JOptionPane.INFORMATION_MESSAGE);
    }

    // 로그인 화면으로 돌아가기
    private void openLoginView() {
        new LoginView().setVisible(true);
        this.dispose();
    }
}
