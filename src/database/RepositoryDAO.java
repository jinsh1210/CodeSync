package database;

import models.Repository;
import models.FileInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryDAO {
	private Connection connection;

	public RepositoryDAO() {
		connection = DatabaseConnection.getInstance().getConnection();
	}

	// 목록 관련..
	// 사용자별 저장소 목록
	public List<Repository> getUserRepositories(int userId) {
		List<Repository> repositories = new ArrayList<>();
		String sql = "SELECT * FROM repositories WHERE user_id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, userId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				repositories.add(new Repository(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
						rs.getInt("user_id"), rs.getTimestamp("created_at"), rs.getString("visibility")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return repositories;
	}

	// 저장소 파일 목록
	public List<FileInfo> getRepositoryFiles(int repositoryId) {
		List<FileInfo> files = new ArrayList<>();
		String sql = "SELECT * FROM files WHERE repository_id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, repositoryId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				files.add(new FileInfo(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("repository_id"),
						rs.getString("filename"), rs.getBytes("file_data"), rs.getTimestamp("upload_time"),
						rs.getString("branch")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return files;
	}

	// 파일 관련..
	// 파일 업로드
	public boolean addFile(FileInfo file) {
		String sql = "INSERT INTO files (user_id, repository_id, filename, file_data, upload_time) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, file.getUserId());
			pstmt.setInt(2, file.getRepositoryId());
			pstmt.setString(3, file.getFilename());
			pstmt.setBytes(4, file.getFileData());
			pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 파일 삭제
	public boolean deleteFile(int fileId, int userId) {
		String sql = "DELETE FROM files WHERE id = ? AND user_id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, fileId);
			pstmt.setInt(2, userId);
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 저장소 관련..
	// 저장소 생성
	public boolean createRepository(String name, String description, int userId, String visibility) {
		String sql = "INSERT INTO repositories (name, description, user_id, visibility) VALUES (?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, name);
			pstmt.setString(2, description);
			pstmt.setInt(3, userId);
			pstmt.setString(4, visibility); // 'public' 또는 'private'
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 저장소 삭제
	public boolean deleteRepository(int repoId, int userId) {
		String sql = "DELETE FROM repositories WHERE id = ? AND user_id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, repoId);
			pstmt.setInt(2, userId);
			int affected = pstmt.executeUpdate();
			return affected > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 조회 관련..
	// 개인(private) 저장소 조회
	public Repository getRepository(int id) {
		String sql = "SELECT * FROM repositories WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return new Repository(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
						rs.getInt("user_id"), rs.getTimestamp("created_at"), rs.getString("visibility"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// 공개(public) 저장소 조회
	public List<Repository> getPublicRepositories() {
		List<Repository> repositories = new ArrayList<>();
		String sql = "SELECT * FROM repositories WHERE visibility = 'public'";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				repositories.add(new Repository(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
						rs.getInt("user_id"), rs.getTimestamp("created_at"), rs.getString("visibility")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return repositories;
	}
}
