package utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

public class IconConv {

    // png 파일 색 변환
    public ImageIcon recolorImage(String imagePath, Color targetColor) {
        try {
            // 이미지 파일을 읽어 BufferedImage 객체로 생성
            BufferedImage originalImage = ImageIO.read(new File(imagePath));

            // targetColor가 null이면 원본 이미지 그대로 반환
            if (targetColor == null) {
                return new ImageIcon(originalImage);
            }

            // 원본 이미지와 동일한 크기와 타입의 새로운 BufferedImage 생성 (ARGB 형식)
            BufferedImage coloredImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            // 이미지의 모든 픽셀을 순회하면서 색상 변경
            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    // 현재 픽셀의 RGBA 값을 가져옴
                    int argb = originalImage.getRGB(x, y);
                    Color col = new Color(argb, true);

                    // 투명도가 0보다 큰 픽셀만 targetColor로 변경 (알파값 유지)
                    if (col.getAlpha() > 0) {
                        Color newColor = new Color(
                                targetColor.getRed(),
                                targetColor.getGreen(),
                                targetColor.getBlue(),
                                col.getAlpha());
                        // 변경된 색상으로 픽셀 설정
                        coloredImage.setRGB(x, y, newColor.getRGB());
                    } else {
                        // 투명한 픽셀은 원본 색상 그대로 유지
                        coloredImage.setRGB(x, y, argb);
                    }
                }
            }

            // 색상이 변경된 이미지를 ImageIcon으로 반환
            return new ImageIcon(coloredImage);
        } catch (IOException e) {
            // 파일 읽기 실패 시 예외 출력 후 null 반환
            e.printStackTrace();
            return null;
        }
    }

    // 라벨 생성 로직
    public JLabel createImageLabel(String imagePath, Color color, int width, int height) {
        ImageIcon recoloredIcon = recolorImage(imagePath, color);
        if (recoloredIcon == null)
            return new JLabel(); // 예외 처리
        Image scaledImage = recoloredIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new JLabel(new ImageIcon(scaledImage));
    }

    // 버튼 생성 로직
    public JButton createImageButton(String imagePath, Color color, int width, int height, String buttonText,
            String Hint, boolean enableHover) {
        JLabel imageLabel = createImageLabel(imagePath, color, width, height);
        ImageIcon normalIcon = (ImageIcon) imageLabel.getIcon();

        int hoverWidth = width + 15;
        int hoverHeight = height + 15;
        ImageIcon recoloredIcon = recolorImage(imagePath, color);
        Image scaledHoverImage = recoloredIcon.getImage().getScaledInstance(hoverWidth, hoverHeight,
                Image.SCALE_SMOOTH);
        ImageIcon hoverIcon = new ImageIcon(scaledHoverImage);

        JButton button = new JButton(buttonText, normalIcon);
        button.setOpaque(false); // 버튼 배경 투명 설정 (백그라운드 그리기 비활성화)
        button.setFocusable(false); // 포커스 가능 상태 비활성화
        button.setContentAreaFilled(false); // 버튼 내용 영역 채우기 비활성화 (배경색 제거)
        button.setBorderPainted(false); // 테두리 그리기 비활성화

        // 버튼 크기 고정
        Dimension fixedSize = new Dimension(hoverWidth, hoverHeight);
        button.setPreferredSize(fixedSize);
        button.setMaximumSize(fixedSize);
        button.setMinimumSize(fixedSize);

        // 버튼 내용 정렬 및 여백 설정 (중앙 정렬, 여백 활용)
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setMargin(new Insets(0, 0, 0, 0));

        final Timer hoverTimer = new Timer(100, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                button.setToolTipText(Hint); // 원하는 힌트 내용
                ToolTipManager.sharedInstance().mouseMoved(
                        new java.awt.event.MouseEvent(button, 0, 0, 0,
                                0, 0,
                                0, false));
            }
        });
        hoverTimer.setRepeats(false);

        // 마우스 호버 효과
        if (enableHover) {
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    button.setIcon(hoverIcon);
                    hoverTimer.restart();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    button.setIcon(normalIcon);
                    hoverTimer.stop();
                    button.setToolTipText(null); // 툴팁 제거
                }
            });
        }
        return button;
    }
}
