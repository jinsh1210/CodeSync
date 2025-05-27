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
            BufferedImage originalImage = ImageIO.read(new File(imagePath));

            // targetColor가 null이면 원본 이미지 그대로 반환
            if (targetColor == null) {
                return new ImageIcon(originalImage);
            }

            BufferedImage coloredImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    int rgba = originalImage.getRGB(x, y);
                    Color col = new Color(rgba, true);

                    if (col.getAlpha() > 0) {
                        Color newColor = new Color(
                                targetColor.getRed(),
                                targetColor.getGreen(),
                                targetColor.getBlue(),
                                col.getAlpha());
                        coloredImage.setRGB(x, y, newColor.getRGB());
                    } else {
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
            String Hint) {
        JLabel imageLabel = createImageLabel(imagePath, color, width, height);
        ImageIcon normalIcon = (ImageIcon) imageLabel.getIcon();

        int hoverWidth = width + 15;
        int hoverHeight = height + 15;
        ImageIcon recoloredIcon = recolorImage(imagePath, color);
        Image scaledHoverImage = recoloredIcon.getImage().getScaledInstance(hoverWidth, hoverHeight,
                Image.SCALE_SMOOTH);
        ImageIcon hoverIcon = new ImageIcon(scaledHoverImage);

        JButton button = new JButton(buttonText, normalIcon);
        button.setOpaque(false);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);

        // 버튼 크기 고정
        Dimension fixedSize = new Dimension(hoverWidth, hoverHeight);
        button.setPreferredSize(fixedSize);
        button.setMaximumSize(fixedSize);
        button.setMinimumSize(fixedSize);

        // 버튼 내용 정렬 및 여백 설정 (중앙 정렬, 여백 활용)
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setMargin(new Insets(0, 0, 0, 0));

        final Timer hoverTimer = new Timer(400, new java.awt.event.ActionListener() {
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

        return button;
    }
}
