package views.MainView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;

import models.Repository;
import models.User;
import utils.ClientSock;
import utils.Style;
import views.MainView.MainFunc.RepositoryListCellRenderer;
import views.login_register.LRMain;

//MainView 클래스 - 로그인된 사용자의 저장소를 보여주고 관리하는 메인 UI
public class MainView extends JFrame {
	private User currentUser;
	private JList<Repository> repositoryList;
	private DefaultListModel<Repository> listModel;
	private JPopupMenu popupMenu;
	private JPanel detailPanel;

	private JSplitPane splitPane;
	private MainFunc mainFunc;
	private Timer timer=null;

	// 생성자 - 현재 사용자 정보를 저장하고 UI 초기화 및 저장소 목록 로딩
	public MainView(User user) {
		this.currentUser = user;
		listModel = new DefaultListModel<>();
		detailPanel = new JPanel();
		mainFunc = new MainFunc(listModel, detailPanel, currentUser);
		mainFunc.loadRepositories();
		initializeUI();
	}

	// 메인 화면 UI 구성 및 이벤트 바인딩
	private void initializeUI() {

		setSize(1200, 800);
		setMinimumSize(new Dimension(1200, 800));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JButton refreshIconButton = new JButton("");
		refreshIconButton.setMargin(new Insets(2, 4, 2, 4));
		refreshIconButton.setFocusable(false);
		ImageIcon refreshIcon = new ImageIcon("src/icons/refresh.png");
		Image scaledrefresh = refreshIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
		refreshIconButton.setIcon(new ImageIcon(scaledrefresh));
		refreshIconButton.setBackground(Color.WHITE);
		refreshIconButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
		refreshIconButton.setFocusPainted(false);
		refreshIconButton.setBorderPainted(false);
		refreshIconButton.setContentAreaFilled(false);
		refreshIconButton.setOpaque(false);

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);

		JLabel titleLabel = new JLabel("어서오세요, " + currentUser.getUsername() + "님");
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.TEXT_SECONDARY_COLOR);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(Style.BACKGROUND_COLOR);
		topPanel.add(titleLabel, BorderLayout.WEST);

		// 리스트 상단 패널
		JPanel topRepoPanel = new JPanel(new BorderLayout());
		topRepoPanel.setBackground(Style.BACKGROUND_COLOR);
		topRepoPanel.add(refreshIconButton, BorderLayout.EAST);

		// 메뉴바 구현
		JMenuBar menuBar = new JMenuBar();

		// 메뉴 구분선
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		separator.setPreferredSize(new Dimension(2, 20));
		separator.setMaximumSize(new Dimension(2, 20));
		separator.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		separator.setForeground(Color.GRAY);
		separator.setBackground(Color.GRAY);

		JMenu repoMenu = new JMenu("저장소");
		JMenuItem createRepoItem = new JMenuItem("저장소 만들기");
		JMenuItem searchReposItem = new JMenuItem("저장소 검색");
		repoMenu.add(createRepoItem);
		repoMenu.add(searchReposItem);

		JMenu accountMenu = new JMenu("계정");
		JMenuItem logoutItem = new JMenuItem("로그아웃");
		accountMenu.add(logoutItem);

		menuBar.add(repoMenu);
		menuBar.add(Box.createHorizontalStrut(5));
		menuBar.add(separator);
		menuBar.add(Box.createHorizontalStrut(5));
		menuBar.add(accountMenu);

		// 이 밑으로 메뉴바 오른쪽 정렬
		menuBar.add(Box.createHorizontalGlue());

		// 메뉴 기능
		createRepoItem.addActionListener(e -> mainFunc.showCreateRepositoryDialog());
		searchReposItem.addActionListener(e -> mainFunc.searchRepositories());
		logoutItem.addActionListener(e -> handleLogout());

		refreshIconButton.addActionListener(e -> {
			toggleSplitPaneDivider(splitPane, 800);
			detailPanel.removeAll();
			detailPanel.revalidate();
			detailPanel.repaint();
			mainFunc.loadRepositories(); // 리스트 새로 로드
			SwingUtilities.invokeLater(() -> {
				if (!listModel.isEmpty()) {
					repositoryList.setSelectedIndex(0); // 첫번째 선택
					repositoryList.clearSelection(); // 바로 선택 해제
				}
			});
		});

		setJMenuBar(menuBar);

		// 메뉴바 전체 크기 조정
		menuBar.setPreferredSize(new Dimension(0, 36));

		// 각 메뉴의 폰트와 마진 확대
		repoMenu.setFont(Style.MENU_FONT);
		accountMenu.setFont(Style.MENU_FONT);

		// 메뉴 아이템 폰트 확대
		createRepoItem.setFont(Style.MENU_FONT);
		searchReposItem.setFont(Style.MENU_FONT);
		logoutItem.setFont(Style.MENU_FONT);

		repositoryList = new JList<>(listModel);
		repositoryList.setCellRenderer(new RepositoryListCellRenderer());
		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryList.setFont(Style.LABEL_FONT.deriveFont(14f));

		repositoryList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = repositoryList.locationToIndex(e.getPoint());
				if (index != -1) {
					Rectangle bounds = repositoryList.getCellBounds(index, index);
					if (bounds.contains(e.getPoint())) {
						repositoryList.setSelectedIndex(index);

						if (SwingUtilities.isRightMouseButton(e)) {
							popupMenu.show(repositoryList, e.getX(), e.getY());
						} else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
							toggleSplitPaneDivider(splitPane, 200);
							timer=mainFunc.openRepositoryPanel(listModel.get(index));
						}
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(repositoryList);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.setBackground(Style.BACKGROUND_COLOR);
		listPanel.add(topRepoPanel, BorderLayout.NORTH);
		listPanel.add(scrollPane, BorderLayout.CENTER);

		mainPanel.add(topPanel, BorderLayout.NORTH);

		// 저장소 상세 정보 패널 생성
		detailPanel.setBackground(Style.BACKGROUND_COLOR);
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBorder(BorderFactory.createTitledBorder("저장소 정보"));
		detailPanel.setFont(Style.LABEL_FONT.deriveFont(14f));

		// 저장소 정보
		JLabel nameLabel = new JLabel();
		JLabel descLabel = new JLabel();
		JLabel visibilityLabel = new JLabel();
		JLabel username = new JLabel();
		JLabel sizeLabel = new JLabel();

		nameLabel.setFont(Style.LABEL_FONT.deriveFont(14f));
		descLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
		visibilityLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
		username.setFont(Style.LABEL_FONT.deriveFont(13f));
		sizeLabel.setFont(Style.LABEL_FONT.deriveFont(13f));

		nameLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
		descLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
		visibilityLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
		username.setForeground(Style.TEXT_SECONDARY_COLOR);
		sizeLabel.setForeground(Style.TEXT_SECONDARY_COLOR);

		detailPanel.add(nameLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(descLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(visibilityLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(username);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(sizeLabel);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel);
		splitPane.setDividerLocation(800);
		splitPane.setResizeWeight(0.7);
		splitPane.setBorder(null);
		// 크기 조절 비활성화
		splitPane.setEnabled(false);
		splitPane.setDividerSize(0);

		// 리스트 항목 선택 시 상세 패널 갱신
		repositoryList.addListSelectionListener(e -> {
			System.out.println("🧩 detailPanel 컴포넌트 수: " + detailPanel.getComponentCount());
			if (!e.getValueIsAdjusting()) { // 변경 이벤트가 끝났을 때만 처리
				if (detailPanel.getComponentCount() == 0) {
					detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
					detailPanel.setBorder(BorderFactory.createTitledBorder("저장소 정보"));
					detailPanel.setBackground(Style.BACKGROUND_COLOR);

					nameLabel.setFont(Style.LABEL_FONT.deriveFont(14f));
					descLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
					visibilityLabel.setFont(Style.LABEL_FONT.deriveFont(13f));
					username.setFont(Style.LABEL_FONT.deriveFont(13f));
					sizeLabel.setFont(Style.LABEL_FONT.deriveFont(13f));

					nameLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
					descLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
					visibilityLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
					username.setForeground(Style.TEXT_SECONDARY_COLOR);
					sizeLabel.setForeground(Style.TEXT_SECONDARY_COLOR);

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

				Repository selected = repositoryList.getSelectedValue();
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
				} else {
					nameLabel.setText("");
					descLabel.setText("");
					visibilityLabel.setText("");
					username.setText("");
					sizeLabel.setText("");
				}
			}
		});

		mainPanel.add(splitPane, BorderLayout.CENTER);

		add(mainPanel);

		popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("삭제");
		popupMenu.add(deleteItem);

		deleteItem.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				mainFunc.handleDeleteRepository(selected);
			}
		});
	}

	// 로그아웃 처리 - 로그인 화면으로 전환하고 현재 창 닫기
	private void handleLogout() {
		int confirm = JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			new LRMain().setVisible(true);
			this.dispose();
			if(timer!=null) timer.stop();
			ClientSock.disconnect();
			ClientSock.connect();
		}
	}

	private void toggleSplitPaneDivider(JSplitPane splitPane, int targetLocation) {
		int start = splitPane.getDividerLocation();
		int end = targetLocation;

		// TODO: 부드럽게 해야함
		Animator animator = new Animator(1000);
		animator.setAcceleration(0.5f);
		animator.setDeceleration(0.5f);
		animator.setResolution(0); // 부드러운 애니메이션
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
