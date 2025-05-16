package views;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import models.User;
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

	public RepositoryView(Repository repository, User currentUser) {
		this.repository = repository;
		this.currentUser = currentUser;
		this.repositoryDAO = new RepositoryDAO();
		initializeUI();
		loadFiles();
	}

	private void initializeUI() {
		setTitle(repository.getName());
		setSize(700, 500);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel infoPanel = new JPanel(new BorderLayout());
		JLabel descLabel = new JLabel(repository.getDescription());
		descLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		infoPanel.add(descLabel, BorderLayout.CENTER);

		JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		uploadButton = new JButton("파일 업로드");
		downloadButton = new JButton("다운로드");
		deleteButton = new JButton("삭제");

		toolbarPanel.add(uploadButton);
		toolbarPanel.add(downloadButton);
		toolbarPanel.add(deleteButton);

		listModel = new DefaultListModel<>();
		fileList = new JList<>(listModel);
		fileList.setCellRenderer(new FileListCellRenderer());
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(fileList);

		mainPanel.add(infoPanel, BorderLayout.NORTH);
		mainPanel.add(toolbarPanel, BorderLayout.CENTER);
		mainPanel.add(scrollPane, BorderLayout.SOUTH);

		add(mainPanel);

		uploadButton.addActionListener(e -> handleUpload());
		downloadButton.addActionListener(e -> handleDownload());
		deleteButton.addActionListener(e -> handleDelete());
	}

	private void loadFiles() {
		listModel.clear();
		List<FileInfo> files = repositoryDAO.getRepositoryFiles(repository.getId());
		for (FileInfo file : files) {
			listModel.addElement(file);
		}
	}

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

	private void handleDownload() {
		FileInfo selectedFile = fileList.getSelectedValue();
		if (selectedFile == null) {
			JOptionPane.showMessageDialog(this, "다운로드할 파일을 선택해주세요.");
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(selectedFile.getFilename()));
		int result = fileChooser.showSaveDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File targetFile = fileChooser.getSelectedFile();
			try (FileOutputStream fos = new FileOutputStream(targetFile)) {
				fos.write(selectedFile.getFileData());
				JOptionPane.showMessageDialog(this, "파일이 다운로드되었습니다.");
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "파일 다운로드 중 오류가 발생했습니다.");
			}
		}
	}

	private void handleDelete() {
		FileInfo selectedFile = fileList.getSelectedValue();
		if (selectedFile == null) {
			JOptionPane.showMessageDialog(this, "삭제할 파일을 선택해주세요.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 파일을 삭제하시겠습니까?", "파일 삭제", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			boolean deleted = repositoryDAO.deleteFile(selectedFile.getId());
			if (deleted) {
				JOptionPane.showMessageDialog(this, "파일이 삭제되었습니다.");
				loadFiles();
			} else {
				JOptionPane.showMessageDialog(this, "파일 삭제에 실패했습니다.");
			}
		}
	}

	private class FileListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof FileInfo) {
				FileInfo file = (FileInfo) value;
				setText(file.getFilename() + " - " + formatFileSize(file.getFileData().length));
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