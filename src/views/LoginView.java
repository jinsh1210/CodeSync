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
    
    public LoginView() {
        userDAO = new UserDAO();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("파일 공유 시스템 - 로그인");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 메인 패널
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        // 로고 또는 타이틀
        JLabel titleLabel = new JLabel("File Sharing System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 입력 필드들
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // 버튼들
        loginButton = new JButton("로그인");
        signUpButton = new JButton("회원가입");
        
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 컴포넌트 배치
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(new JLabel("사용자 이름:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(new JLabel("비밀번호:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(signUpButton);
        
        add(mainPanel);
        
        // 이벤트 리스너
        loginButton.addActionListener(e -> handleLogin());
        signUpButton.addActionListener(e -> openSignUpView());
        
        // Enter 키로 로그인
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
        SwingUtilities.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
} 