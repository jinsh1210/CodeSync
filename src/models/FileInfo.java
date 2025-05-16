package models;

import java.sql.Timestamp;

public class FileInfo {
	private int id;
	private int userId;
	private int repositoryId;
	private String filename;
	private byte[] fileData;
	private Timestamp uploadTime;

	public FileInfo(int id, int userId, int repositoryId, String filename, byte[] fileData, Timestamp uploadTime) {
		this.id = id;
		this.userId = userId;
		this.repositoryId = repositoryId;
		this.filename = filename;
		this.fileData = fileData;
		this.uploadTime = uploadTime;
	}

	// Getters and Setters
	public int getId() {
		return id;
	}

	public int getUserId() {
		return userId;
	}

	public int getRepositoryId() {
		return repositoryId;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}

	public Timestamp getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(Timestamp uploadTime) {
		this.uploadTime = uploadTime;
	}
}
