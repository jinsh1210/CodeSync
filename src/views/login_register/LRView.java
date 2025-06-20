
package views.login_register;

import net.miginfocom.swing.MigLayout;

import java.awt.*;
import javax.swing.*;

import lombok.*;
import utils.*;

@Getter
@Setter

public class LRView extends javax.swing.JLayeredPane {

    private JTextField upusernameField;
    private JPasswordField uppasswordField;
    private JPasswordField upconfirmPasswordField;

    private JTextField inusernameField;
    private JPasswordField inpasswordField;

    // LRView 생성자
    public LRView() {
        ClientSock.connect();
        LRFunc.setLRView(this);
        initComponents();
        initLogin();
        initRegister();
        register.setVisible(false);
        login.setVisible(true);
    }

    // 회원가입 화면 초기화
    private void initRegister() {
        register.setLayout(new MigLayout("wrap", "push[center]push", "push[]25[]10[]10[]10[]10[]10[]30[]push"));
        // 제목
        JLabel label = new JLabel("회원가입");
        label.setFont(Style.TITLE_FONT);
        label.setForeground(Style.PRIMARY_COLOR);
        register.add(label);

        // 아이디(유저이름)
        JLabel labelusername = new JLabel("아이디");
        labelusername.setFont(Style.LABEL_FONT);
        labelusername.setForeground(Color.BLACK);
        register.add(labelusername, "w 60%");
        upusernameField = Style.createStyledTextField();
        register.add(upusernameField, "w 60%");

        // 비밀번호
        JLabel labelpassword = new JLabel("비밀번호");
        labelpassword.setFont(Style.LABEL_FONT);
        labelpassword.setForeground(Color.BLACK);
        register.add(labelpassword, "w 60%");
        uppasswordField = Style.createStyledPasswordField();
        register.add(uppasswordField, "w 60%");

        // 비밀번호 확인
        JLabel labelconfirmpassword = new JLabel("비밀번호 확인");
        labelconfirmpassword.setFont(Style.LABEL_FONT);
        labelconfirmpassword.setForeground(Color.BLACK);
        register.add(labelconfirmpassword, "w 60%");
        upconfirmPasswordField = Style.createStyledPasswordField();
        register.add(upconfirmPasswordField, "w 60%");

        // 회원가입 버튼
        JButton signUpButton = Style.createStyledButton("회원가입", Style.PRIMARY_COLOR, Color.WHITE);
        signUpButton.addActionListener(e -> LRFunc.handleSignUp());
        upusernameField.addActionListener(e -> LRFunc.handleSignUp());
        uppasswordField.addActionListener(e -> LRFunc.handleSignUp());
        upconfirmPasswordField.addActionListener(e -> LRFunc.handleSignUp());
        register.add(signUpButton, "w 30%, h 40");
    }

    // 로그인 화면 초기화
    private void initLogin() {
        login.setLayout(new MigLayout("wrap", "push[center]push", "push[]25[]10[]10[]10[]30[]push"));
        // 제목
        JLabel label = new JLabel("로그인");
        label.setFont(Style.TITLE_FONT);
        label.setForeground(Style.PRIMARY_COLOR);
        login.add(label);

        // 아이디(유저이름)
        JLabel labelusername = new JLabel("아이디");
        labelusername.setFont(Style.LABEL_FONT);
        labelusername.setForeground(Color.BLACK);
        login.add(labelusername, "w 60%");
        inusernameField = Style.createStyledTextField();
        login.add(inusernameField, "w 60%");

        // 비밀번호
        JLabel labelpassword = new JLabel("비밀번호");
        labelpassword.setFont(Style.LABEL_FONT);
        labelpassword.setForeground(Color.BLACK);
        login.add(labelpassword, "w 60%");
        inpasswordField = Style.createStyledPasswordField();
        login.add(inpasswordField, "w 60%");

        // 로그인 버튼
        JButton loginButton = Style.createStyledButton("로그인", Style.PRIMARY_COLOR, Color.WHITE);
        loginButton.addActionListener(e -> LRFunc.handleLogin());
        inusernameField.addActionListener(e-> LRFunc.handleLogin());
        inpasswordField.addActionListener(e-> LRFunc.handleLogin());
        login.add(loginButton, "w 30%, h 40");
    }

    // 애니메이션 작동 시 로그인 <-> 회원가입 전환 로직
    public void showRegister(boolean show) {
        if (show) {
            register.setVisible(false);
            login.setVisible(true);
        } else {
            register.setVisible(true);
            login.setVisible(false);
        }
    }

    // 컴포넌트 초기화
    private void initComponents() {

        login = new javax.swing.JPanel();
        register = new javax.swing.JPanel();
        setLayout(new java.awt.CardLayout());

        add(login, "card3");
        add(register, "card2");
        login.setOpaque(false);
        register.setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int width = getWidth();
        int height = getHeight();
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(255, 255, 255), // 시작 색
                width, height, new Color(255, 255, 255) // 끝 색
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);
        g2.dispose();
    }
    private javax.swing.JPanel login;
    private javax.swing.JPanel register;

}
