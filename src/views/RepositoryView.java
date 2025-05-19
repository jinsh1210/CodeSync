package views;

import javax.swing.*;
import javax.swing.tree.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import models.User;
import models.Repository;
import models.FileInfo;
import utils.ClientSock;
import utils.Style;

public class RepositoryView extends JFrame {
    private Repository repository;
    private User currentUser;
	private String lastSelectedPath = "";  // 선택된 경로 저장용
    private JTree fileTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;

    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private Timer refreshTimer;

    public RepositoryView(Repository repository, User currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
        initializeUI();
        loadFiles();
    }

    private void initializeUI() {
        setTitle("J.S.Repo - Repository");
        setSize(550, 600);
        setMinimumSize(new Dimension(500, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(Style.BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setForeground(Style.PRIMARY_COLOR);

        JLabel descLabel = new JLabel("설명 : " + repository.getDescription());
        descLabel.setFont(Style.LABEL_FONT);
        descLabel.setForeground(new Color(80, 80, 80));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Style.BACKGROUND_COLOR);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.SOUTH);

        uploadButton = Style.createStyledButton("업로드", Style.PRIMARY_COLOR, Color.WHITE);
        downloadButton = Style.createStyledButton("다운로드", new Color(41, 128, 185), Color.WHITE);
        deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(Style.BACKGROUND_COLOR);
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(deleteButton);

        rootNode = new DefaultMutableTreeNode("파일 목록");
        treeModel = new DefaultTreeModel(rootNode);
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

				if (obj instanceof String && "[비어 있음]".equals(obj)) {
					setIcon(null); // 아이콘 제거
				}

				return c;
			}
		});
        JScrollPane scrollPane = new JScrollPane(fileTree);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        uploadButton.addActionListener(e -> handleUpload());
        downloadButton.addActionListener(e -> handleDownload());
        deleteButton.addActionListener(e -> handleDelete());

        refreshTimer = new Timer(3000, e -> loadFiles());
        refreshTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (refreshTimer != null && refreshTimer.isRunning()) {
                    refreshTimer.stop();
                }
            }
        });
		fileTree.addTreeSelectionListener(e -> {
			TreePath path = fileTree.getSelectionPath();
			if (path == null || path.getPathCount() <= 1) {
				lastSelectedPath = "";
				return;
			}

			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			String selectedName = selectedNode.getUserObject().toString();

			// ✅ [비어 있음] 노드는 선택 취소
			if ("[비어 있음]".equals(selectedName)) {
				fileTree.clearSelection(); // 선택 제거
				lastSelectedPath = "";
				return;
			}

			StringBuilder sb = new StringBuilder();
			Object[] nodes = path.getPath();
			for (int i = 1; i < nodes.length; i++) {
				sb.append(nodes[i].toString());
				if (i < nodes.length - 1) sb.append("/");
			}
			lastSelectedPath = sb.toString();
		});

    }

    private void loadFiles() {
		List<String> expandedPaths = getExpandedPathsAsStrings(fileTree);
        rootNode.removeAllChildren();

        try {
            ClientSock.sendCommand("/repo_content " + repository.getName());

            String response = "";
            while (true) {
                String line = ClientSock.receiveResponse();
                if (line == null) break;
                response += line;
                if (line.contains("/#/repo_content_EOL")) break;
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
			if (path != null) {
				tree.expandPath(path);
			}
		}
	}

	private TreePath findTreePathByString(JTree tree, String fullPath) {
		String[] parts = fullPath.split("/");
		TreeModel model = tree.getModel();
		Object node = model.getRoot();
		TreePath path = new TreePath(node);

		for (int i = 1; i < parts.length; i++) { // [0]은 root
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
			if (!found) return null;
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

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part == null || part.isBlank()) continue;

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
    // 기존 기능: 그대로 유지

    private void handleUpload() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);  // ✅ 파일 또는 폴더 선택 허용

    int result = fileChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();

			try {
				String selectedPath = lastSelectedPath;

				// 파일 노드가 선택되어 있을 경우 → 상위 폴더 경로로
				if (selectedPath.contains(".") && !selectedPath.endsWith("/")) {
					int lastSlash = selectedPath.lastIndexOf("/");
					selectedPath = (lastSlash != -1) ? selectedPath.substring(0, lastSlash) : "";
				}

				if (selectedFile.isFile()) {
					String filename = selectedFile.getName();
					String serverPath = selectedPath.isEmpty() ? filename : selectedPath + "/" + filename;
					ClientSock.push(selectedFile, repository.getName(), currentUser.getId(), serverPath);

				} else if (selectedFile.isDirectory()) {
					ClientSock.push(selectedFile, selectedPath,repository.getName(),currentUser.getId());
				}

				loadFiles();  // 새로고침

			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "업로드 중 오류가 발생했습니다.");
			}
		}
	}



    private void handleDownload() {
		TreePath path = fileTree.getSelectionPath();
		if (path == null) {
			JOptionPane.showMessageDialog(this, "다운로드할 항목을 선택해주세요.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		Object[] nodes = path.getPath();
		for (int i = 1; i < nodes.length; i++) {  // [0]은 root
			sb.append(nodes[i].toString());
			if (i < nodes.length - 1) sb.append("/");
		}
		String selectedPath = sb.toString();

		JFileChooser folderChooser = new JFileChooser();
		folderChooser.setDialogTitle("다운로드 경로 선택");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = folderChooser.showDialog(this, "선택");

		if (result != JFileChooser.APPROVE_OPTION) return;
		File targetFolder = folderChooser.getSelectedFile();

		
		try {
			new Thread(()->{
				refreshTimer.stop();
				ClientSock.pull(repository.getName(), selectedPath, targetFolder);
				SwingUtilities.invokeLater(() ->
					JOptionPane.showMessageDialog(this, "다운로드가 완료되었습니다."));
				refreshTimer.start();
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() ->
				JOptionPane.showMessageDialog(this, "다운로드 중 오류가 발생했습니다.")
			);
		}
	}


    private void handleDelete() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 먼저 선택해주세요.");
            return;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();

        // 루트 노드 방지
        if (selectedNode.getParent() == null) {
            JOptionPane.showMessageDialog(this, "루트 노드는 삭제할 수 없습니다.");
            return;
        }
		TreePath path = fileTree.getSelectionPath();
        // 트리 경로를 기반으로 실제 경로 생성
        StringBuilder sb = new StringBuilder();
		Object[] nodes = path.getPath();
		for (int i = 1; i < nodes.length; i++) {  // [0]은 root
			sb.append(nodes[i].toString());
			if (i < nodes.length - 1) sb.append("/");
		}
		String targetPath = sb.toString();

        int confirm = JOptionPane.showConfirmDialog(this,
            "선택한 항목(" + targetPath + ")을 삭제하시겠습니까?",
            "삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            // 서버에 삭제 요청
            ClientSock.sendCommand("/delete_file " + repository.getName() + " " + targetPath + " " + currentUser.getUsername());

            // 응답 확인
            String response = ClientSock.receiveResponse();
            if (response.startsWith("/#/delete_success")) {
                JOptionPane.showMessageDialog(this, "삭제가 완료되었습니다.");
                loadFiles();
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
}




//리스트 방식을 트리 형식으로 구현하면서 생긴 과거 코드
/*package views;

import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import models.User;
import utils.Style;
import models.Repository;
import models.FileInfo;
import utils.ClientSock;

public class RepositoryView extends JFrame {
	private Repository repository;
	private User currentUser;
	private JList<FileInfo> fileList;
	private DefaultListModel<FileInfo> listModel;
	private JButton uploadButton;
	private JButton downloadButton;
	private JButton deleteButton;
	private Timer refreshTimer;

	public RepositoryView(Repository repository, User currentUser) {
		this.repository = repository;
		this.currentUser = currentUser;
		initializeUI();
		loadFiles();
	}

	private void initializeUI() {
		setTitle("J.S.Repo - Repository");
		setSize(550, 600);
		setMinimumSize(new Dimension(500, 600));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);

		JLabel descLabel = new JLabel("설명 : " + repository.getDescription());
		descLabel.setFont(Style.LABEL_FONT);
		descLabel.setForeground(new Color(80, 80, 80));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(Style.BACKGROUND_COLOR);
		headerPanel.add(titleLabel, BorderLayout.NORTH);
		headerPanel.add(descLabel, BorderLayout.SOUTH);

		uploadButton = Style.createStyledButton("파일 업로드", Style.PRIMARY_COLOR, Color.WHITE);
		downloadButton = Style.createStyledButton("다운로드", new Color(41, 128, 185), Color.WHITE);
		deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(uploadButton);
		buttonPanel.add(downloadButton);
		buttonPanel.add(deleteButton);

		listModel = new DefaultListModel<>();
		fileList = new JList<>(listModel);
		fileList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		fileList.setCellRenderer(new FileListCellRenderer());
		fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JScrollPane scrollPane = new JScrollPane(fileList);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		add(mainPanel);

		uploadButton.addActionListener(e -> handleUpload());
		downloadButton.addActionListener(e -> handleDownload());
		deleteButton.addActionListener(e -> handleDelete());

		refreshTimer = new Timer(3000, e -> loadFiles());
		refreshTimer.start();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (refreshTimer != null && refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});
	}
	
	// 파일 로딩
	private void loadFiles() {
		listModel.clear();

		try {
			// 명령어 전송
			ClientSock.sendCommand("/repo_content " + repository.getName());
			StringBuilder response = new StringBuilder();

			while (true) {
				String line = ClientSock.receiveResponse();
				if (line == null) break;
				response.append(line);
				if (line.contains("/#/repo_content_EOL")) break;
			}

			String fullResponse = response.toString();
			int start = fullResponse.indexOf("/#/repo_content_SOL") + "/#/repo_content_SOL".length();
			int end = fullResponse.indexOf("/#/repo_content_EOL");
			String json = fullResponse.substring(start, end).trim();

			System.out.println(json);
			JSONArray array = new JSONArray(json);

			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String path = obj.getString("path");
				String type = obj.getString("type");

				FileInfo file = new FileInfo(path, type);
				listModel.addElement(file);
			}
			System.out.println("성공");
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "파일 목록을 불러오는 데 실패했습니다.");
		}
	}

	// 업로드
	private void handleUpload() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			String[] branches = { "main", "feature", "dev" };
			String selectedBranch = (String) JOptionPane.showInputDialog(this, "업로드할 브랜치를 선택하세요:", "브랜치 선택",
					JOptionPane.PLAIN_MESSAGE, null, branches, "main");
			if (selectedBranch == null)
				return;

			try {
				byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
				FileInfo fileInfo = new FileInfo(0, currentUser.getId(), repository.getId(),
						selectedFile.getName(), fileBytes, new java.sql.Timestamp(System.currentTimeMillis()), selectedBranch);
				ClientSock.uploadFileToServer(fileInfo);
				loadFiles();
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "파일 업로드 중 오류가 발생했습니다.");
			}
		}
	}
	// 다운로드
	private void handleDownload() {
		List<FileInfo> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty()) {
			JOptionPane.showMessageDialog(this, "다운로드할 파일을 선택해주세요.");
			return;
		}

		JFileChooser folderChooser = new JFileChooser();
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = folderChooser.showSaveDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File targetFolder = folderChooser.getSelectedFile();
			for (FileInfo file : selectedFiles) {
				try (FileOutputStream fos = new FileOutputStream(new File(targetFolder, file.getFilename()))) {
					fos.write(file.getFileData());
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "파일 저장 중 오류 발생: " + file.getFilename());
				}
			}
			JOptionPane.showMessageDialog(this, "선택한 파일이 다운로드되었습니다.");
		}
	}
	// 삭제
	private void handleDelete() {
		List<FileInfo> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty()) {
			JOptionPane.showMessageDialog(this, "삭제할 파일을 선택해주세요.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "선택한 " + selectedFiles.size() + "개의 파일을 정말로 삭제하시겠습니까?",
				"파일 삭제 확인", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			for (FileInfo file : selectedFiles) {
				ClientSock.sendCommand("DELETE_FILE|" + file.getId() + "|" + currentUser.getId());
			}
			loadFiles();
		}
	}
	// 파일 표시 형식
	private class FileListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
													boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FileInfo) {
				FileInfo file = (FileInfo) value;

				String name = file.getFilename() != null ? file.getFilename() : file.getPath();
				String branch = file.getBranch() != null ? file.getBranch() : file.getType();
				String size = (file.getFileData() != null) ? formatFileSize(file.getFileData().length) : "정보 없음";

				setText("파일이름: " + name + " | 브랜치: " + branch + " | (" + size + ")");
			}
			return this;
		}


		private String formatFileSize(long size) {
			if (size < 1024)
				return size + " B";
			if (size < 1024 * 1024)
				return String.format("%.1f KB", size / 1024.0);
			if (size < 1024 * 1024 * 1024)
				return String.format("%.1f MB", size / (1024.0 * 1024));
			return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
		}
	}
}*/