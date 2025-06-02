package views.MainView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import utils.Style;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class MainEditRepo {
    private MainFunc mainFunc;
    private MainView mainView;

    // 생성자
    public MainEditRepo(MainFunc mainFunc, MainView mainView) {
        this.mainFunc = mainFunc;
        this.mainView = mainView;
    }
    // 패널 생성 메소드
    public JPanel createPanel() {
        // 저장소 이름 입력 필드 생성
        JTextField nameField = Style.createStyledTextField();
        // 저장소 설명 입력 필드(여러 줄) 생성
        JTextArea descField = Style.createStyledTextArea(3, 20);
        // 공개/비공개 선택 콤보박스 생성
        JComboBox<String> visibilityComboBox = new JComboBox<>(new String[] { "private", "public" });

        // 타이틀 라벨 생성 및 스타일 지정
        JLabel titleLabel = new JLabel("저장소 생성");
        titleLabel.setForeground(Style.PRIMARY_COLOR);
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // MigLayout을 사용한 패널 생성 및 배경/테두리/크기 지정
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[right][grow,fill]", "[]10[]10[]10[]"));
        panel.setBackground(Style.FIELD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Style.BASIC_TEXT_COLOR, 1), // 바깥 테두리
                BorderFactory.createEmptyBorder(10, 10, 10, 10) // 안쪽 여백
        ));
        panel.setPreferredSize(new Dimension(350, 335));

        // 설명 입력 필드를 스크롤 가능하게 감싸기
        JScrollPane scrollPane = new JScrollPane(descField);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 타이틀 라벨 추가 (2칸 병합, 가운데 정렬, 아래 여백)
        panel.add(titleLabel, "span 2, center, gapbottom 20");
        // 이름 라벨 및 입력 필드 추가
        panel.add(new JLabel("이름:"));
        panel.add(nameField, "growx, wmin 150");
        // 설명 라벨 및 입력 필드(스크롤) 추가
        panel.add(new JLabel("설명:"));
        panel.add(scrollPane, "growx, h 80!");
        // 권한 라벨 및 콤보박스 추가
        panel.add(new JLabel("권한:"));
        panel.add(visibilityComboBox, "growx, wmin 150");

        // 취소 버튼 생성 및 스타일 지정
        JButton cancelButton = Style.createStyledButton("취소", Style.PRIMARY_COLOR, Style.FIELD_BACKGROUND);
        cancelButton.setPreferredSize(new Dimension(100, 30));
        // 취소 버튼 클릭 시 동작 정의
        cancelButton.addActionListener(e -> {
            mainFunc.loadRepositories(); // 저장소 목록 새로고침
            nameField.setText(""); // 입력 필드 초기화
            descField.setText(""); // 입력 필드 초기화
            mainView.toggleEditRepoPanel(); // 패널 닫기
        });
        // 저장 버튼 생성 및 스타일 지정
        JButton saveButton = Style.createStyledButton("저장", Style.WARNING_COLOR, Style.FIELD_BACKGROUND);
        saveButton.setPreferredSize(new Dimension(100, 30));
        // 저장 버튼 클릭 시 동작 정의
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim(); // 이름 값 가져오기
            String description = descField.getText().trim(); // 설명 값 가져오기
            String visibility = (String) visibilityComboBox.getSelectedItem(); // 권한 값 가져오기

            mainFunc.handleAddRepo(name, description, visibility); // 저장소 추가 처리
            mainView.toggleEditRepoPanel(); // 패널 닫기
        });

        // 버튼들을 담을 패널 생성 및 스타일 지정
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        // 버튼 패널 추가 (2칸 병합, 가운데 정렬, 위아래 여백)
        panel.add(buttonPanel, "span 2, center, gapy 10");

        // 완성된 패널 반환
        return panel;
    }
}
