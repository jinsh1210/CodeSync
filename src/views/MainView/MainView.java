package views.MainView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;

import lombok.Getter;
import lombok.Setter;
import models.Repository;
import models.User;
import utils.ClientSock;
import utils.IconConv;
import utils.Style;
import views.MainView.MainFunc.RepositoryListCellRenderer;
import views.login_register.LRMain;

@Getter
@Setter

// MainView 클래스 - 로그인된 사용자의 저장소를 보여주고 관리하는 메인 UI
public class MainView extends JFrame {
	private User currentUser;
	private JList<Repository> repositoryList;
	private DefaultListModel<Repository> listModel;
	private JPopupMenu popupMenu;
	private JPanel detailPanel;

	private JSplitPane splitPane;
	private MainFunc mainFunc;
	private Timer timer = null;
	private IconConv ic = new IconConv();
	private JTextField searchField;

	// 생성자 - 현재 사용자 정보를 저장하고 UI 초기화 및 저장소 목록 로딩
	public MainView(User user) {
		this.currentUser = user;
		listModel = new DefaultListModel<>();
		detailPanel = new JPanel();
		mainFunc = new MainFunc(listModel, detailPanel, currentUser, this);
		mainFunc.loadRepositories();
		initializeUI();
	}

	// 메인 화면 UI 구성 및 이벤트 바인딩
	private void initializeUI() {

		setSize(1200, 800);
		setMinimumSize(new Dimension(1200, 800));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		// 새로고침 버튼
		JButton refreshIconButton = ic.createImageButton("src/icons/refresh.png", null, 18, 18, null, "새로고침");
		refreshIconButton.setMargin(new Insets(2, 4, 2, 4));
		refreshIconButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
		refreshIconButton.addActionListener(e -> {
			toggleSplitPaneDivider(splitPane, 800);
			detailPanel.removeAll();
			detailPanel.revalidate();
			detailPanel.repaint();
			mainFunc.loadRepositories();
			SwingUtilities.invokeLater(() -> {
				if (!listModel.isEmpty()) {
					repositoryList.setSelectedIndex(0);
					repositoryList.clearSelection();
				}
			});
		});

		// 메인 패널
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);

		// 상단 패널 구성 요소 ...
		// 제목
		JLabel titleLabel = new JLabel("어서오세요, " + currentUser.getUsername() + "님");

		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);

		// 검색 버튼
		JButton searchButton = ic.createImageButton("src/icons/search.png", Style.PRIMARY_COLOR, 20, 20, null, "검색");

		// 검색 필드
		searchField = new JTextField(20);
		searchField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Style.PRIMARY_COLOR));
		searchField.setBackground(Style.BACKGROUND_COLOR);
		searchField.setOpaque(true);
		searchField.setFont(Style.LABEL_FONT.deriveFont(14f));
		searchField.setForeground(Style.BASIC_TEXT_COLOR);
		// 검색 기능
		searchButton.addActionListener(e -> {
			toggleSplitPaneDivider(splitPane, 800);
			detailPanel.removeAll();
			detailPanel.revalidate();
			detailPanel.repaint();
			mainFunc.loadRepositories();
			SwingUtilities.invokeLater(() -> {
				if (!listModel.isEmpty()) {
					repositoryList.setSelectedIndex(0);
					repositoryList.clearSelection();
				}
			});
			mainFunc.searchRepositories();
			searchField.setText("");
		});
		searchField.addActionListener(e -> {
			toggleSplitPaneDivider(splitPane, 800);
			detailPanel.removeAll();
			detailPanel.revalidate();
			detailPanel.repaint();
			mainFunc.loadRepositories();
			SwingUtilities.invokeLater(() -> {
				if (!listModel.isEmpty()) {
					repositoryList.setSelectedIndex(0);
					repositoryList.clearSelection();
				}
			});
			mainFunc.searchRepositories();
			searchField.setText("");
		});

		// 메인 상단 우측 패널
		JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		topRightPanel.add(searchField);
		topRightPanel.add(searchButton);
		topRightPanel.setBackground(Style.BACKGROUND_COLOR);

		// 메인 상단 패널
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(Style.BACKGROUND_COLOR);
		topPanel.add(titleLabel, BorderLayout.WEST);
		topPanel.add(topRightPanel, BorderLayout.EAST);
		// 메인 패널에 추가
		mainPanel.add(topPanel, BorderLayout.NORTH);

		// 메뉴 관련 ...
		// 메뉴바
		JMenuBar menuBar = new JMenuBar();

		// 메뉴 버튼
		JButton btnAddRepo = ic.createImageButton("src/icons/addfile.png", Style.PRIMARY_COLOR, 30, 30, null, "저장소 생성");
		JButton btnLogout = ic.createImageButton("src/icons/logout.png", Style.PRIMARY_COLOR, 30, 30, null, "로그아웃");

		// 메뉴 기능
		btnAddRepo.addActionListener(e -> mainFunc.showCreateRepositoryDialog());
		btnLogout.addActionListener(e -> handleLogout());

		// 메뉴 정렬
		menuBar.add(btnAddRepo);
		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(btnLogout);

		// 이 밑으로 메뉴바 오른쪽 정렬
		menuBar.add(Box.createHorizontalGlue());

		setJMenuBar(menuBar);

		// 메뉴바 전체 크기 조정
		menuBar.setPreferredSize(new Dimension(0, 36));

		// 저장소 패널 ...
		// 저장소 리스트 상단 패널
		JPanel topRepoPanel = new JPanel(new BorderLayout());
		topRepoPanel.setBackground(Style.BACKGROUND_COLOR);
		topRepoPanel.add(refreshIconButton, BorderLayout.EAST);

		// 저장소 리스트 표현
		repositoryList = new JList<>(listModel);
		repositoryList.setCellRenderer(new RepositoryListCellRenderer());
		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryList.setFont(Style.LABEL_FONT.deriveFont(14f));

		// 팝메뉴 ...
		popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("레포지토리 삭제");
		JMenuItem changeVisible = new JMenuItem("공개여부 변경");
		JMenuItem rmCollabo = new JMenuItem("콜라보 탈퇴");

		// 마우스 우클릭 -> 팝 메뉴(저장소 삭제, 공개여부 변경)
		repositoryList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = repositoryList.locationToIndex(e.getPoint());
				// 우클릭 처리
				if (index != -1) {
					Rectangle bounds = repositoryList.getCellBounds(index, index);
					if (bounds.contains(e.getPoint())) {
						repositoryList.setSelectedIndex(index);
						if (SwingUtilities.isRightMouseButton(e)) {
							Repository selected = repositoryList.getSelectedValue();
							popupMenu.removeAll();
							// 본인 저장소일 경우
							if (selected != null && selected.getUsername().equals(currentUser.getUsername())) {
								popupMenu.add(deleteItem);
								popupMenu.add(changeVisible);
								popupMenu.show(repositoryList, e.getX(), e.getY());
								// 본인 저장소가 아닐 경우
							} else if (selected != null && !selected.getUsername().equals(currentUser.getUsername())) {
								// TODO: 콜라보에 속해 있을 때만 뜨게 변경 필요
								popupMenu.add(rmCollabo);
								popupMenu.show(repositoryList, e.getX(), e.getY());
							}
							// 더블클릭 처리 -> 애니메이션 요청 및 저장소 불러오기
						} else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
							toggleSplitPaneDivider(splitPane, 300);
							timer = mainFunc.openRepositoryPanel(listModel.get(index));
						}
					}
				}
			}
		});
		// 공개 여부 변경 요청
		changeVisible.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				mainFunc.handleChangeVisible(currentUser.getUsername(), selected.getName(),
						(selected.getVisibility().equals("public") ? "private" : "public"));
			}
		});
		// 저장소 삭제 요청
		deleteItem.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				mainFunc.handleDeleteRepository(selected);
			}
		});
		// 콜라보 해제 요청
		rmCollabo.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null)
				mainFunc.handleRmCollabo(selected.getName(), currentUser.getUsername(), selected.getUsername());
		});

		// 스크롤팬
		JScrollPane scrollPane = new JScrollPane(repositoryList);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		// 상단 및 스크롤팬 저장소 패널에 추가
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.setBackground(Style.BACKGROUND_COLOR);
		listPanel.add(topRepoPanel, BorderLayout.NORTH);
		listPanel.add(scrollPane, BorderLayout.CENTER);

		// 저장소 상세 패널 ...
		detailPanel.setBackground(Style.BACKGROUND_COLOR);
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				"저장소 정보",
				0, 0,
				Style.TITLE_FONT.deriveFont(18f),
				Style.BASIC_TEXT_COLOR));
		detailPanel.setFont(Style.LABEL_FONT);

		// 저장소 정보 ...
		JLabel nameLabel = new JLabel();
		JLabel descLabel = new JLabel();
		JLabel visibilityLabel = new JLabel();
		JLabel username = new JLabel();
		JLabel sizeLabel = new JLabel();
		// 폰트
		nameLabel.setFont(Style.LABEL_FONT.deriveFont(14f));
		descLabel.setFont(Style.DESC_FONT);
		visibilityLabel.setFont(Style.DESC_FONT);
		username.setFont(Style.DESC_FONT);
		sizeLabel.setFont(Style.DESC_FONT);
		// 색깔
		nameLabel.setForeground(Style.BASIC_TEXT_COLOR);
		descLabel.setForeground(Style.BASIC_TEXT_COLOR);
		visibilityLabel.setForeground(Style.BASIC_TEXT_COLOR);
		username.setForeground(Style.BASIC_TEXT_COLOR);
		sizeLabel.setForeground(Style.BASIC_TEXT_COLOR);
		// detailPanel에 라벨 5픽셀 간격으로 추가
		detailPanel.add(nameLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(descLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(visibilityLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(username);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(sizeLabel);

		// 나누기 팬 ...
		// 저장소 패널과 나누는 팬
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel);
		// x좌표 800을 포인트로 나누기
		splitPane.setDividerLocation(800);
		splitPane.setResizeWeight(0.7);
		splitPane.setBorder(null);
		// 크기 조절 비활성화
		splitPane.setEnabled(false);
		splitPane.setDividerSize(0);

		// 리스트 항목 선택 시 상세 패널 갱신
		repositoryList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) { // 변경 이벤트가 끝났을 때만 처리
				if (detailPanel.getComponentCount() == 0) {
					detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
					detailPanel.setBorder(BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY),
							"저장소 정보",
							0, 0,
							Style.TITLE_FONT.deriveFont(18f),
							Style.BASIC_TEXT_COLOR));
					detailPanel.setBackground(Style.BACKGROUND_COLOR);
					// 폰트
					nameLabel.setFont(Style.LABEL_FONT.deriveFont(14f));
					descLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
					visibilityLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
					username.setFont(Style.LABEL_FONT.deriveFont(13f));
					sizeLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
					// 색깔
					nameLabel.setForeground(Style.BASIC_TEXT_COLOR);
					descLabel.setForeground(Style.BASIC_TEXT_COLOR);
					visibilityLabel.setForeground(Style.BASIC_TEXT_COLOR);
					username.setForeground(Style.BASIC_TEXT_COLOR);
					sizeLabel.setForeground(Style.BASIC_TEXT_COLOR);
					// 5픽셀 간격으로 추가
					detailPanel.add(nameLabel);
					detailPanel.add(Box.createVerticalStrut(5));
					detailPanel.add(descLabel);
					detailPanel.add(Box.createVerticalStrut(5));
					detailPanel.add(visibilityLabel);
					detailPanel.add(Box.createVerticalStrut(5));
					detailPanel.add(username);
					detailPanel.add(Box.createVerticalStrut(5));
					detailPanel.add(sizeLabel);
					detailPanel.revalidate();
					detailPanel.repaint();
				}
				// 설명이 너무 길면 ...으로 대체
				Repository selected = repositoryList.getSelectedValue();
				// 선택된 저장소가 있는 경우
				if (selected != null) {
					String description = selected.getDescription();
					if (description.length() > 5) {
						description = description.substring(0, 5) + "...";
					}
					nameLabel.setText("이름: " + selected.getName());
					descLabel.setText("설명: " + description);
					visibilityLabel.setText("공개 여부: " + selected.getVisibility());
					username.setText("소유자: " + selected.getUsername());
					sizeLabel.setText("저장소 용량: " + selected.getSize() + "MB");
					// 선택된 저장소 없을 경우
				} else {
					nameLabel.setText("");
					descLabel.setText("");
					visibilityLabel.setText("");
					username.setText("");
					sizeLabel.setText("");
				}
			}
		});
		// 메인 패널 구현
		mainPanel.add(splitPane, BorderLayout.CENTER);
		add(mainPanel);

	}

	// 로그아웃 처리 - 로그인 화면으로 전환하고 현재 창 닫기
	private void handleLogout() {
		int confirm = JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			new LRMain().setVisible(true);
			this.dispose();
			if (timer != null)
				timer.stop();
			ClientSock.disconnect();
			ClientSock.connect();
		}
	}

	// 애니메이션 로직
	private Animator animator = null;

	private void toggleSplitPaneDivider(JSplitPane splitPane, int targetLocation) {

		if (animator != null && animator.isRunning()) {
			animator.stop(); // 이전 애니메이션 중단
		}
		int start = splitPane.getDividerLocation();
		int end = targetLocation;

		animator = new Animator(800);
		animator.setAcceleration(0.5f);
		animator.setDeceleration(0.5f);
		animator.setResolution(10); // 부드러운 애니메이션
		animator.addTarget(new TimingTargetAdapter() {
			@Override
			public void timingEvent(float fraction) {
				int newLocation = (int) (start + (end - start) * fraction);
				splitPane.setDividerLocation(newLocation);
			}
		});
		animator.start();
	}
}
