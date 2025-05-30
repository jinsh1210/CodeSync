package views.login_register;

import java.awt.*;
import java.awt.event.*;

import org.jdesktop.animation.timing.*;
import net.miginfocom.swing.MigLayout;

import java.text.DecimalFormat;

import javax.swing.UIManager;

public class LRMain extends javax.swing.JFrame {

    private MigLayout layout;
    private LRCover cover;
    private LRView loginAndRegister;
    private boolean isLogin;

    private final double coverSize = 40;
    private final double loginSize = 60;
    private final DecimalFormat df = new DecimalFormat("##0.###");

    public LRMain() {
        initComponents();
        init();
    }

    // 로그인 | 회원가입 | 커버 전체화면
    private void init() {
        layout = new MigLayout("fill, insets 0");

        LRView lrView = new LRView();
        cover = new LRCover(lrView);
        loginAndRegister = lrView;

        TimingTarget target = new TimingTargetAdapter() {

            @Override
            public void timingEvent(float fraction) {
                double fractionCover;
                double fractionLogin;
                if (isLogin) {
                    fractionCover = 1f - fraction;
                    fractionLogin = fraction;
                    if (fraction >= 0.5f) {
                        cover.registerRight(fractionCover * 100);
                    } else {
                        cover.loginRight(fractionLogin * 100);
                    }
                } else {
                    fractionCover = fraction;
                    fractionLogin = 1f - fraction;
                    if (fraction <= 0.5f) {
                        cover.registerLeft(fractionCover * 100);
                    } else {
                        cover.loginLeft(fractionLogin * 100);
                    }
                }
                if (fraction >= 0.5f) {
                    loginAndRegister.showRegister(isLogin);
                }
                fractionCover = Double.valueOf(df.format(fractionCover));
                fractionLogin = Double.valueOf(df.format(fractionLogin));
                // 컴포넌트 크기 지정
                layout.setComponentConstraints(cover, "width " + coverSize + "%, pos " + fractionCover + "al 0 n 100%");
                layout.setComponentConstraints(loginAndRegister,
                        "width " + loginSize + "%, pos " + fractionLogin + "al 0 n 100%");
                bg.revalidate();
            }

            @Override
            public void end() {
                isLogin = !isLogin;
            }
        };

        // 애니메이션 로직
        Animator animator = new Animator(800, target);
        animator.setAcceleration(0.5f);
        animator.setDeceleration(0.5f);
        animator.setResolution(0); // 부드러운 애니메이션
        bg.setLayout(layout);
        bg.add(cover, "width " + coverSize + "%, pos 0al 0 n 100%");
        bg.add(loginAndRegister, "width " + loginSize + "%, pos 1al 0 n 100%");
        cover.addEvent(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!animator.isRunning()) {
                    animator.start();
                }
            }
        });
    }

    private void initComponents() {
        // 그라데이션
        bg = new javax.swing.JLayeredPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int width = getWidth();
                int height = getHeight();
                GradientPaint gradient;
                if (isLogin) {
                    gradient = new GradientPaint(
                            0, 0, new Color(255, 255, 255),
                            width, 0, new Color(50, 126, 228));

                } else {
                    gradient = new GradientPaint(
                            0, 0, new Color(50, 126, 228),
                            width, 0, new Color(255, 255, 255));
                }
                g2.setPaint(gradient);
                g2.fillRect(0, 0, width, height);
                g2.dispose();
            }
        };

        setMinimumSize(new Dimension(800, 500));
        setResizable(false);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        bg.setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bg));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(bg));
    }

    public static void main(String args[]) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LRMain().setVisible(true);
            }
        });
    }

    private javax.swing.JLayeredPane bg;
}
