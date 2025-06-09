package utils.Rounded;

import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {
    private int radius;

    public RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;
        setContentAreaFilled(false); // 버튼의 배경 채우기 비활성화
        setFocusPainted(false); // 포커스 표시 비활성화
        setBorderPainted(false); // 기본 테두리 비활성화
        setOpaque(false); // 불투명 설정 비활성화 (투명하게)
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); // Graphics2D 객체 생성
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 안티앨리어싱 적용 (부드러운 모서리)
        g2.setColor(getBackground()); // 배경색 설정
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); // 둥근 사각형 배경 그림
        super.paintComponent(g); // JButton의 원래 그리기 동작 수행 (텍스트 등)
        g2.dispose(); // Graphics 객체 해제
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground().darker()); // 배경색보다 조금 더 어두운 색으로 테두리 설정
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
    }
}