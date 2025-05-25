package views.repositoryView;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
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

import lombok.Getter;
import lombok.Setter;
import models.Repository;
import models.User;
import utils.ClientSock;
import utils.Style;

@Getter
@Setter

public class RepoFunc {

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
	private String targetUser = null;
	private String SavedPath = null;

	// 자동 새로고침용 타이머
	private Timer refreshTimer;

	public RepoFunc(Repository repository, User currentUser, JTree fileTree,
			DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel,
			JProgressBar progressBar, Timer refreshTimer) {
		this.repository = repository;
		this.currentUser = currentUser;
		this.fileTree = fileTree;
		this.rootNode = rootNode;
		this.treeModel = treeModel;
		this.progressBar = progressBar;
		this.refreshTimer = refreshTimer;

		// 초기값을 getPath로 불러옴
		this.SavedPath = ClientSock.getPath(currentUser.getUsername(), repository.getName());
		System.out.println("초기 로컬 저장소 경로: " + SavedPath);
	}

	// 개별 컴포넌트에 다크 모드 색상 적용 (재귀적 적용 포함)
	public void applyComponentDarkMode(Component comp, Color bg, Color fg) {
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
	public void loadFiles(String userName) {
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
			restoreExpandedPathsFromStrings(fileTree, expandedPaths);
			fileTree.setRowHeight(24);
			// ⬇ 여기에 추가
			DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value,
						boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

					JLabel label = (JLabel) super.getTreeCellRendererComponent(
							tree, value, sel, expanded, leaf, row, hasFocus);

					String nodeText = value.toString();
					if ("[비어 있음]".equals(nodeText)) {
						label.setIcon(null); // 아이콘 제거
					}

					label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4)); // 마진도 적용

					if (Style.isDarkMode) {
						label.setBackground(sel ? new Color(70, 70, 70) : Style.DARK_BACKGROUND_COLOR);
						label.setForeground(Style.DARK_TEXT_COLOR);
					} else {
						label.setBackground(sel ? getBackgroundSelectionColor() : getBackgroundNonSelectionColor());
						label.setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
					}
					label.setOpaque(true); // 배경 적용

					return label;
				}
			};

			renderer.setLeafIcon(UIManager.getIcon("FileView.fileIcon")); // ✅ 파일 아이콘 지정
			fileTree.setCellRenderer(renderer);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "파일 목록을 불러오는 데 실패했습니다.");
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
	public void handleUpload() {
		// SavedPath 체크 및 지정
		if (SavedPath == null || SavedPath.isEmpty()) {
			JOptionPane.showMessageDialog(null, "로컬 저장소를 지정해주세요");
			return;
		}

		File selectedFile = new File(SavedPath);
		if (!selectedFile.exists()) {
			JOptionPane.showMessageDialog(null, "지정한 경로가 존재하지 않습니다.");
			return;
		}

		new Thread(() -> {
			try {
				refreshTimer.stop();
				ClientSock.push(selectedFile, "", repository.getName(), currentUser.getId(), repository.getUsername(),
						progressBar);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "업로드 중 오류 발생");
			} finally {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
				refreshTimer.start();
				loadFiles(targetUser);
			}
		}).start();
	}

	// 파일 다운로드 처리
	public void handleDownload() {
		// SavedPath 체크 및 지정
		if (SavedPath == null || SavedPath.isEmpty()) {
			JOptionPane.showMessageDialog(null, "로컬 저장소를 지정해주세요");
			return;
		}

		File targetFolder = new File(SavedPath);
		
		new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(true)); // 시작 시 보이게
				refreshTimer.stop();

				ClientSock.pull(
						repository.getName(),
						"",
						targetFolder,
						repository.getUsername(),
						progressBar);

				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(null, "다운로드가 완료되었습니다.");
					progressBar.setVisible(false);
				});

			} catch (Exception e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(null, "다운로드 중 오류 발생");
					progressBar.setVisible(false);
				});
			} finally {
				refreshTimer.start();
			}
		}).start();
	}

	// 파일 또는 폴더 삭제 처리
	public void handleDelete() {
		TreePath selectedPath = fileTree.getSelectionPath();
		if (selectedPath == null) {
			JOptionPane.showMessageDialog(null, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}
		
		Object[] nodes = selectedPath.getPath();

		// 루트 노드 선택 시 이름으로 처리
		String selectedPathStr;
		if (nodes.length == 1) {
			selectedPathStr = repository.getName(); // 루트 이름으로 처리
		} else {
			selectedPathStr = String.join("/",
					Arrays.stream(nodes, 1, nodes.length)
							.map(Object::toString)
							.toArray(String[]::new));
		}

		// 비어 있음 노드 처리
		if (selectedPathStr.endsWith("[비어 있음]")) {
			selectedPathStr = selectedPathStr.replace("/[비어 있음]", "");
		}

		if (selectedPathStr.isBlank()) {
			JOptionPane.showMessageDialog(null, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}
		System.out.println("삭제할 항목: " + selectedPathStr); // 디버그
		int confirm = JOptionPane.showConfirmDialog(null,
				"선택한 항목(" + selectedPathStr + ")을 삭제하시겠습니까?",
				"삭제 확인",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION)
			return;

		try {
			ClientSock.sendCommand("/delete_file " + repository.getName() + " \"" + selectedPathStr + "\" "
					+ repository.getUsername());

			String response = ClientSock.receiveResponse();
			if (response.startsWith("/#/delete_success")) {
				JOptionPane.showMessageDialog(null, "삭제가 완료되었습니다.");
				loadFiles(targetUser);
			} else if (response.startsWith("/#/error")) {
				JOptionPane.showMessageDialog(null, "❌ " + response.replace("/#/error", "").trim());
			} else {
				JOptionPane.showMessageDialog(null, "❓ 알 수 없는 응답: " + response);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "삭제 중 오류가 발생했습니다.");
		}
	}

	public void handlesetLocalFolder() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = chooser.showDialog(null, "로컬 저장소 경로 선택");
		if (result == JFileChooser.APPROVE_OPTION) {
			File selected = chooser.getSelectedFile();
			if (selected != null && selected.isDirectory()) {
				SavedPath = selected.getAbsolutePath()+ File.separator + repository.getName();
				ClientSock.setPath(currentUser.getUsername(), repository.getName(), SavedPath);
				System.out.println("저장된 경로: " + SavedPath);
			} else {
				JOptionPane.showMessageDialog(null, "유효한 폴더를 선택해주세요.");
			}
		} else {
			JOptionPane.showMessageDialog(null, "폴더 선택을 취소했습니다.");
		}
	}
}
