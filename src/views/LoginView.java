package views;

import java.awt.*;
import javax.swing.*;
import models.User;
import utils.Style;
// import utils.UserSettings;
import utils.ClientSock;
import utils.DarkModeManager;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signUpButton;

    public LoginView() {
        ClientSock.connect();
        initializeUI();
        // 🌙 다크모드 적용
        DarkModeManager.apply(getContentPane());
    }

    private void initializeUI() {
        setupFrame();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Style.BACKGROUND_COLOR);

        GridBagConstraints gbc = createGbc(0, 0, 2);
        JLabel titleLabel = new JLabel("J.S.Repository");
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setForeground(Style.PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, gbc);

        gbc.gridy++;
        mainPanel.add(createLabel("사용자 이름"), gbc);
        gbc.gridy++;
        usernameField = Style.createStyledTextField();
        mainPanel.add(usernameField, gbc);

        gbc.gridy++;
        mainPanel.add(createLabel("비밀번호"), gbc);
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
        attachListeners();
        getRootPane().setDefaultButton(loginButton);
    }

    private void setupFrame() {
        setTitle("J.S.Repo - Login");
        setSize(500, 400);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Style.LABEL_FONT);
        return label;
    }

    private GridBagConstraints createGbc(int x, int y, int width) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void attachListeners() {
        loginButton.addActionListener(e -> handleLogin());
        signUpButton.addActionListener(e -> openSignUpView());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("사용자 이름과 비밀번호를 입력해주세요.");
            return;
        }

        try {
            ClientSock.sendCommand(":c:login");
            ClientSock.sendCommand(username);
            ClientSock.sendCommand(password);
            String response = ClientSock.receiveResponse();

            if (response != null && response.startsWith("/#/info")) {
                User user = new User();
                user.setUsername(username);
                
                //TODO: 서버에서 저장(DB가 나을 듯)
                /* boolean userDarkMode = UserSettings.loadDarkMode(username);
                Style.isDarkMode = userDarkMode;
                DarkModeManager.setDarkMode(userDarkMode); */

                JOptionPane.showMessageDialog(this, "로그인 성공");
                new MainView(user).setVisible(true);
                this.dispose();

            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim();
                showErrorDialog(msg + ": 사용자 이름 또는 비밀번호가 일치하지 않습니다.");
            } else {
                showErrorDialog(response);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorDialog("서버 연결 실패");
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
