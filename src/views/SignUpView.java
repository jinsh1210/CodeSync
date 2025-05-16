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
    
    public SignUpView() {
        userDAO = new UserDAO();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("파일 공유 시스템 - 회원가입");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 메인 패널
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // 입력 필드들
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        emailField = new JTextField(20);
        
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // 버튼들을 담을 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        signUpButton = new JButton("회원가입");
        cancelButton = new JButton("취소");
        
        buttonPanel.add(signUpButton);
        buttonPanel.add(Box.createHorizontalStrut(10)); // 버튼 사이 간격
        buttonPanel.add(cancelButton);
        
        // 컴포넌트 배치
        mainPanel.add(new JLabel("사용자 이름:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(15));
        
        mainPanel.add(new JLabel("비밀번호:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(15));
        
        mainPanel.add(new JLabel("비밀번호 확인:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(confirmPasswordField);
        mainPanel.add(Box.createVerticalStrut(15));
        
        mainPanel.add(new JLabel("이메일:"));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(emailField);
        mainPanel.add(Box.createVerticalStrut(25));
        
        mainPanel.add(buttonPanel);
        
        add(mainPanel);
        
        // 이벤트 리스너
        signUpButton.addActionListener(e -> handleSignUp());
        cancelButton.addActionListener(e -> handleCancel());
    }
    
    private void handleSignUp() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String email = emailField.getText();
        
        // 입력 검증
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 필드를 입력해주세요.");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
            return;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "유효한 이메일 주소를 입력해주세요.");
            return;
        }
        
        // 회원가입 처리
        if (userDAO.createUser(username, password, email)) {
            JOptionPane.showMessageDialog(this, "회원가입이 완료되었습니다.");
            openLoginView();
        } else {
            JOptionPane.showMessageDialog(this, "회원가입 실패: 이미 존재하는 사용자 이름이거나 이메일입니다.");
        }
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