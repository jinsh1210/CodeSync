package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.json.JSONArray;
import org.json.JSONObject;

import models.Repository;
import models.User;
import utils.ClientSock;
import utils.Style;

//저장소 내부 파일 목록을 보여주는 뷰
public class RepositoryView extends JFrame {
	// 저장소 정보 및 사용자 정보
	private Repository repository;
	private User currentUser;

	// 마지막으로 선택된 경로
	private String lastSelectedPath = "";

	// 파일 트리 및 관련 구성요소
	private JTree fileTree;
	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel treeModel;
	// 진행율
	private JProgressBar progressBar; // 선언 위치는 클래스 상단 필드에 추가
	// 상단 버튼들
	private JButton uploadButton;
	private JButton downloadButton;
	private JButton deleteButton;
	private String targetUser = null;

	// 자동 새로고침용 타이머
	private Timer refreshTimer;

	// 생성자 - 저장소 및 사용자 정보 전달받아 UI 초기화
	public RepositoryView(Repository repository, User currentUser, String targetUser) {
		this.repository = repository;
		this.currentUser = currentUser;
		this.targetUser = targetUser;
		initializeUI();
		loadFiles(targetUser);
	}

	// 전체 UI를 초기화하고 구성 요소(제목, 설명, 트리, 버튼 등)를 배치함
	private void initializeUI() {
		// 프레임 제목, 크기, 닫힘 동작 등 기본 설정
		setTitle("J.S.Repo - Repository");
		setSize(550, 600);
		setMinimumSize(new Dimension(500, 600));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		//파일탐색기 디자인변경
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace(); // 실패해도 앱은 정상 작동함
		}

		// 전체 구성요소를 담는 메인 패널 설정 (여백 및 배경색 포함)
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 저장소 이름을 보여주는 제목 라벨 생성
		JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);

		// 저장소 설명을 HTML 형식으로 변환하여 표시할 라벨 생성
		String html = "<html>" + repository.getDescription().replace("\n", "<br>") + "</html>";
		JLabel descLabel = new JLabel(html);
		descLabel.setFont(Style.LABEL_FONT);
		descLabel.setForeground(new Color(80, 80, 80));

		// 제목과 설명을 세로로 배치한 상단 헤더 패널 구성
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBackground(Style.BACKGROUND_COLOR);
		headerPanel.add(titleLabel);
		headerPanel.add(Box.createRigidArea(new Dimension(0, 8))); // 제목과 설명 사이 간격
		headerPanel.add(descLabel);

		// 우측 상단 콜라보레이터 버튼 (본인 저장소일 때만 표시)
		JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		topRightPanel.setBackground(Style.BACKGROUND_COLOR);
		if (repository.getUsername().equals(currentUser.getUsername())) {
			JButton collaborateButton = new JButton("");
			collaborateButton.setFocusable(false);
			ImageIcon icon = new ImageIcon("src/icons/collabor.png");
			Image scaledIcon = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
			collaborateButton.setIcon(new ImageIcon(scaledIcon));
			collaborateButton.setContentAreaFilled(false);
			collaborateButton.setBorderPainted(false);
			collaborateButton.setOpaque(false);
			collaborateButton.addActionListener(e -> handleViewCollaborators());
			topRightPanel.add(collaborateButton);
		}

		// 업로드/다운로드 상태를 표시할 진행 바 생성
		progressBar = new JProgressBar(0, 100);
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(400, 20));

		// 진행 바, 제목/설명, 콜라보 버튼을 포함한 헤더 전체 래퍼 구성
		JPanel headerWrapper = new JPanel(new BorderLayout());
		headerWrapper.setBackground(Style.BACKGROUND_COLOR);
		headerWrapper.add(progressBar, BorderLayout.NORTH);
		headerWrapper.add(headerPanel, BorderLayout.CENTER);
		headerWrapper.add(topRightPanel, BorderLayout.EAST);
		mainPanel.add(headerWrapper, BorderLayout.NORTH);

		// 업로드/다운로드/삭제 버튼 생성 및 색상 설정
		uploadButton = Style.createStyledButton("업로드", Style.PRIMARY_COLOR, Color.WHITE);
		downloadButton = Style.createStyledButton("다운로드", new Color(41, 128, 185), Color.WHITE);
		deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);

		// 하단 버튼 영역 패널 구성
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(uploadButton);
		buttonPanel.add(downloadButton);
		buttonPanel.add(deleteButton);

		// 버튼 패널을 포함한 전체 하단 패널 설정
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(Style.BACKGROUND_COLOR);
		bottomPanel.add(buttonPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		// 파일 트리의 루트 노드 및 모델 구성
		rootNode = new DefaultMutableTreeNode(repository.getName());
		treeModel = new DefaultTreeModel(rootNode);
		// 파일 트리 컴포넌트 생성 및 다크모드 스타일 지정
		fileTree = new JTree(treeModel);
		fileTree.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
		fileTree.setRootVisible(true);
		fileTree.setShowsRootHandles(true);
		fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
				Object obj = node.getUserObject();
				if (Style.isDarkMode) {
					setForeground(Style.DARK_TEXT_COLOR);
					setBackgroundNonSelectionColor(Style.DARK_BACKGROUND_COLOR);
					setBackgroundSelectionColor(new Color(70, 70, 70));
				}
				if (obj instanceof String && "[비어 있음]".equals(obj)) {
					setIcon(null);
				}
				return c;
			}
		});
		// 트리를 감싸는 스크롤 패널 생성 및 테두리 설정
		JScrollPane scrollPane = new JScrollPane(fileTree);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// 모든 구성 요소가 담긴 mainPanel을 프레임에 추가
		add(mainPanel);
		// 다크 모드 적용
		applyDarkMode();

		// 버튼에 업로드/다운로드/삭제 기능 연결
		uploadButton.addActionListener(e -> handleUpload());
		downloadButton.addActionListener(e -> handleDownload());
		deleteButton.addActionListener(e -> handleDelete());

		// 자동 새로고침 타이머 설정 (3초 주기)
		refreshTimer = new Timer(3000, e -> loadFiles(targetUser));
		refreshTimer.start();
		// 창이 닫힐 때 타이머 종료 이벤트 설정
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (refreshTimer != null && refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});

		// 트리 항목 선택 시 선택 경로를 저장하는 이벤트 핸들러
		fileTree.addTreeSelectionListener(e -> {
			TreePath path = fileTree.getSelectionPath();
			if (path == null)
				return;

			Object[] nodes = path.getPath();
			lastSelectedPath = String.join("/",
					Arrays.stream(nodes, 1, nodes.length)
							.map(Object::toString)
							.toArray(String[]::new));

			if (lastSelectedPath.endsWith("[비어 있음]")) {
				lastSelectedPath = lastSelectedPath.replace("/[비어 있음]", "");
			}
		});
		System.out.println(lastSelectedPath); //디버그
	}

	// 다크 모드 설정을 UI 전체에 적용함
	private void applyDarkMode() {
		Color bgColor = Style.isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR;
		Color fgColor = Style.isDarkMode ? Style.DARK_TEXT_COLOR : Color.BLACK;

		getContentPane().setBackground(bgColor);
		applyComponentDarkMode(getContentPane(), bgColor, fgColor);
	}

	// 개별 컴포넌트에 다크 모드 색상 적용 (재귀적 적용 포함)
	private void applyComponentDarkMode(Component comp, Color bg, Color fg) {
		if (comp instanceof JPanel || comp instanceof JScrollPane || comp instanceof JTree) {
			comp.setBackground(bg);
			comp.setForeground(fg);
		}

		if (comp instanceof JLabel) {
			comp.setForeground(fg);
		} else if (comp instanceof JButton) {
			JButton button = (JButton) comp;
			button.setBackground(Style.isDarkMode ? Style.DARK_FIELD_BACKGROUND : Style.PRIMARY_COLOR);
			button.setForeground(Style.isDarkMode ? Style.DARK_TEXT_COLOR : Color.WHITE);
		} else if (comp instanceof JScrollPane) {
			JScrollPane scroll = (JScrollPane) comp;
			scroll.getViewport().setBackground(bg);
			Component view = scroll.getViewport().getView();
			if (view != null) {
				applyComponentDarkMode(view, bg, fg);
			}
		}

		if (comp instanceof Container) {
			for (Component child : ((Container) comp).getComponents()) {
				applyComponentDarkMode(child, bg, fg);
			}
		}
	}

	// 서버로부터 파일 목록을 받아와 트리 형태로 표시함
	private void loadFiles(String userName) {
		List<String> expandedPaths = getExpandedPathsAsStrings(fileTree);
		rootNode.removeAllChildren();

		try {
			if (userName == null)
				ClientSock.sendCommand("/repo_content " + repository.getName());
			else
				ClientSock.sendCommand("/repo_content " + userName + " " + repository.getName());
			String response = "";
			while (true) {
				String line = ClientSock.receiveResponse();
				if (line == null)
					break;
				response += line;
				if (line.contains("/#/repo_content_EOL"))
					break;
			}
			int start = response.indexOf("/#/repo_content_SOL") + "/#/repo_content_SOL".length();
			int end = response.indexOf("/#/repo_content_EOL");
			response = response.substring(start, end).trim();
			JSONArray array = new JSONArray(response);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String path = obj.getString("path");
				String type = obj.getString("type");
				addPathToTree(path, type);
			}

			treeModel.reload();
			markEmptyFolders(rootNode);
			restoreExpandedPathsFromStrings(fileTree, expandedPaths);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "파일 목록을 불러오는 데 실패했습니다.");
		}
	}

	// 현재 확장된 트리 노드의 경로를 문자열로 저장
	private List<String> getExpandedPathsAsStrings(JTree tree) {
		List<String> paths = new ArrayList<>();
		TreeModel model = tree.getModel();
		Object root = model.getRoot();
		if (root != null) {
			collectExpandedStrings(tree, new TreePath(root), "", paths);
		}
		return paths;
	}

	// 트리에서 확장된 노드들의 경로를 재귀적으로 수집
	private void collectExpandedStrings(JTree tree, TreePath path, String currentPath, List<String> paths) {
		if (tree.isExpanded(path)) {
			Object node = path.getLastPathComponent();
			String name = node.toString();
			String fullPath = currentPath.isEmpty() ? name : currentPath + "/" + name;
			paths.add(fullPath);

			int childCount = tree.getModel().getChildCount(node);
			for (int i = 0; i < childCount; i++) {
				Object child = tree.getModel().getChild(node, i);
				collectExpandedStrings(tree, path.pathByAddingChild(child), fullPath, paths);
			}
		}
	}

	// 문자열 경로 목록을 바탕으로 트리의 확장 상태를 복원
	private void restoreExpandedPathsFromStrings(JTree tree, List<String> paths) {
		for (String fullPath : paths) {
			TreePath path = findTreePathByString(tree, fullPath);
			if (path != null) {
				tree.expandPath(path);
			}
		}
	}

	// 문자열로 된 경로를 통해 트리의 TreePath 객체를 찾아 반환
	private TreePath findTreePathByString(JTree tree, String fullPath) {
		String[] parts = fullPath.split("/");
		TreeModel model = tree.getModel();
		Object node = model.getRoot();
		TreePath path = new TreePath(node);

		for (int i = 1; i < parts.length; i++) {
			boolean found = false;
			int count = model.getChildCount(node);
			for (int j = 0; j < count; j++) {
				Object child = model.getChild(node, j);
				if (child.toString().equals(parts[i])) {
					path = path.pathByAddingChild(child);
					node = child;
					found = true;
					break;
				}
			}
			if (!found)
				return null;
		}
		return path;
	}

	// 폴더가 비어 있으면 [비어 있음] 표시 추가
	private void markEmptyFolders(DefaultMutableTreeNode node) {
		if (node.getChildCount() == 0) {
			String name = node.getUserObject().toString();
			if (!name.contains(".") && !name.equals("[비어 있음]")) {
				node.add(new DefaultMutableTreeNode("[비어 있음]"));
			}
			return;
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			markEmptyFolders(child);
		}
	}

	// 주어진 경로를 트리에 추가하여 파일/폴더 구조 생성
	private void addPathToTree(String path, String type) {
		String[] parts = path.split("/");
		DefaultMutableTreeNode current = rootNode;

		for (String part : parts) {
			if (part == null || part.isBlank())
				continue;

			DefaultMutableTreeNode child = null;
			for (int j = 0; j < current.getChildCount(); j++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) current.getChildAt(j);
				if (node.getUserObject().equals(part)) {
					child = node;
					break;
				}
			}

			if (child == null) {
				child = new DefaultMutableTreeNode(part);
				current.add(child);
			}
			current = child;
		}
	}

	// 파일 또는 폴더 업로드 기능 처리 (파일 선택 후 서버에 전송)
	private void handleUpload() {
		System.out.println(lastSelectedPath);//디버그
		if (lastSelectedPath == null || lastSelectedPath.endsWith("[비어 있음]")) {
			JOptionPane.showMessageDialog(this, "항목을 먼저 선택해주세요.");
			return;
		}
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();

			try {
				final String selectedPath = lastSelectedPath;
				String adjustedSelectedPath = selectedPath;
				if (adjustedSelectedPath.contains(".") && !adjustedSelectedPath.endsWith("/")) {
					int lastSlash = adjustedSelectedPath.lastIndexOf("/");
					adjustedSelectedPath = (lastSlash != -1) ? adjustedSelectedPath.substring(0, lastSlash) : "";
				}

				if (selectedFile.isFile()) {
					String filename = selectedFile.getName();
					String serverPath = adjustedSelectedPath.equals("") ? filename
							: adjustedSelectedPath + "/" + filename;
					new Thread(() -> {
						try {
							refreshTimer.stop();

							ClientSock.push(selectedFile, repository.getName(), currentUser.getId(), serverPath,
									repository.getUsername(), progressBar);
							refreshTimer.start();
						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(this, "업로드 중 오류 발생");
						} finally {
							SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
							loadFiles(targetUser);
						}
					}).start();
				} else if (selectedFile.isDirectory()) {
					final String dirPath = adjustedSelectedPath;
					new Thread(() -> {
						try {
							refreshTimer.stop();
							ClientSock.push(selectedFile, dirPath, repository.getName(), currentUser.getId(),
									repository.getUsername(), progressBar);
							refreshTimer.start();
						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(this, "업로드 중 오류 발생");
						} finally {
							SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
							loadFiles(targetUser);
						}
					}).start();
				}

				// loadFiles(targetUser);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "업로드 중 오류가 발생했습니다.");
			}
		}
	}

	// 파일 다운로드 처리
	private void handleDownload() {
		if (lastSelectedPath == null || lastSelectedPath.endsWith("[비어 있음]")) {
			JOptionPane.showMessageDialog(this, "항목을 먼저 선택해주세요.");
			return;
		}
		String selectedPath = lastSelectedPath;

		JFileChooser folderChooser = new JFileChooser();
		folderChooser.setDialogTitle("다운로드 경로 선택");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = folderChooser.showDialog(this, "선택");

		if (result != JFileChooser.APPROVE_OPTION)
			return;
		File targetFolder = folderChooser.getSelectedFile();

		new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(true)); // 시작 시 보이게
				refreshTimer.stop();

				ClientSock.pull(
						repository.getName(),
						selectedPath,
						targetFolder,
						repository.getUsername(),
						progressBar);

				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(this, "다운로드가 완료되었습니다.");
					progressBar.setVisible(false);
				});

			} catch (Exception e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(this, "다운로드 중 오류 발생");
					progressBar.setVisible(false);
				});
			} finally {
				refreshTimer.start();
			}
		}).start();
	}

	// 파일 또는 폴더 삭제 처리
	private void handleDelete() {
		if (lastSelectedPath == null || lastSelectedPath.isBlank()||lastSelectedPath.endsWith("[비어 있음]")) {
			JOptionPane.showMessageDialog(this, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}
		System.out.println(lastSelectedPath);//디버그
		int confirm = JOptionPane.showConfirmDialog(this,
				"선택한 항목(" + lastSelectedPath + ")을 삭제하시겠습니까?",
				"삭제 확인",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION)
			return;

		try {
			ClientSock.sendCommand("/delete_file " + repository.getName() + " \"" + lastSelectedPath + "\" "
					+ repository.getUsername());

			String response = ClientSock.receiveResponse();
			if (response.startsWith("/#/delete_success")) {
				JOptionPane.showMessageDialog(this, "삭제가 완료되었습니다.");
				loadFiles(targetUser);
			} else if (response.startsWith("/#/error")) {
				JOptionPane.showMessageDialog(this, "❌ " + response.replace("/#/error", "").trim());
			} else {
				JOptionPane.showMessageDialog(this, "❓ 알 수 없는 응답: " + response);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "삭제 중 오류가 발생했습니다.");
		}
	}

	// 저장소의 콜라보레이터 목록을 표시하고 추가/삭제 기능 제공
	private void handleViewCollaborators() {
		try {
			ClientSock.sendCommand("/list_collaborators " + repository.getName());

			StringBuilder responseBuilder = new StringBuilder();
			boolean started = false;

			while (true) {
				String line = ClientSock.receiveResponse();
				if (line == null)
					break;

				if (line.contains("/#/collaborator_list_SOL")) {
					started = true;
					continue;
				}
				if (line.contains("/#/collaborator_list_EOL")) {
					line = line.replace("/#/collaborator_list_EOL", "");
					responseBuilder.append(line);
					break;
				}
				if (started) {
					responseBuilder.append(line);
				}
			}

			String jsonText = responseBuilder.toString().trim();
			JSONArray collaborators = new JSONArray(jsonText);

			DefaultListModel<String> listModel = new DefaultListModel<>();
			for (int i = 0; i < collaborators.length(); i++) {
				JSONObject userObj = collaborators.getJSONObject(i);
				String userId = userObj.getString("user_id");
				listModel.addElement((i + 1) + ". " + userId);
			}

			JList<String> collaboratorList = new JList<>(listModel);
			collaboratorList.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
			JScrollPane scrollPane = new JScrollPane(collaboratorList);
			scrollPane.setPreferredSize(new Dimension(300, 200));

			// 삭제 우클릭 메뉴
			JPopupMenu menu = new JPopupMenu();
			JMenuItem removeItem = new JMenuItem("삭제");
			removeItem.addActionListener(ev -> {
				int index = collaboratorList.getSelectedIndex();
				if (index >= 0) {
					String selected = listModel.get(index);
					String targetId = selected.substring(selected.indexOf(" ") + 1);
					int confirm = JOptionPane.showConfirmDialog(null,
							"[" + targetId + "] 사용자를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
					if (confirm == JOptionPane.YES_OPTION) {
						try {
							ClientSock.sendCommand("/remove_collaborator " + repository.getName() + " " + targetId);
							String res = ClientSock.receiveResponse();
							if (res.startsWith("/#/remove_collaborator")) {
								listModel.remove(index);
							} else {
								JOptionPane.showMessageDialog(null, "❌ 삭제 실패: " + res.replace("/#/error", "").trim());
							}
						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, "서버 오류");
						}
					}
				}
			});
			menu.add(removeItem);
			collaboratorList.setComponentPopupMenu(menu);

			// 추가 버튼
			JButton addButton = new JButton("추가");
			addButton.addActionListener(e -> {
				String newUser = JOptionPane.showInputDialog(null, "추가할 사용자 아이디 입력:");
				if (newUser != null && !newUser.trim().isEmpty()) {
					try {
						ClientSock.sendCommand("/add_collaborator " + repository.getName() + " " + newUser.trim());
						String response = ClientSock.receiveResponse();
						if (response.startsWith("/#/add_collaborator")) {
							listModel.addElement((listModel.size() + 1) + ". " + newUser.trim());
						} else {
							JOptionPane.showMessageDialog(null, "❌ 추가 실패: " + response.replace("/#/error", "").trim());
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "서버 오류");
					}
				}
			});

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(scrollPane, BorderLayout.CENTER);

			JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			controlPanel.add(addButton);
			panel.add(controlPanel, BorderLayout.SOUTH);

			JOptionPane.showMessageDialog(this, panel, "콜라보레이터 목록", JOptionPane.PLAIN_MESSAGE);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "콜라보레이터 목록을 불러오는 중 오류 발생");
		}
	}
}
