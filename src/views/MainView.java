package views;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import models.User;
import utils.Style;
import models.Repository;
import utils.ClientSock;
import org.json.JSONArray;
import org.json.JSONObject;

//MainView 클래스 - 로그인된 사용자의 저장소를 보여주고 관리하는 메인 UI
public class MainView extends JFrame {
	private User currentUser;
	private JList<Repository> repositoryList;
	private DefaultListModel<Repository> listModel;
	private JPopupMenu popupMenu;

	// 생성자 - 현재 사용자 정보를 저장하고 UI 초기화 및 저장소 목록 로딩
	public MainView(User user) {
		this.currentUser = user;
		initializeUI();
		loadRepositories();
	}

	// 메인 화면 UI 구성 및 이벤트 바인딩
	private void initializeUI() {
		setTitle("J.S.Repo - Main");
		setSize(650, 500);
		setMinimumSize(new Dimension(600, 400));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JButton refreshIconButton = new JButton("🔄");
		refreshIconButton.setMargin(new Insets(2, 4, 2, 4));
		refreshIconButton.setFocusable(false);
		refreshIconButton.setFont(Style.BUTTON_FONT.deriveFont(14f));

		refreshIconButton.setBackground(Color.WHITE); // 다크모드는 applyDarkMode에서 반영
		refreshIconButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);

		JLabel titleLabel = new JLabel("어서오세요, " + currentUser.getUsername() + "님");
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.TEXT_PRIMARY_COLOR);

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
		separator.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // 좌우 여백
		separator.setForeground(Color.GRAY); // 색상은 어둡게
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

		// 다크 모드 토글 버튼 생성
		JToggleButton darkModeToggle = new JToggleButton("🌙");
		darkModeToggle.setFont(Style.BUTTON_FONT);
		darkModeToggle.setFocusable(false);
		darkModeToggle.setSelected(Style.isDarkMode); // 현재 설정 상태 반영
		darkModeToggle.setBackground(new Color(230, 230, 230));
		darkModeToggle.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

		createRepoItem.addActionListener(e -> showCreateRepositoryDialog());
		refreshIconButton.addActionListener(e -> loadRepositories());
		searchReposItem.addActionListener(e -> searchRepositories());
		logoutItem.addActionListener(e -> handleLogout());
		darkModeToggle.addItemListener(e -> {
			Style.toggleDarkMode();
			applyDarkMode();
		});

		menuBar.add(darkModeToggle);
		setJMenuBar(menuBar);

		// 메뉴바 전체 크기 조정
		menuBar.setPreferredSize(new Dimension(0, 36)); // 기존보다 약간 높은 높이

		// 각 메뉴의 폰트와 마진 확대
		repoMenu.setFont(Style.menuFont);
		accountMenu.setFont(Style.menuFont);

		// 메뉴 아이템 폰트 확대
		createRepoItem.setFont(Style.menuFont);
		searchReposItem.setFont(Style.menuFont);
		logoutItem.setFont(Style.menuFont);

		// 다크모드 토글 버튼 크기 키우기
		darkModeToggle.setFont(Style.BUTTON_FONT);
		darkModeToggle.setPreferredSize(new Dimension(50, 36));

		listModel = new DefaultListModel<>();
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
							openRepository(listModel.get(index));
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
		JPanel detailPanel = new JPanel();
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

		nameLabel.setForeground(Style.TEXT_PRIMARY_COLOR);
		descLabel.setForeground(Style.TEXT_SECONDARY_COLOR);
		visibilityLabel.setForeground(Style.TEXT_META_COLOR);
		username.setForeground(Style.TEXT_META_COLOR);
		sizeLabel.setForeground(Style.TEXT_META_COLOR);

		detailPanel.add(nameLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(descLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(visibilityLabel);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(username);
		detailPanel.add(Box.createVerticalStrut(5));
		detailPanel.add(sizeLabel);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel);
		splitPane.setDividerLocation(450);
		splitPane.setResizeWeight(0.7);
		splitPane.setBorder(null);
		// 크기 조절 비활성화
		splitPane.setEnabled(false);
		splitPane.setDividerSize(0);

		// 리스트 항목 선택 시 상세 패널 갱신
		repositoryList.addListSelectionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				// 저장소 설명 너무 길면 ...으로 대체
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
		});

		mainPanel.add(splitPane, BorderLayout.CENTER);

		add(mainPanel);

		// (이전 버튼 이벤트 연결 제거됨, 메뉴 아이템에 연결됨)

		popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("삭제");
		popupMenu.add(deleteItem);

		deleteItem.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				handleDeleteRepository(selected);
			}
		});
	}

	// 서버로부터 개인 저장소 목록을 불러와 리스트에 표시
	private void loadRepositories() {
		listModel.clear();
		try {
			ClientSock.sendCommand("/repo_list");

			String line;
			StringBuilder jsonBuilder = new StringBuilder();
			boolean inRepoList = false;

			while ((line = ClientSock.receiveResponse()) != null) {
				if (line.equals("/#/repo_SOL")) {
					inRepoList = true;
					continue;
				} else if (line.equals("/#/repo_EOL")) {
					break;
				}

				if (inRepoList) {
					jsonBuilder.append(line);
				}
			}

			JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
			Set<Integer> addedIds = new HashSet<>();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject obj = jsonArray.getJSONObject(i);
				String name = obj.getString("name");
				String description = obj.getString("description");
				String visibility = obj.getString("visibility");
				String username = obj.getString("user");
				double filesize = obj.getDouble("size");

				int id = i;
				if (!addedIds.contains(id)) {
					Repository repo = new Repository(id, name, description, visibility, username, filesize);
					listModel.addElement(repo);
					addedIds.add(id);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "저장소 로딩 실패");
		}
	}

	// 저장소 생성 다이얼로그를 띄우고 서버에 생성 요청
	private void showCreateRepositoryDialog() {
		JTextField nameField = new JTextField();
		JTextArea descField = new JTextArea(3, 20);
		descField.setLineWrap(true);
		descField.setWrapStyleWord(true);

		JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
		panel.setBackground(Style.BACKGROUND_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(new JLabel("저장소 이름:"));
		panel.add(nameField);
		panel.add(new JLabel("설명:"));
		panel.add(new JScrollPane(descField));

		String[] options = { "private", "public" };
		String selected = (String) JOptionPane.showInputDialog(this, "접근 권한:", "설정", JOptionPane.PLAIN_MESSAGE, null,
				options, "private");

		int result = JOptionPane.showConfirmDialog(this, panel, "저장소 생성", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION && selected != null) {
			String rawName = nameField.getText().trim();
			String name = nameField.getText().trim().replaceAll("\\s+", "_");
			String description = descField.getText().trim();

			if (!name.equals(rawName)) {
				JOptionPane.showMessageDialog(this, "저장소 이름에 포함된 공백은 밑줄(_)로 자동 변경됩니다.\n변경된 이름: " + name, "이름 자동 수정",
						JOptionPane.INFORMATION_MESSAGE);
			}

			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(this, "저장소 이름을 입력해주세요.");
				return;
			}

			try {
				String safeDescription = description.replace("\n", "\\n");
				ClientSock.sendCommand("/repo_create " + name + " \"" + safeDescription + "\" " + selected);
				String response = ClientSock.receiveResponse();

				if (response != null && response.contains("/#/repo_create 저장소 생성 성공")) {
					JOptionPane.showMessageDialog(this, "저장소 생성 성공");
					loadRepositories();
				} else if (response != null && response.startsWith("/#/error")) {
					String msg = response.replace("/#/error", "").trim();
					showErrorDialog("저장소 생성 실패: " + msg);
				} else {
					showErrorDialog("알 수 없는 서버 응답: " + response);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "서버 연결 실패");
			}
		}
	}

	// 저장소를 더블클릭했을 때 RepositoryView 창 열기
	private void openRepository(Repository repository) {
		try {
			RepositoryView repoView = new RepositoryView(repository, currentUser);
			repoView.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "저장소 열기 실패: " + e.getMessage());
		}
	}

	// 로그아웃 처리 - 로그인 화면으로 전환하고 현재 창 닫기
	private void handleLogout() {
		int confirm = JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			new LoginView().setVisible(true);
			this.dispose();
		}
	}

	// 저장소 리스트 셀 렌더링 (형식 지정)
	private class RepositoryListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Repository) {
				Repository repo = (Repository) value;

				setIcon(getVisibilityIcon(repo.getVisibility()));
				String description = repo.getDescription();
				if (description.length() > 5) {
					description = description.substring(0, 5) + "...";
				}
				setText("저장소: " + repo.getName() + " | " + description);

				// 다크 모드일 경우 색상 적용
				if (Style.isDarkMode) {
					setBackground(isSelected ? new Color(60, 60, 60) : Style.DARK_BACKGROUND_COLOR);
					setForeground(Style.DARK_TEXT_COLOR);
				}
			}
			return this;
		}
	}

	// 저장소 삭제 요청 및 응답 처리
	private void handleDeleteRepository(Repository selected) {
		int confirm = JOptionPane.showConfirmDialog(this, "정말로 '" + selected.getName() + "' 저장소를 삭제하시겠습니까?",
				"저장소 삭제 확인", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			try {
				ClientSock.sendCommand("/repo_delete " + selected.getName());
				String response = ClientSock.receiveResponse();
				if (response.startsWith("/#/repo_delete_success")) {
					JOptionPane.showMessageDialog(this, "저장소가 삭제되었습니다.");
					loadRepositories();
				} else if (response != null && response.startsWith("/#/repo_delete_fail")) {
					showErrorDialog("삭제 실패: 저장소 삭제 권한이 없습니다.");
				} else {
					showErrorDialog(response);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "서버 연결 실패");
			}
		}
	}

	// 에러 메시지 일괄 처리 팝업
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}

	// 다크 모드 적용 메서드
	private void applyDarkMode() {
		Color bgColor = Style.isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR;
		Color fgColor = Style.isDarkMode ? Style.DARK_TEXT_COLOR : Style.TEXT_SECONDARY_COLOR;

		getContentPane().setBackground(bgColor);

		for (Component c : getContentPane().getComponents()) {
			applyComponentDarkMode(c, bgColor, fgColor);
		}
	}

	// 다크모드 적용
	private void applyComponentDarkMode(Component comp, Color bg, Color fg) {
		comp.setBackground(bg);
		comp.setForeground(fg);

		if (comp instanceof JPanel && ((JPanel) comp).getBorder() instanceof TitledBorder) {
			TitledBorder border = (TitledBorder) ((JPanel) comp).getBorder();
			border.setTitleColor(fg); // 제목 텍스트 색상 강제 적용
		}

		if (comp instanceof JPanel) {
			// ⭐ JPanel은 항상 수동 배경 설정 필요
			comp.setBackground(bg);
			for (Component child : ((JPanel) comp).getComponents()) {
				applyComponentDarkMode(child, bg, fg);
			}
		} else if (comp instanceof JScrollPane) {
			JScrollPane scroll = (JScrollPane) comp;
			scroll.setBackground(bg);
			scroll.getViewport().setBackground(bg);
			Component view = scroll.getViewport().getView();
			if (view != null) {
				applyComponentDarkMode(view, bg, fg);
			}
		} else if (comp instanceof JSplitPane) {
			JSplitPane split = (JSplitPane) comp;
			applyComponentDarkMode(split.getLeftComponent(), bg, fg);
			applyComponentDarkMode(split.getRightComponent(), bg, fg);
		} else if (comp instanceof JLabel || comp instanceof JButton || comp instanceof JToggleButton) {
			comp.setForeground(fg); // 텍스트 요소 색상 적용
		} else if (comp instanceof JButton) {
			comp.setForeground(fg);
			comp.setBackground(bg);
		}
	}

	// Public | Private 이미지 추가
	private ImageIcon getVisibilityIcon(String visibility) {
		String filename = visibility.equalsIgnoreCase("public")
				? "unlocked.png"
				: "locked.png";

		// 상대 경로로 src/icons/ 접근
		String path = "src/icons/" + filename;
		java.io.File file = new java.io.File(path);

		if (!file.exists()) {
			System.err.println("⚠️ 아이콘 파일을 찾을 수 없습니다: " + path);
			return new ImageIcon();
		}

		// 리사이징 처리
		ImageIcon originalIcon = new ImageIcon(path);
		Image scaledImage = originalIcon.getImage().getScaledInstance(30, 40, Image.SCALE_SMOOTH);
		return new ImageIcon(scaledImage); // 파일 경로에서 직접 로딩
	}

	// 저장소 검색 기능
	private void searchRepositories() {
		String keyword = JOptionPane.showInputDialog(this, "검색할 키워드를 입력하세요:");
		if (keyword == null || keyword.trim().isEmpty())
			return;

		listModel.clear();
		try {
			ClientSock.sendCommand("/search_repos " + keyword);

			StringBuilder rawBuilder = new StringBuilder();
			while (true) {
				String part = ClientSock.receiveResponse();
				rawBuilder.append(part);
				if (part.contains("/#/search_repo_EOL"))
					break;
			}

			String fullResponse = rawBuilder.toString().trim();
			System.out.println("[서버 응답]: " + fullResponse);

			// ✅ JSON 부분 추출
			int startIdx = fullResponse.indexOf("/#/search_repo_SOL") + "/#/search_repo_SOL".length();
			int endIdx = fullResponse.indexOf("/#/search_repo_EOL");

			if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
				throw new RuntimeException("JSON 응답 파싱 실패: 구분자 오류");
			}

			String jsonText = fullResponse.substring(startIdx, endIdx).trim();
			System.out.println("[추출된 JSON]: " + jsonText);

			JSONArray jsonArray = new JSONArray(jsonText);
			Set<Integer> addedIds = new HashSet<>();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject obj = jsonArray.getJSONObject(i);
				int id = obj.optInt("id", i);
				String name = obj.getString("name");
				String description = obj.getString("description");
				String visibility = obj.getString("visibility");
				String username = obj.getString("user");
				double filesize = obj.getDouble("size");

				if (!addedIds.contains(id)) {
					Repository repo = new Repository(id, name, description, visibility, username, filesize);
					listModel.addElement(repo);
					addedIds.add(id);
				}
			}

			if (jsonArray.length() == 0) {
				JOptionPane.showMessageDialog(this, "검색 결과가 없습니다.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "저장소 검색 실패");
		}
	}
}
