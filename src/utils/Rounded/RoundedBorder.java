package utils.Rounded;

import java.awt.Color;        
import java.awt.Component; 
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public class RoundedBorder implements javax.swing.border.Border {
    private int radius; // 테두리의 모서리를 둥글게 만들 반지름 값
    private Color color; // 테두리 색상

    public RoundedBorder(int radius, Color color) {
        this.radius = radius; // 반지름 값 할당
        this.color = color; // 색상 값 할당
    }

    public Insets getBorderInsets(Component c) { // 컴포넌트의 테두리 여백 설정
        return new Insets(4, this.radius + 1, 4, this.radius); // 위아래 여백은 4, 좌우는 반지름 기준 설정
    }

    public boolean isBorderOpaque() { // 테두리가 불투명한지 여부 반환
        return true; // 항상 불투명함
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g; // Graphics를 Graphics2D로 캐스팅 (더 정교한 제어 가능)
        g2.setColor(color); // 테두리 색상 설정
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 앤티앨리어싱 적용 (부드러운 모서리 처리)
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius); // 둥근 사각형 테두리 그림
    }
}