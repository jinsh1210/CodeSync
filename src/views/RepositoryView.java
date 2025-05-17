package views;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import models.User;
import utils.Style;
import models.Repository;
import models.FileInfo;
import database.RepositoryDAO;

public class RepositoryView extends JFrame {
	private Repository repository;
	private User currentUser;
	private RepositoryDAO repositoryDAO;
	private JList<FileInfo> fileList;
	private DefaultListModel<FileInfo> listModel;
	private JButton uploadButton;
	private JButton downloadButton;
	private JButton deleteButton;
	private Timer refreshTimer;

	public RepositoryView(Repository repository, User currentUser) {
		this.repository = repository;
		this.currentUser = currentUser;
		this.repositoryDAO = new RepositoryDAO();
		initializeUI();
		loadFiles();
	}

	private void initializeUI() {
		// 전체 화면 구성
		setTitle("J.S.Repo - Repository");
		setSize(550, 600);
		setMinimumSize(new Dimension(500, 600));

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// 헤더
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

		// 버튼
		uploadButton = Style.createStyledButton("파일 업로드", Style.PRIMARY_COLOR, Color.WHITE);
		downloadButton = Style.createStyledButton("다운로드", new Color(41, 128, 185), Color.WHITE);
		deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(uploadButton);
		buttonPanel.add(downloadButton);
		buttonPanel.add(deleteButton);

		// 저장소 파일 리스트
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

		// 기능 연결
		uploadButton.addActionListener(e -> handleUpload());
		downloadButton.addActionListener(e -> handleDownload());
		deleteButton.addActionListener(e -> handleDelete());
		refreshTimer = new Timer(3000, e -> loadFiles());
		refreshTimer.start();

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				if (refreshTimer != null && refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});
	}

	// DB에서 파일 불러오기
	private void loadFiles() {
		listModel.clear();
		List<FileInfo> files = repositoryDAO.getRepositoryFiles(repository.getId());
		for (FileInfo file : files) {
			listModel.addElement(file);
		}
	}

	// DB에 파일 업로드 로직
	private void handleUpload() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
				FileInfo fileInfo = new FileInfo(0, currentUser.getId(), repository.getId(), selectedFile.getName(),
						fileBytes, new java.sql.Timestamp(System.currentTimeMillis()));
				if (repositoryDAO.addFile(fileInfo)) {
					loadFiles();
				} else {
					JOptionPane.showMessageDialog(this, "파일 업로드에 실패했습니다.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "파일 업로드 중 오류가 발생했습니다.");
			}
		}
	}

	// 다운로드 로직
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

	// 삭제 로직
	private void handleDelete() {
		List<FileInfo> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty()) {
			JOptionPane.showMessageDialog(this, "삭제할 파일을 선택해주세요.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "선택한 " 
				+ selectedFiles.size() 
				+ "개의 파일을 정말로 삭제하시겠습니까?",
				"파일 삭제 확인", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			int deletedCount = 0;
			int failedCount = 0;

			for (FileInfo file : selectedFiles) {
				if (repositoryDAO.deleteFile(file.getId(), currentUser.getId())) {
					deletedCount++;
				} else {
					failedCount++;
				}
			}

			String message = "";
			if (deletedCount > 0) {
			    message += deletedCount + "개의 파일이 삭제되었습니다.";
			}
			if (failedCount > 0) {
			    if (!message.isEmpty()) message += "\n";
			    message += failedCount + "개의 파일은 삭제 권한이 없거나 오류가 발생했습니다.";
			}
			if (!message.isEmpty()) {
	            JOptionPane.showMessageDialog(this, message);
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
				setText(file.getFilename() + "  (" + formatFileSize(file.getFileData().length) + ")");
			}
			return this;
		}

		// 단위 표시
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
