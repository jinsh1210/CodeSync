package utils;

import javax.swing.*;
import java.awt.*;

//기본 스타일
public class Style {
	// 공통 색상/폰트
	public static final Color PRIMARY_COLOR = new Color(52, 152, 219);
	public static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	public static final Color FIELD_BACKGROUND = Color.WHITE;

	public static final Font TITLE_FONT = new Font("Malgun Gothic", Font.BOLD, 26);
	public static final Font LABEL_FONT = new Font("Malgun Gothic", Font.PLAIN, 16);
	public static final Font BUTTON_FONT = new Font("Malgun Gothic", Font.BOLD, 16);

	// 공통 텍스트 필드
	public static JTextField createStyledTextField() {
		JTextField field = new JTextField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(FIELD_BACKGROUND);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return field;
	}

	// 공통 패스워드 필드
	public static JPasswordField createStyledPasswordField() {
		JPasswordField field = new JPasswordField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(FIELD_BACKGROUND);
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
		return button;
	}
}
