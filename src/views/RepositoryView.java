package views;

import javax.swing.*;
import javax.swing.tree.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.User;
import models.Repository;
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

	// 상단 버튼들
	private JButton uploadButton;
	private JButton downloadButton;
	private JButton deleteButton;
	private String targetUser = null;

	// 자동 새로고침용 타이머
	private Timer refreshTimer;
	private JButton collaborateButton;

	// 생성자 - 저장소 및 사용자 정보 전달받아 UI 초기화
	public RepositoryView(Repository repository, User currentUser, String targetUser) {
		this.repository = repository;
		this.currentUser = currentUser;
		this.targetUser = targetUser;
		initializeUI();
		loadFiles(targetUser);
	}

	// UI 구성 초기화
	private void initializeUI() {
		setTitle("J.S.Repo - Repository");
		setSize(550, 600);
		setMinimumSize(new Dimension(500, 600));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		// 전체 패널 및 제목, 설명
		JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);

		String html = "<html>" + repository.getDescription().replace("\n", "<br>") + "</html>";
		JLabel descLabel = new JLabel(html);
		descLabel.setFont(Style.LABEL_FONT);
		descLabel.setForeground(new Color(80, 80, 80));

		// 상단 제목 및 설명 패널
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(Style.BACKGROUND_COLOR);
		headerPanel.add(titleLabel, BorderLayout.NORTH);
		headerPanel.add(descLabel, BorderLayout.SOUTH);

		JPanel headerWrapper = new JPanel(new BorderLayout());
		headerWrapper.setBackground(Style.BACKGROUND_COLOR);

		headerWrapper.add(headerPanel, BorderLayout.CENTER); // 제목 + 설명
		if (repository.getUsername().equals(currentUser.getUsername())) {

			JButton collaborateButton = new JButton("");
			collaborateButton.setMargin(new Insets(2, 4, 2, 4));
			collaborateButton.setFocusable(false);
			ImageIcon refreshIcon = new ImageIcon("src/icons/collabor.png");
			Image scaledrefresh = refreshIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
			collaborateButton.setIcon(new ImageIcon(scaledrefresh));
			collaborateButton.setBackground(Color.WHITE); // 다크모드는 applyDarkMode에서 반영
			collaborateButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
			collaborateButton.setFocusPainted(false); // 포커스 테두리 제거
			collaborateButton.setBorderPainted(false); // 버튼 테두리 제거
			collaborateButton.setContentAreaFilled(false); // 배경 채우기 제거
			collaborateButton.setOpaque(false); // 불투명 설정 해제
			
			collaborateButton.addActionListener(e -> handleViewCollaborators());

			JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			topRightPanel.setBackground(Style.BACKGROUND_COLOR);
			topRightPanel.add(collaborateButton);

			headerWrapper.add(topRightPanel, BorderLayout.EAST);
		}
		mainPanel.add(headerWrapper, BorderLayout.NORTH);

		// 버튼 초기화 및 스타일 적용
		uploadButton = Style.createStyledButton("업로드", Style.PRIMARY_COLOR, Color.WHITE);
		downloadButton = Style.createStyledButton("다운로드", new Color(41, 128, 185), Color.WHITE);
		deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(uploadButton);
		buttonPanel.add(downloadButton);
		buttonPanel.add(deleteButton);

		// 파일 트리 구성
		rootNode = new DefaultMutableTreeNode(repository.getName());
		treeModel = new DefaultTreeModel(rootNode);
		fileTree = new JTree(treeModel);
		fileTree.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
		fileTree.setRootVisible(true);
		fileTree.setShowsRootHandles(true);

		// 아이콘 제거 설정 (비어있음 표시)
		fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
				Object obj = node.getUserObject();

				// 다크모드일 경우 텍스트 색상 변경
				if (Style.isDarkMode) {
					setForeground(Style.DARK_TEXT_COLOR);
					setBackgroundNonSelectionColor(Style.DARK_BACKGROUND_COLOR);
					setBackgroundSelectionColor(new Color(70, 70, 70));
				}

				// "[비어 있음]" 항목은 아이콘 제거
				if (obj instanceof String && "[비어 있음]".equals(obj)) {
					setIcon(null);
				}
				return c;
			}
		});

		JScrollPane scrollPane = new JScrollPane(fileTree);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		// 메인 패널 구성
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		add(mainPanel);
		applyDarkMode();

		// 버튼 이벤트 등록
		uploadButton.addActionListener(e -> handleUpload());
		downloadButton.addActionListener(e -> handleDownload());
		deleteButton.addActionListener(e -> handleDelete());

		// 주기적으로 파일 목록 새로고침
		refreshTimer = new Timer(3000, e -> loadFiles(targetUser));
		refreshTimer.start();

		// 창이 닫힐 때 타이머 종료
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (refreshTimer != null && refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});

		// 파일 트리 항목 선택 시 경로 저장
		// fileTree.addTreeSelectionListener(e -> {
		// 	TreePath path = fileTree.getSelectionPath();
		// 	if (path == null || path.getPathCount() <= 1) {
		// 		return;
		// 	}

		// 	DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		// 	String selectedName = selectedNode.getUserObject().toString();

		// 	if ("[비어 있음]".equals(selectedName)) {
		// 		fileTree.clearSelection();
		// 		lastSelectedPath = "";
		// 		return;
		// 	}

		// 	StringBuilder sb = new StringBuilder();
		// 	Object[] nodes = path.getPath();
		// 	for (int i = 1; i < nodes.length; i++) {
		// 		sb.append(nodes[i].toString());
		// 		if (i < nodes.length - 1)
		// 			sb.append("/");
		// 	}
		// 	lastSelectedPath = sb.toString();
		// 	System.out.println("lastSelectedPath :"+lastSelectedPath); //디버그
		// });
		fileTree.addTreeSelectionListener(e -> {
			TreePath path = fileTree.getSelectionPath();
			if (path == null) {
				return;
			}

			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			String selectedName = selectedNode.getUserObject().toString();

			// if ("[비어 있음]".equals(selectedName)) {
			// 	fileTree.clearSelection();
			// 	lastSelectedPath = "";
			// 	return;
			// }

			StringBuilder sb = new StringBuilder();
			Object[] nodes = path.getPath();

			// ✅ 루트 노드만 선택된 경우 (예: MyRepo)
			if (nodes.length == 2) { // [루트, 저장소이름]
				lastSelectedPath = nodes[1].toString();  // 저장소 이름
			} else {
				for (int i = 1; i < nodes.length; i++) {
					sb.append(nodes[i].toString());
					if (i < nodes.length - 1)
						sb.append("/");
				}
				lastSelectedPath = sb.toString();
			}
			if(lastSelectedPath.endsWith("/[비어 있음]")) lastSelectedPath = lastSelectedPath.substring(0, lastSelectedPath.length() - "/[비어 있음]".length());
			System.out.println("lastSelectedPath :" + lastSelectedPath); //디버그
		});

		
	}

	// 다크 모드 적용
	private void applyDarkMode() {
		Color bgColor = Style.isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR;
		Color fgColor = Style.isDarkMode ? Style.DARK_TEXT_COLOR : Color.BLACK;

		getContentPane().setBackground(bgColor);
		applyComponentDarkMode(getContentPane(), bgColor, fgColor);
	}

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

	// 서버로부터 저장소의 파일 목록을 받아 트리로 구성
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

	// 현재 확장된 폴더의 경로들을 문자열로 저장
	private List<String> getExpandedPathsAsStrings(JTree tree) {
		List<String> paths = new ArrayList<>();
		TreeModel model = tree.getModel();
		Object root = model.getRoot();
		if (root != null) {
			collectExpandedStrings(tree, new TreePath(root), "", paths);
		}
		return paths;
	}

	// 트리에서 확장된 경로들을 재귀적으로 수집
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

	// 문자열 경로에 따라 트리 경로를 찾아 확장
	private void restoreExpandedPathsFromStrings(JTree tree, List<String> paths) {
		for (String fullPath : paths) {
			TreePath path = findTreePathByString(tree, fullPath);
			if (path != null) {
				tree.expandPath(path);
			}
		}
	}

	// 문자열 기반 트리 경로 찾기
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

	// 트리에 경로를 추가하여 트리 노드 구성
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

	// 파일 업로드 처리 (파일 또는 폴더)
	// TODO: 콜라보 유저만 가능하게 구현 필요
	private void handleUpload() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();

			try {
				System.out.println("selectedPath 대입전 lastselectedpath: "+lastSelectedPath);
				String selectedPath = lastSelectedPath;
				System.out.println("if 전 selectedPath: "+selectedPath);
				if (selectedPath.contains(".") && !selectedPath.endsWith("/")) {
					int lastSlash = selectedPath.lastIndexOf("/");
					selectedPath = (lastSlash != -1) ? selectedPath.substring(0, lastSlash) : "";
				}

				if (selectedFile.isFile()) {
					String filename = selectedFile.getName();
					System.out.println("selectedPath: "+selectedPath);//디버그
					String serverPath = selectedPath.equals("") ? filename : selectedPath + "/" + filename;
					System.out.println("servPath: "+serverPath); //디버그
					ClientSock.push(selectedFile, repository.getName(), currentUser.getId(), serverPath);
				} else if (selectedFile.isDirectory()) {
					System.out.println("isDirectory: "+selectedPath);//디버그
					ClientSock.push(selectedFile, selectedPath, repository.getName(), currentUser.getId());

				}

				loadFiles(targetUser);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "업로드 중 오류가 발생했습니다.");
			}
		}
	}

	// 파일 다운로드 처리
	private void handleDownload() {
		TreePath path = fileTree.getSelectionPath();
		if (path == null) {
			JOptionPane.showMessageDialog(this, "다운로드할 항목을 선택해주세요.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		Object[] nodes = path.getPath();
		for (int i = 1; i < nodes.length; i++) {
			sb.append(nodes[i].toString());
			if (i < nodes.length - 1)
				sb.append("/");
		}
		String selectedPath = sb.toString();

		JFileChooser folderChooser = new JFileChooser();
		folderChooser.setDialogTitle("다운로드 경로 선택");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = folderChooser.showDialog(this, "선택");

		if (result != JFileChooser.APPROVE_OPTION)
			return;
		File targetFolder = folderChooser.getSelectedFile();

		try {
			new Thread(() -> {
				refreshTimer.stop();
				ClientSock.pull(repository.getName(), selectedPath, targetFolder);
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "다운로드가 완료되었습니다."));
				refreshTimer.start();
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "다운로드 중 오류가 발생했습니다."));
		}
	}

	// 파일 또는 폴더 삭제 처리
	// TODO: 콜라보 유저만 가능하게 구현 필요
	private void handleDelete() {
		TreePath selectedPath = fileTree.getSelectionPath();
		if (selectedPath == null) {
			JOptionPane.showMessageDialog(this, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}

		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
		if (selectedNode.getParent() == null) {
			JOptionPane.showMessageDialog(this, "루트 노드는 삭제할 수 없습니다.");
			return;
		}

		// 트리 경로 문자열 조합
		StringBuilder sb = new StringBuilder();
		Object[] nodes = selectedPath.getPath();
		for (int i = 1; i < nodes.length; i++) {
			sb.append(nodes[i].toString());
			if (i < nodes.length - 1)
				sb.append("/");
		}
		String targetPath = sb.toString();

		int confirm = JOptionPane.showConfirmDialog(this, "선택한 항목(" + targetPath + ")을 삭제하시겠습니까?", "삭제 확인",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION)
			return;

		try {
			ClientSock.sendCommand(
					"/delete_file " + repository.getName() + " " + targetPath + " " + currentUser.getUsername());
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

	// 콜라보 조회
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
