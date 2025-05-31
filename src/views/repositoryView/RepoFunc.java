package views.repositoryView;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
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
import views.MainView.MainFunc;

@Getter
@Setter
// 저장소 기능 클래스
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
	private MainFunc mainFunc;

	// 생성자
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

	// 파일 목록 불러오기
	public void loadFiles(String userName) {
		List<String> expandedPaths = getExpandedPathsAsStrings(fileTree);
		try {
			if (userName == null) {
				ClientSock.sendCommand("/repo_content " + repository.getName());
			} else {
				ClientSock.sendCommand("/repo_content " + userName + " " + repository.getName());
			}
			String response = "";
			while (true) {
				String line = ClientSock.receiveResponse();
				if (line.contains("/#/repo_content_error 저장소가 존재하지 않습니다")) {
					JOptionPane.showMessageDialog(null, "저장소가 존재하지 않습니다.", "에러", JOptionPane.ERROR_MESSAGE);
					refreshTimer.stop();
					return;
				} else if (line.contains("/#/repo_content_error 접근 권한이 없습니다")) {
					refreshTimer.stop();
					return;
				}
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

			List<String> newPaths = new ArrayList<>();
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String path = obj.getString("path");
				newPaths.add(path);
			}
			// 현재 트리에서 path 리스트 추출
			List<String> currentPaths = new ArrayList<>();
			java.util.Enumeration<?> e = rootNode.depthFirstEnumeration();
			while (e.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
				javax.swing.tree.TreeNode[] nodes = node.getPath();
				if (nodes.length > 1) {
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < nodes.length; i++) {
						sb.append(nodes[i].toString());
						if (i < nodes.length - 1)
							sb.append("/");
					}
					currentPaths.add(sb.toString() + (node.isLeaf() ? "" : "/"));
				}
			}
			java.util.Collections.sort(newPaths);
			java.util.Collections.sort(currentPaths);
			if (newPaths.equals(currentPaths)) {
				if (rootNode.getChildCount() == 0) {
					rootNode.add(new DefaultMutableTreeNode("[비어 있음]"));
					treeModel.reload();
				}
				return; // 변경사항이 없으므로 JTree 리프레시 생략
			}

			rootNode.removeAllChildren();
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
					label.setOpaque(false);
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

	// 트리 확장 경로 문자열로 저장
	private List<String> getExpandedPathsAsStrings(JTree tree) {
		List<String> paths = new ArrayList<>();
		// 트리의 모델(계층 구조)을 가져옴
		TreeModel model = tree.getModel();
		// 트리의 루트 노드를 가져옴
		Object root = model.getRoot();
		// 루트 노드가 존재할 경우
		if (root != null) {
			// 루트부터 재귀적으로 경로를 수집
			collectExpandedStrings(tree, new TreePath(root), "", paths);
		}
		// 경로 리스트 반환
		return paths;
	}

	// 확장 경로 재귀적으로 모으기
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

	// 확장 경로 복원하기
	private void restoreExpandedPathsFromStrings(JTree tree, List<String> paths) {
		for (String fullPath : paths) {
			TreePath path = findTreePathByString(tree, fullPath);
			if (path != null)
				tree.expandPath(path);
		}
	}

	// 문자열 경로를 트리 경로로 찾기
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

	// 비어있는 폴더 표시하기
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

	// 경로를 트리에 추가하기
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

	// 파일 업로드하기
	public void handleUpload() {
		ClientSock.sendCommand("/push " + repository.getName() + " \"" + "checkPermission" + "\" " + 0 + " "
				+ repository.getUsername());
		if (ClientSock.receiveResponse().startsWith("/#/push_error 이 저장소에 푸시할 권한이 없습니다.")) {
			JOptionPane.showMessageDialog(null, "권한이 없습니다.", "권한 오류", JOptionPane.ERROR_MESSAGE);
			return;
		}
		System.out.println("권한확인완료");
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
					if (errorArray != null) {
						for (int i = 0; i < errorArray.length(); i++) {
							String errorPath = errorArray.getString(i);
							String fileName = errorPath.substring(errorPath.lastIndexOf("/") + 1);
							fileNames.append(fileName);
							if (i < errorArray.length() - 1) {
								fileNames.append(", ");
							}
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
							ClientSock.getHash(repository.getName(), currentUser.getUsername());
							break;
						case 2: // 취소
							// 아무것도 하지 않음
							break;
						default:
							break;
					}
				} else {
					ClientSock.push(selectedFile, "", repository.getName(), currentUser.getId(),
							repository.getUsername(), progressBar, array);
					ClientSock.getHash(repository.getName(), repository.getUsername());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "업로드 중 오류 발생");
			} finally {
				SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
				refreshTimer.start();
			}
		}).start();
	}

	// 파일 해시값 계산하기
	private String computeFileHash(Path path) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (InputStream is = Files.newInputStream(path)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : digest.digest()) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	// 파일 다운로드하기
	public void handleDownload() {
		if (SavedPath == null || SavedPath.isEmpty()) {
			JOptionPane.showMessageDialog(null, "로컬 저장소를 지정해주세요");
			return;
		}
		File targetFolder = new File(SavedPath);

		// 병합 충돌 감지
		try {
			File hashFile = new File(ClientSock.getPath(currentUser.getUsername(), repository.getName()),
					".jsRepohashed.json");
			if (hashFile.exists()) {
				String jsonText = Files.readString(hashFile.toPath());
				JSONArray hashArray = new JSONArray(jsonText);
				List<String> conflictFiles = new ArrayList<>();

				for (int i = 0; i < hashArray.length(); i++) {
					JSONObject obj = hashArray.getJSONObject(i);
					if (obj.optBoolean("freeze", false))
						continue;

					String serverPath = obj.getString("path");
					// serverPath is like "repos/username/reponame/relative/path/to/file"
					String basePrefix = "repos/" + repository.getUsername() + "/" + repository.getName() + "/";
					if (!serverPath.startsWith(basePrefix))
						continue;

					String relativePath = serverPath.substring(basePrefix.length());
					Path localPath = Path.of(SavedPath, relativePath).normalize();
					File localFile = localPath.toFile();
					if (!localFile.exists() || localFile.isDirectory())
						continue;

					String currentHash = computeFileHash(localFile.toPath());
					String savedHash = obj.getString("hash");

					if (!currentHash.equals(savedHash)) {
						conflictFiles.add(relativePath);
					}
				}

				if (!conflictFiles.isEmpty()) {
					int option = JOptionPane.showConfirmDialog(null,
							"수정된 파일이 감지되었습니다:\n" + String.join("\n", conflictFiles) +
									"\n서버 파일로 덮어쓰시겠습니까?",
							"병합 충돌 감지", JOptionPane.YES_NO_OPTION);
					if (option != JOptionPane.YES_OPTION) {
						return;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "병합 충돌 검사 중 오류 발생");
			return;
		}

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

	// 파일 또는 폴더 삭제하기
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

	// 로컬 저장소 경로 지정하기
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
