
package views.login_register;

import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import utils.Style;

public class LRCover extends javax.swing.JPanel {

    private final DecimalFormat df = new DecimalFormat("##0.###");
    private ActionListener event;
    private MigLayout layout;
    private JButton button;
    private boolean isLogin;

    public LRCover() {
        setOpaque(false);
        layout = new MigLayout("wrap, fill", "[center]", "push[]50[]10[]10[]push");
        setLayout(layout);
        init();
    }

    private void init() {

        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(Style.BACKGROUND_COLOR);
        ImageIcon logoIcon = new ImageIcon("src/icons/logo.png");
        Image scaledLogo = logoIcon.getImage().getScaledInstance(300-40, 160-40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledLogo);
        JLabel logoLabel = new JLabel(scaledIcon);
        add(logoLabel);

        // 로그인 회원가입 전환 버튼
        button = Style.createStyledButton("회원가입", Style.PRIMARY_COLOR, Color.WHITE);
        add(button, "w 45%, h 40");
        button.addActionListener(e -> {
            buttonActionPerformed(e);
        });
    }

    public void buttonActionPerformed(java.awt.event.ActionEvent evt) {
        event.actionPerformed(evt);
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs;
        GradientPaint gra = new GradientPaint(0, 0, new Color(165, 221, 255), 0, getHeight(), new Color(15, 114, 175));
        g2.setPaint(gra);
        g2.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(grphcs);
    }

    public void addEvent(ActionListener event) {
        this.event = event;
    }

    // 로그인 화면인지 아닌지 구분
    public void registerLeft(double v) {
        v = Double.valueOf(df.format(v));
        login(false);
    }
    public void registerRight(double v) {
        v = Double.valueOf(df.format(v));
        login(false);
    }
    public void loginLeft(double v) {
        v = Double.valueOf(df.format(v));
        login(true);
    }
    public void loginRight(double v) {
        v = Double.valueOf(df.format(v));
        login(true);
    }

    // 회원가입 창일 경우 버튼 = 로그인
    // 로그인 창일 경우 버튼 = 회원가입
    private void login(boolean login) {
        if (this.isLogin != login) {
            if (login) {
                button.setText("로그인");
            } else {
                button.setText("회원가입");
            }
            this.isLogin = login;
        }
    }
}
