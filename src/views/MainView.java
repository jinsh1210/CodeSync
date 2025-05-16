package views;

import javax.swing.*;
import java.awt.*;
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

	public MainView(User user) {
		this.currentUser = user;
		this.repositoryDAO = new RepositoryDAO();
		initializeUI();
		loadRepositories();
	}

	private void initializeUI() {
		setTitle("파일 공유 시스템 - " + currentUser.getUsername());
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel toolbarPanel = new JPanel(new BorderLayout());
		JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel rightToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		createRepoButton = new JButton("새 저장소 생성");
		refreshButton = new JButton("새로고침");
		logoutButton = new JButton("로그아웃");

		leftToolbar.add(createRepoButton);
		leftToolbar.add(refreshButton);
		rightToolbar.add(logoutButton);

		toolbarPanel.add(leftToolbar, BorderLayout.WEST);
		toolbarPanel.add(rightToolbar, BorderLayout.EAST);

		listModel = new DefaultListModel<>();
		repositoryList = new JList<>(listModel);
		repositoryList.setCellRenderer(new RepositoryListCellRenderer());
		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(repositoryList);

		mainPanel.add(toolbarPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		add(mainPanel);

		createRepoButton.addActionListener(e -> showCreateRepositoryDialog());
		refreshButton.addActionListener(e -> loadRepositories());
		logoutButton.addActionListener(e -> handleLogout());
		repositoryList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Repository selected = repositoryList.getSelectedValue();
				if (selected != null) {
					openRepository(selected);
				}
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

		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.add(new JLabel("저장소 이름:"));
		panel.add(nameField);
		panel.add(new JLabel("설명:"));
		panel.add(new JScrollPane(descField));

		String[] options = {"private", "public"};
		String selected = (String) JOptionPane.showInputDialog(
			this,
			"리포지토리 공개 여부를 선택하세요",
			"공개 설정",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			"private"
		);

		int result = JOptionPane.showConfirmDialog(this, panel, "새 저장소 생성", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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
				JOptionPane.showMessageDialog(this, "저장소 생성에 실패했습니다.");
			}
		}
	}

	private void openRepository(Repository repository) {
		RepositoryView repoView = new RepositoryView(repository, currentUser);
		repoView.setVisible(true);
	}

	private void handleLogout() {
		int confirm = JOptionPane.showConfirmDialog(this, "로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			LoginView loginView = new LoginView();
			loginView.setVisible(true);
			this.dispose();
		}
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
}