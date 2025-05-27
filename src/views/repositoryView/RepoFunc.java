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

	private Repository repository;
	private User currentUser;
	private String lastSelectedPath = "";
	private JTree fileTree;
	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel treeModel;
	private JProgressBar progressBar;
	private String targetUser = null;
	private String SavedPath = null;
	private Timer refreshTimer;
	private JSONArray array = null;

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
		this.SavedPath = ClientSock.getPath(currentUser.getUsername(), repository.getName());
		System.out.println("초기 로컬 저장소 경로: " + SavedPath);
	}

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
				if(line.contains("/#/repo_content_error 저장소가 존재하지 않습니다")){
					JOptionPane.showMessageDialog(null, "저장소가 존재하지 않습니다.","에러",JOptionPane.ERROR_MESSAGE);
					return;
				}else if(line.contains("/#/repo_content_error 접근 권한이 없습니다")) return;
				if (line == null)
					break;
				response += line;
				if (line.contains("/#/repo_content_EOL"))
					break;
			}
			int start = response.indexOf("/#/repo_content_SOL") + "/#/repo_content_SOL".length();
			int end = response.indexOf("/#/repo_content_EOL");
			response = response.substring(start, end).trim();
			array = new JSONArray(response);
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
			DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value,
						boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
							hasFocus);
					String nodeText = value.toString();
					if ("[비어 있음]".equals(nodeText)) {
						label.setIcon(null);
					}
					label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
					label.setBackground(sel ? getBackgroundSelectionColor() : getBackgroundNonSelectionColor());
					label.setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
					label.setOpaque(true);
					return label;
				}
			};
			renderer.setLeafIcon(UIManager.getIcon("FileView.fileIcon"));
			fileTree.setCellRenderer(renderer);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "파일 목록을 불러오는 데 실패했습니다.");
		}
	}

	private List<String> getExpandedPathsAsStrings(JTree tree) {
		List<String> paths = new ArrayList<>();
		TreeModel model = tree.getModel();
		Object root = model.getRoot();
		if (root != null) {
			collectExpandedStrings(tree, new TreePath(root), "", paths);
		}
		return paths;
	}

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

	private void restoreExpandedPathsFromStrings(JTree tree, List<String> paths) {
		for (String fullPath : paths) {
			TreePath path = findTreePathByString(tree, fullPath);
			if (path != null)
				tree.expandPath(path);
		}
	}

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

	public void handleUpload() {
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
				// Ensure .jsRepohashed.json exists
				File hashFile = new File(SavedPath, ".jsRepohashed.json");
				if (!hashFile.exists()) {
					try (java.io.FileWriter writer = new java.io.FileWriter(hashFile)) {
						writer.write("[]");
						System.out.println("[handleUpload] 초기 해시파일 생성 완료");
					} catch (java.io.IOException e) {
						System.err.println("[handleUpload] 초기 해시파일 생성 실패");
						e.printStackTrace();
					}
				}
				if (!ClientSock.mergeCheck(repository.getName(), repository.getUsername())) {

					JSONArray errorArray = ClientSock.mergeFailed;
					StringBuilder fileNames = new StringBuilder();
					for (int i = 0; i < errorArray.length(); i++) {
						String errorPath = errorArray.getString(i);
						String fileName = errorPath.substring(errorPath.lastIndexOf("/") + 1);
						fileNames.append(fileName);
						if (i < errorArray.length() - 1) {
							fileNames.append(", ");
						}
					}
					int option = JOptionPane.showOptionDialog(null,
							"병합 충돌 발생!\n파일 이름: " + fileNames.toString(), "병합 충돌",
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
							new String[] { "풀", "강제 푸시", "취소" }, "풀");

					switch (option) {
						case 0: // 풀
							handleDownload();
							break;
						case 1: // 강제 푸시
							ClientSock.push(new File(SavedPath), "", repository.getName(), currentUser.getId(),
									repository.getUsername(), progressBar, array);
							handleDownload();
							break;
						case 2: // 취소
							// 아무것도 하지 않음
							break;
						default:
							break;
					}
				} else
					ClientSock.push(selectedFile, "", repository.getName(), currentUser.getId(),
							repository.getUsername(), progressBar, array);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "업로드 중 오류 발생");
			} finally {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
				refreshTimer.start();
			}
		}).start();
	}

	public void handleDownload() {
		if (SavedPath == null || SavedPath.isEmpty()) {
			JOptionPane.showMessageDialog(null, "로컬 저장소를 지정해주세요");
			return;
		}
		File targetFolder = new File(SavedPath);
		new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(true));
				refreshTimer.stop();
				ClientSock.pull(repository.getName(), "", targetFolder, repository.getUsername(), progressBar, array);
				SwingUtilities.invokeLater(() -> {
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

	public void handleDelete() {
		TreePath selectedPath = fileTree.getSelectionPath();
		if (selectedPath == null) {
			JOptionPane.showMessageDialog(null, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}
		Object[] nodes = selectedPath.getPath();
		String selectedPathStr = (nodes.length == 1) ? repository.getName()
				: String.join("/", Arrays.stream(nodes, 1, nodes.length).map(Object::toString).toArray(String[]::new));
		if (selectedPathStr.endsWith("[비어 있음]")) {
			selectedPathStr = selectedPathStr.replace("/[비어 있음]", "");
		}
		if (selectedPathStr.isBlank()) {
			JOptionPane.showMessageDialog(null, "삭제할 항목을 먼저 선택해주세요.");
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(null, "선택한 항목(" + selectedPathStr + ")을 삭제하시겠습니까?", "삭제 확인",
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
				SavedPath = selected.getAbsolutePath() + File.separator + repository.getName();
				ClientSock.setPath(currentUser.getUsername(), repository.getName(), SavedPath);
				System.out.println("저장된 경로: " + SavedPath);
				int option = JOptionPane.showOptionDialog(null,
						"로컬 저장소가 지정되었습니다.\n어떤 작업을 수행하시겠습니까?", "병합 충돌",
						JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
						new String[] { "풀", "푸시", "하지 않음" }, "풀");

				switch (option) {
					case 0: // 풀
						handleDownload();
						break;
					case 1: // 푸시
						handleUpload();
						break;
					case 2: // 취소
						// 아무것도 하지 않음
						break;
					default:
						break;
				}
			} else {
				JOptionPane.showMessageDialog(null, "유효한 폴더를 선택해주세요.");
			}
		}
	}
}
