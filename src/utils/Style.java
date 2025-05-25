package utils;

import javax.swing.*;
import java.awt.*;

//기본 스타일
public class Style {
	// 공통 색상/폰트
	public static final Color PRIMARY_COLOR = new Color(52, 152, 219);
	public static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	public static final Color FIELD_BACKGROUND = Color.WHITE;

	public static boolean isDarkMode = false;

	public static final Color DARK_PRIMARY_COLOR = new Color(44, 62, 80);
	public static final Color DARK_BACKGROUND_COLOR = new Color(34, 34, 34);
	public static final Color DARK_FIELD_BACKGROUND = new Color(64, 64, 64);
	public static final Color DARK_TEXT_COLOR = new Color(230, 230, 230);
	public static final Color DARK_BUTTON_COLOR = new Color(70, 70, 70);

	public static final Font TITLE_FONT = new Font("Malgun Gothic", Font.BOLD, 26);
	public static final Font LABEL_FONT = new Font("Malgun Gothic", Font.PLAIN, 16);
	public static final Font BUTTON_FONT = new Font("Malgun Gothic", Font.BOLD, 16);
	public static final Font menuFont = new Font("Malgun Gothic", Font.PLAIN, 16);

	// 밝은 테마 전용 텍스트 색상
	public static final Color TEXT_PRIMARY_COLOR = new Color(52, 152, 219); // 파란색 제목
	public static final Color TEXT_SECONDARY_COLOR = Color.DARK_GRAY; // 보조 정보
	public static final Color TEXT_META_COLOR = Color.GRAY; // 부가 정보

	// 공통 텍스트 필드
	public static JTextField createStyledTextField() {
		JTextField field = new JTextField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(isDarkMode ? DARK_FIELD_BACKGROUND : FIELD_BACKGROUND);
		field.setForeground(isDarkMode ? DARK_TEXT_COLOR : Color.BLACK);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return field;
	}

	// 공통 패스워드 필드
	public static JPasswordField createStyledPasswordField() {
		JPasswordField field = new JPasswordField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(isDarkMode ? DARK_FIELD_BACKGROUND : FIELD_BACKGROUND);
		field.setForeground(isDarkMode ? DARK_TEXT_COLOR : Color.BLACK);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return field;
	}

	// 공통 버튼
	public static JButton createStyledButton(String text, Color bgColor, Color fgColor) {
		JButton button = new JButton(text);
		button.setFont(BUTTON_FONT);
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(true);
		button.setPreferredSize(new Dimension(130, 40));

		// Hover 색상 (배경색을 약간 어둡게)
		Color hoverColor = bgColor.darker();

		// MouseListener 추가
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				button.setBackground(hoverColor);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				button.setBackground(bgColor);
			}
		});

		return button;
	}

	public static void toggleDarkMode() {
		isDarkMode = !isDarkMode;
	}
}
