package utils;

import java.awt.*;
import javax.swing.*;
import utils.Rounded.*;

//기본 스타일
public class Style {
	// 공통 색깔
	public static final Color PRIMARY_COLOR = new Color(35, 116, 225);
	public static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	public static final Color FIELD_BACKGROUND = Color.WHITE;
	public static final Color BASIC_TEXT_COLOR = Color.BLACK;
	public static final Color WARNING_COLOR = new Color(192, 57, 43);

	// 공통 폰트
	public static final Font TITLE_FONT = new Font("Malgun Gothic", Font.BOLD, 26);
	public static final Font LABEL_FONT = new Font("Malgun Gothic", Font.PLAIN, 16);
	public static final Font BUTTON_FONT = new Font("Malgun Gothic", Font.BOLD, 16);
	public static final Font MENU_FONT = new Font("Malgun Gothic", Font.PLAIN, 16);
	public static final Font DESC_FONT = new Font("Malgun Gothic", Font.PLAIN, 13);

	// 텍스트 색상
	public static final Color TEXT_PRIMARY_COLOR = new Color(52, 152, 219); // 파란색 제목
	public static final Color TEXT_SECONDARY_COLOR = Color.DARK_GRAY; // 보조 정보
	public static final Color TEXT_META_COLOR = Color.GRAY; // 부가 정보

	// 공통 텍스트 필드
	public static JTextField createStyledTextField() {
		JTextField field = new JTextField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(FIELD_BACKGROUND);
		field.setForeground(Color.BLACK);

		int borderRadius = 15; // 둥근 정도 조절
		Color borderColor = new Color(200, 200, 200);
		field.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(borderRadius, borderColor),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));

		return field;
	}

	// 공통 텍스트 공간
	public static JTextArea createStyledTextArea(int row, int column) {
		JTextArea area = new JTextArea(row, column); // 기본 행수와 열수
		area.setFont(LABEL_FONT);
		area.setBackground(FIELD_BACKGROUND);
		area.setForeground(Color.BLACK);
		area.setLineWrap(true); // 자동 줄바꿈
		area.setWrapStyleWord(true); // 단어 단위 줄바꿈

		int borderRadius = 15; // 둥근 정도 조절
		Color borderColor = new Color(200, 200, 200);

		area.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(borderRadius, borderColor),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));

		return area;
	}

	// 공통 패스워드 필드
	public static JPasswordField createStyledPasswordField() {
		JPasswordField field = new JPasswordField(20);
		field.setFont(LABEL_FONT);
		field.setBackground(FIELD_BACKGROUND);
		field.setForeground(Color.BLACK);

		int borderRadius = 15;
		Color borderColor = new Color(200, 200, 200);
		field.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(borderRadius, borderColor),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		return field;
	}

	// 공통 버튼
	public static JButton createStyledButton(String text, Color bgColor, Color fgColor) {
		RoundedButton button = new RoundedButton(text, 30);
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFont(BUTTON_FONT);
		button.setPreferredSize(new Dimension(130, 40));

		// 마우스 오버 효과
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				button.setBackground(bgColor.darker());
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				button.setBackground(bgColor);
			}
		});
		return button;
	}
}
