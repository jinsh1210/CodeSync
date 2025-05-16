package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import models.User;
import models.Repository;
import database.RepositoryDAO;

public class MainView extends JFrame {
	private User currentUser;
	private RepositoryDAO repositoryDAO;
	private JList<Repository> repositoryList;
	private DefaultListModel<Repository> listModel;
	private JButton createRepoButton;
	private JButton refreshButton;
	private JButton logoutButton;
	private JPopupMenu popupMenu;

	private static final Color PRIMARY_COLOR = new Color(52, 152, 219);
	private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
	private static final Color FIELD_BACKGROUND = Color.WHITE;
	private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
	private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16);

	public MainView(User user) {
		this.currentUser = user;
		this.repositoryDAO = new RepositoryDAO();
		initializeUI();
		loadRepositories();
	}

	private void initializeUI() {
		setTitle("J.S.Repo - Main");
		setSize(600, 600);
		setMinimumSize(new Dimension(600, 400));

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		mainPanel.setBackground(BACKGROUND_COLOR);

		JLabel titleLabel = new JLabel("어서오세요, " + currentUser.getUsername() + "님");
		titleLabel.setFont(TITLE_FONT);
		titleLabel.setForeground(PRIMARY_COLOR);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(BACKGROUND_COLOR);
		topPanel.add(titleLabel, BorderLayout.WEST);

		createRepoButton = createStyledButton("저장소 만들기", PRIMARY_COLOR, Color.WHITE);
		refreshButton = createStyledButton("새로고침", PRIMARY_COLOR, Color.WHITE);
		logoutButton = createStyledButton("로그아웃", new Color(231, 76, 60), Color.WHITE);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		buttonPanel.add(createRepoButton);
		buttonPanel.add(refreshButton);
		buttonPanel.add(logoutButton);

		topPanel.add(buttonPanel, BorderLayout.EAST);

		listModel = new DefaultListModel<>();
		repositoryList = new JList<>(listModel);
		repositoryList.setCellRenderer(new RepositoryListCellRenderer());
		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryList.setFont(new Font("Segoe UI", Font.PLAIN, 14));

		// ✅ 마우스 클릭으로 정확한 위치만 동작하도록 변경
		repositoryList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = repositoryList.locationToIndex(e.getPoint());
				if (index != -1) {
					Rectangle bounds = repositoryList.getCellBounds(index, index);
					if (bounds.contains(e.getPoint())) {
						repositoryList.setSelectedIndex(index); // ✅ 정확한 항목 선택 보장

						if (SwingUtilities.isRightMouseButton(e)) {
							popupMenu.show(repositoryList, e.getX(), e.getY());
						} else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
							openRepository(listModel.get(index)); // 더블클릭으로 열기 유지
						}
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(repositoryList);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		add(mainPanel);

		createRepoButton.addActionListener(e -> showCreateRepositoryDialog());
		refreshButton.addActionListener(e -> loadRepositories());
		logoutButton.addActionListener(e -> handleLogout());

		popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("삭제");
		popupMenu.add(deleteItem);

		// 삭제 동작
		deleteItem.addActionListener(e -> {
			Repository selected = repositoryList.getSelectedValue();
			if (selected != null) {
				handleDeleteRepository(selected);
			}
		});
	}

	private void loadRepositories() {
		listModel.clear();
		List<Repository> ownRepos = repositoryDAO.getUserRepositories(currentUser.getId());
		List<Repository> publicRepos = repositoryDAO.getPublicRepositories();

		Set<Integer> addedIds = new HashSet<>();
		for (Repository repo : ownRepos) {
			listModel.addElement(repo);
			addedIds.add(repo.getId());
		}

		for (Repository repo : publicRepos) {
			if (!addedIds.contains(repo.getId())) {
				listModel.addElement(repo);
			}
		}
	}

	private void showCreateRepositoryDialog() {
		JTextField nameField = new JTextField();
		JTextArea descField = new JTextArea(3, 20);
		descField.setLineWrap(true);
		descField.setWrapStyleWord(true);

		JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
		panel.setBackground(BACKGROUND_COLOR);
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
			String name = nameField.getText().trim();
			String description = descField.getText().trim();

			if (name.isEmpty()) {
				JOptionPane.showMessageDialog(this, "저장소 이름을 입력해주세요.");
				return;
			}

			if (repositoryDAO.createRepository(name, description, currentUser.getId(), selected)) {
				loadRepositories();
			} else {
				JOptionPane.showMessageDialog(this, "저장소 생성 실패!");
			}
		}
	}

	private void openRepository(Repository repository) {
		RepositoryView repoView = new RepositoryView(repository, currentUser);
		repoView.setVisible(true);
	}

	private void handleLogout() {
		int confirm = JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			LoginView loginView = new LoginView();
			loginView.setVisible(true);
			this.dispose();
		}
	}

	private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
		JButton button = new JButton(text);
		button.setFont(BUTTON_FONT);
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);   // 🔧 배경을 채움
	    button.setOpaque(true);              // 🔧 불투명으로 설정
		return button;
	}

	private class RepositoryListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Repository) {
				Repository repo = (Repository) value;
				setText(repo.getName() + " - " + repo.getDescription());
			}
			return this;
		}
	}

	private void handleDeleteRepository(Repository selected) {
		int confirm = JOptionPane.showConfirmDialog(this, "정말로 '" + selected.getName() + "' 저장소를 삭제하시겠습니까?",
				"저장소 삭제 확인", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			boolean deleted = repositoryDAO.deleteRepository(selected.getId(), currentUser.getId());
			if (deleted) {
				JOptionPane.showMessageDialog(this, "저장소가 삭제되었습니다.");
				loadRepositories();
			} else {
				JOptionPane.showMessageDialog(this, "삭제에 실패했습니다. 권한이 없거나 서버 오류입니다.");
			}
		}
	}
}
