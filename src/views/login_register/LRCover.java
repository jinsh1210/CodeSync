
package views.login_register;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;
import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import utils.Style;

public class LRCover extends javax.swing.JPanel {

    private final DecimalFormat df = new DecimalFormat("##0.###");
    private ActionListener event;
    private MigLayout layout;
    private JButton button;
    private boolean isLogin;
    private LRView lrView;

    public void setLRView(LRView view) {
        lrView = view;
    }

    // 패널 표현 형식
    public LRCover(LRView view) {
        this.lrView = view;
        setOpaque(false);
        layout = new MigLayout("wrap, fill", "[center]", "push[]50[]10[]10[]push");
        setLayout(layout);
        init();
    }

    // 커버 패널
    private void init() {

        // 로고 사진 패널
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(Style.BACKGROUND_COLOR);
        ImageIcon logoIcon = recolorImage("src/icons/logo.png", new Color(255, 255, 225));
        Image scaledLogo = logoIcon.getImage().getScaledInstance(300 - 20, 160 - 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledLogo);
        JLabel logoLabel = new JLabel(scaledIcon);
        add(logoLabel);

        // 로그인 회원가입 전환 버튼
        button = Style.createStyledButton("회원가입", Style.BACKGROUND_COLOR, Color.DARK_GRAY);
        add(button, "w 45%, h 40");
        button.addActionListener(e -> {
            if (event != null) {
                event.actionPerformed(e);
            }
            // 전환 시 모든 텍스트 필드 공백
            lrView.getUpusernameField().setText("");
            lrView.getUppasswordField().setText("");
            lrView.getUpconfirmPasswordField().setText("");

            lrView.getInusernameField().setText("");
            lrView.getInpasswordField().setText("");

        });
    }

    // 그라데이션
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gra = new GradientPaint(0, 0, new Color(35, 116, 225), getWidth(), 0, new Color(35, 116, 225));
        g2.setPaint(gra);
        g2.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
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

    private ImageIcon recolorImage(String imagePath, Color targetColor) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(imagePath));
            BufferedImage coloredImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    int rgba = originalImage.getRGB(x, y);
                    Color col = new Color(rgba, true);

                    // 알파값(투명도) 유지, 색상만 변경
                    if (col.getAlpha() > 0) {
                        Color newColor = new Color(
                                targetColor.getRed(),
                                targetColor.getGreen(),
                                targetColor.getBlue(),
                                col.getAlpha());
                        coloredImage.setRGB(x, y, newColor.getRGB());
                    } else {
                        // 투명한 픽셀은 그대로 유지
                        coloredImage.setRGB(x, y, rgba);
                    }
                }
            }

            return new ImageIcon(coloredImage);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
