package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
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
		//서버 데베 연결해야됨
		ClientSock.sendCommand("LIST_FILES|" + repository.getId());
		List<FileInfo> files = ClientSock.receiveFileList(repository.getId());
		if (files != null) {
			for (FileInfo file : files) {
				listModel.addElement(file);
			}
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
				setText("파일이름: " + file.getFilename() + " | 브랜치: " + file.getBranch() + " | ("
						+ formatFileSize(file.getFileData().length) + ")");
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
}
