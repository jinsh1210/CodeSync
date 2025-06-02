package views.login_register;

import java.awt.*;
import java.awt.event.*;

import org.jdesktop.animation.timing.*;
import net.miginfocom.swing.MigLayout;
import utils.ClientSock;

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
        // MigLayout을 fill, insets(패딩값) 0 옵션으로 초기화
        layout = new MigLayout("fill, insets 0");

        // 로그인/회원가입 뷰 생성
        LRView lrView = new LRView();
        // 커버 패널 생성 (로그인/회원가입 뷰를 인자로 전달)
        cover = new LRCover(lrView);
        // 로그인/회원가입 패널 참조 저장
        loginAndRegister = lrView;

        // 애니메이션 타이밍 타겟 정의
        TimingTarget target = new TimingTargetAdapter() {

            @Override
            public void timingEvent(float fraction) {
                // 커버와 로그인 패널의 애니메이션 진행률 변수
                double fractionCover;
                double fractionLogin;
                if (isLogin) {
                    // 로그인 상태일 때 애니메이션 진행
                    fractionCover = 1f - fraction;
                    fractionLogin = fraction;
                    if (fraction >= 0.5f) {
                        // 애니메이션 절반 이후에는 registerRight 호출
                        cover.registerRight(fractionCover * 100);
                    } else {
                        // 절반 이전에는 loginRight 호출
                        cover.loginRight(fractionLogin * 100);
                    }
                } else {
                    // 회원가입 상태일 때 애니메이션 진행
                    fractionCover = fraction;
                    fractionLogin = 1f - fraction;
                    if (fraction <= 0.5f) {
                        // 절반 이전에는 registerLeft 호출
                        cover.registerLeft(fractionCover * 100);
                    } else {
                        // 절반 이후에는 loginLeft 호출
                        cover.loginLeft(fractionLogin * 100);
                    }
                }
                // 절반 이후에는 회원가입 화면 표시
                if (fraction >= 0.5f) {
                    loginAndRegister.showRegister(isLogin);
                }
                // 소수점 자리수 포맷 적용
                // IllegalArgumentException 예외 방지를 위해 Double.valueOf 사용
                fractionCover = Double.valueOf(df.format(fractionCover));
                fractionLogin = Double.valueOf(df.format(fractionLogin));
                // 커버와 로그인/회원가입 패널의 크기와 위치 지정
                layout.setComponentConstraints(cover, "width " + coverSize + "%, pos " + fractionCover + "al 0 n 100%");
                layout.setComponentConstraints(loginAndRegister,
                        "width " + loginSize + "%, pos " + fractionLogin + "al 0 n 100%");
                // 레이아웃 갱신
                bg.revalidate();
            }

            @Override
            public void end() {
                // 애니메이션 종료 시 로그인/회원가입 상태 전환
                isLogin = !isLogin;
            }
        };

        // 애니메이션 로직

        // 애니메이션 객체 생성 (800ms 동안 실행)
        Animator animator = new Animator(800, target);
        // 애니메이션 가속도 설정
        animator.setAcceleration(0.5f);
        // 애니메이션 감속도 설정
        animator.setDeceleration(0.5f);
        // 애니메이션 해상도 설정 (0: 최대)
        animator.setResolution(0);
        // 레이아웃을 MigLayout으로 설정
        bg.setLayout(layout);
        // 커버 컴포넌트 추가 및 위치/크기 지정
        bg.add(cover, "width " + coverSize + "%, pos 0al 0 n 100%");
        // 로그인/회원가입 컴포넌트 추가 및 위치/크기 지정
        bg.add(loginAndRegister, "width " + loginSize + "%, pos 1al 0 n 100%");
        // 커버에 이벤트 리스너 추가 (버튼 클릭 시 애니메이션 시작)
        cover.addEvent(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                // 애니메이션이 실행 중이 아닐 때만 시작
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
        // JFrame 설정
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

    // 메인 메서드
    public static void main(String args[]) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ClientSock.disconnect()));
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
