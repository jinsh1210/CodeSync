package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import models.User;
import utils.Style;
import models.Repository;
import utils.ClientSock;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainView extends JFrame {
    private User currentUser;
    private JList<Repository> repositoryList;
    private DefaultListModel<Repository> listModel;
    private JButton createRepoButton;
    private JButton refreshButton;
    private JButton logoutButton;
    private JPopupMenu popupMenu;
    private JButton publicReposButton;


    public MainView(User user) {
        this.currentUser = user;
        initializeUI();
        loadRepositories();
    }

    private void initializeUI() {
        setTitle("J.S.Repo - Main");
        setSize(850, 600);
        setMinimumSize(new Dimension(700, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        mainPanel.setBackground(Style.BACKGROUND_COLOR);

        JLabel titleLabel = new JLabel("어서오세요, " + currentUser.getUsername() + "님");
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setForeground(Style.PRIMARY_COLOR);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Style.BACKGROUND_COLOR);
        topPanel.add(titleLabel, BorderLayout.WEST);

        createRepoButton = Style.createStyledButton("저장소 만들기", Style.PRIMARY_COLOR, Color.WHITE);
        refreshButton = Style.createStyledButton("새로고침", Style.PRIMARY_COLOR, Color.WHITE);
        publicReposButton = Style.createStyledButton("공개 저장소", Style.PRIMARY_COLOR, Color.WHITE);
        logoutButton = Style.createStyledButton("로그아웃", new Color(231, 76, 60), Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Style.BACKGROUND_COLOR);
        buttonPanel.add(createRepoButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(publicReposButton);
        buttonPanel.add(logoutButton);

        topPanel.add(buttonPanel, BorderLayout.EAST);

        listModel = new DefaultListModel<>();
        repositoryList = new JList<>(listModel);
        repositoryList.setCellRenderer(new RepositoryListCellRenderer());
        repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repositoryList.setFont(new Font("Segoe UI", Font.PLAIN, 14));

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

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        createRepoButton.addActionListener(e -> showCreateRepositoryDialog());
        refreshButton.addActionListener(e -> loadRepositories());
        logoutButton.addActionListener(e -> handleLogout());
        publicReposButton.addActionListener(e -> loadPublicRepositories());

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
                int id = i; // 서버에서 id를 전달하지 않으면 인덱스로 임시 부여

                if (!addedIds.contains(id)) {
                    Repository repo = new Repository(id, name, description, visibility);
                    listModel.addElement(repo);
                    addedIds.add(id);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "저장소 로딩 실패");
        }
    }

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

        String[] options = {"private", "public"};
        String selected = (String) JOptionPane.showInputDialog(this, "접근 권한:", "설정",
                JOptionPane.PLAIN_MESSAGE, null, options, "private");

        int result = JOptionPane.showConfirmDialog(this, panel, "저장소 생성", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && selected != null) {
        	String rawName = nameField.getText().trim(); // 사용자가 입력한 원본 이름
        	String name = nameField.getText().trim().replaceAll("\\s+", "_"); // 공백 -> _
            String description = descField.getText().trim();
            
            // 안내 메시지: 이름이 자동 수정되었을 때만 표시
            if (!name.equals(rawName)) {
                JOptionPane.showMessageDialog(this,
                    "저장소 이름에 포함된 공백은 밑줄(_)로 자동 변경됩니다.\n변경된 이름: " + name,
                    "이름 자동 수정",
                    JOptionPane.INFORMATION_MESSAGE);
            }

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "저장소 이름을 입력해주세요.");
                return;
            }

            try {
                ClientSock.sendCommand("/repo_create " + name + " \"" + description + "\" " + selected);
                String response = ClientSock.receiveResponse();
                
                if (response != null && response.contains("/#/repo_create 저장소 생성 성공")) {
                    JOptionPane.showMessageDialog(this, "저장소 생성 성공");
                    loadRepositories();
                } else if (response != null && response.startsWith("/#/error")) {
    				String msg = response.replace("/#/error", "").trim();
    				showErrorDialog("저장소 생성 실패: "+msg);
    			} else {
    				showErrorDialog("알 수 없는 서버 응답: " + response);
    			}
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "서버 연결 실패");
            }
        }
    }

    private void openRepository(Repository repository) {
        try {
            RepositoryView repoView = new RepositoryView(repository, currentUser);
            repoView.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔 확인
            JOptionPane.showMessageDialog(this, "저장소 열기 실패: " + e.getMessage());
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginView().setVisible(true);
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
                setText("(" + repo.getVisibility() + ") " + repo.getName() + " | " + repo.getDescription());
            }
            return this;
        }
    }

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
    private void loadPublicRepositories() {
        listModel.clear();
        try {
            ClientSock.sendCommand("/list_public_repos");

            String line;
            StringBuilder jsonBuilder = new StringBuilder();
            boolean inList = false;

            while ((line = ClientSock.receiveResponse()) != null) {
                if (line.equals("/#/list_public_repos_SOL")) {
                    inList = true;
                    continue;
                } else if (line.equals("/#/list_public_repos_EOL")) {
                    break;
                }

                if (inList) {
                    jsonBuilder.append(line);
                }
            }

            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            Set<Integer> addedIds = new HashSet<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int id = obj.optInt("id", i); // 서버에서 id 제공 시 사용
                String name = obj.getString("name");
                String description = obj.getString("description");
                String visibility = obj.getString("visibility");

                if (!addedIds.contains(id)) {
                    Repository repo = new Repository(id, name, description, visibility);
                    listModel.addElement(repo);
                    addedIds.add(id);
                }
            }

            if (jsonArray.length() == 0) {
                JOptionPane.showMessageDialog(this, "공개 저장소가 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "공개 저장소 로딩 실패");
        }
    }
    private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
	}
}
