package server;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/JSRepo";
    private static final String DB_USER = "Server";
    private static final String DB_PASSWORD = System.getenv("ServPass");
    private Connection conn;
    public UserDAO() {
        // JDBC driver loading
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean repositoryExists(String username, String repoName) {
        String sql = "SELECT 1 FROM repositories WHERE user_id = ? AND name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, repoName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect(){
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println("conn 예외처리");
            e.printStackTrace();
        }
    }

    public String deleteRepository(String username, String repoName) {
        String getRepoIdSql = "SELECT id FROM repositories WHERE user_id = ? AND name = ?";
        String deleteFilesSql = "DELETE FROM files WHERE repository_id = ?";
        String deleteRepoSql = "DELETE FROM repositories WHERE id = ?";

        try (
            PreparedStatement getStmt = conn.prepareStatement(getRepoIdSql);
            PreparedStatement deleteFilesStmt = conn.prepareStatement(deleteFilesSql);
            PreparedStatement deleteRepoStmt = conn.prepareStatement(deleteRepoSql)
        ) {
            // 1. 저장소 UUID 조회
            getStmt.setString(1, username);
            getStmt.setString(2, repoName);
            ResultSet rs = getStmt.executeQuery();

            if (!rs.next()) {
                return "/#/repo_delete_fail 저장소를 찾을 수 없습니다.";
            }

            String repoId = rs.getString("id");

            // 2. files 테이블에서 삭제
            deleteFilesStmt.setString(1, repoId);
            deleteFilesStmt.executeUpdate();

            // 3. repositories 테이블에서 삭제
            deleteRepoStmt.setString(1, repoId);
            int affected = deleteRepoStmt.executeUpdate();

            if (affected > 0) {
                return "/#/repo_delete_success";
            } else {
                return "/#/repo_delete_fail 삭제 실패";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/repo_delete_fail 오류 발생";
        }
    }

    public String deleteFileRecord(String username, String repoName, String relPath) {
        try {
            // 1. repo_id 조회
            String getRepoIdSQL = "SELECT id FROM repositories WHERE user_id = ? AND name = ?";
            String repoId = null;
            try (PreparedStatement getStmt = conn.prepareStatement(getRepoIdSQL)) {
                getStmt.setString(1, username);
                getStmt.setString(2, repoName);
                ResultSet rs = getStmt.executeQuery();
                if (rs.next()) {
                    repoId = rs.getString("id");
                } else {
                    return "/#/delete_record_error 저장소 ID 조회 실패";
                }
            }

            // 2. 경로 완성
            String fullPath = "repos/" + username + "/" + repoName + "/" + relPath;
            File check = new File(fullPath);
            String sql;

            if (check.exists() && check.isDirectory() && !fullPath.endsWith("/")) {
                fullPath += "/";
            }

            if (!fullPath.endsWith("/")) {
                sql = "DELETE FROM files WHERE repository_id = ? AND path LIKE ?";
                fullPath += "%";
            } else {
                sql = "DELETE FROM files WHERE repository_id = ? AND path = ?";
            }
            // 3. 삭제 실행
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, repoId);
                pstmt.setString(2, fullPath);
                int rows = pstmt.executeUpdate();
                return (rows > 0) ? "/#/delete_record_success" : "/#/delete_record_not_found";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "/#/delete_record_error";
        }
    }


    public String isPublicRepository(String owner, String repoName) {
        String sql = "SELECT visibility FROM repositories WHERE user_id = ? AND name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, owner);
            pstmt.setString(2, repoName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("visibility");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "private";
    }

    private double calculateRepositorySize(String owner, String repoName) {
        Path repoPath = Paths.get("repos", owner, repoName);
        if (!Files.exists(repoPath)) return 0.0;

        try (Stream<Path> paths = Files.walk(repoPath)) {
            long totalBytes = paths
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0L;
                    }
                }).sum();

            double sizeInMB = totalBytes / (1024.0 * 1024.0);
            return Math.round(sizeInMB * 100.0) / 100.0;  // 소수점 둘째 자리까지 반올림
        } catch (IOException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public JSONArray getRepositories(String username) {
        JSONArray repoArray = new JSONArray();

        // 1. 내가 소유한 저장소
        String sqlOwned = "SELECT * FROM repositories WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlOwned)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String repoName = rs.getString("name");
                JSONObject repoObj = new JSONObject();
                repoObj.put("name", repoName);
                repoObj.put("description", rs.getString("description"));
                repoObj.put("visibility", rs.getString("visibility"));
                repoObj.put("user", rs.getString("user_id"));
                repoObj.put("created_at", rs.getString("created_at").toString());
                repoObj.put("is_owner", true); // ✅ 내가 주인인 저장소
                double size = calculateRepositorySize(username, repoName);
                repoObj.put("size", size);
                repoArray.put(repoObj);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        // 2. 콜라보레이터로 등록된 저장소
        String sqlCollab = """
            SELECT r.* 
            FROM collaborators c
            JOIN repositories r ON c.repository_id = r.id
            WHERE c.collaborator_id = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlCollab)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String repoName = rs.getString("name");
                String owner = rs.getString("user_id");

                // 중복 방지 (혹시 내가 owner로 등록된 저장소와 겹치는 경우)
                boolean alreadyIncluded = false;
                for (int i = 0; i < repoArray.length(); i++) {
                    JSONObject obj = repoArray.getJSONObject(i);
                    if (obj.getString("name").equals(repoName) && obj.getString("user").equals(owner)) {
                        alreadyIncluded = true;
                        break;
                    }
                }
                if (alreadyIncluded) continue;

                JSONObject repoObj = new JSONObject();
                repoObj.put("name", repoName);
                repoObj.put("description", rs.getString("description"));
                repoObj.put("visibility", rs.getString("visibility"));
                repoObj.put("user", owner);
                repoObj.put("created_at", rs.getString("created_at").toString());
                repoObj.put("is_owner", false); // ✅ 내가 콜라보레이터인 저장소
                double size = calculateRepositorySize(owner, repoName);
                repoObj.put("size", size);
                repoArray.put(repoObj);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return repoArray;
    }

    public boolean searchUser(String usrName){
        String checkSql="SELECT username FROM users WHERE username = ?";
        try(PreparedStatement checkStmt=conn.prepareStatement(checkSql)){
            checkStmt.setString(1, usrName);
            ResultSet rs=checkStmt.executeQuery();
            if(rs.next()) return true;
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
        return false;
    }
    
    // 회원가입 메소드
    public String signup(String username, String password) {
        String checkSql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return "/#/error 이미 존재하는 ID입니다";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/error 회원가입 실패";
        }

        String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return "/#/info "+username;

        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/error 회원가입 실패"; 
        }
    }
    // 로그인 메소드
    public String login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return "/#/info "+ username;
            else return "/#/error 로그인 실패";

        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/error 로그인 실패";
        }
    }

    public String getPublicRepoOwner(String repoName) {
        String sql = "SELECT user_id FROM repositories WHERE name = ? AND visibility = 'public'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 저장소 ID 조회
    public String getRepositoryIdByOwnerAndName(String owner, String repoName) {
        String sql = "SELECT id FROM repositories WHERE user_id = ? AND name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, owner);
            pstmt.setString(2, repoName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 콜라보레이터 추가
    public boolean addCollaborator(String repoId, String userId) {
        String sql = "INSERT IGNORE INTO collaborators (repository_id, collaborator_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoId);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 콜라보레이터 삭제
    public boolean removeCollaborator(String repoId, String userId) {
        String sql = "DELETE FROM collaborators WHERE repository_id = ? AND collaborator_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoId);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }




    // 콜라보레이터 목록 조회
    public JSONArray getCollaborators(String repoId) {
        JSONArray list = new JSONArray();
        String sql = "SELECT collaborator_id FROM collaborators WHERE repository_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                JSONObject user = new JSONObject();
                user.put("user_id", rs.getString("collaborator_id"));
                list.put(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public boolean isCollaborator(String repoId, String username) {
        String sql = "SELECT 1 FROM collaborators WHERE repository_id = ? AND collaborator_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoId);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String getRepositoryOwner(String repoName, String user_id) {
        String sql = "SELECT user_id FROM repositories WHERE name = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoName);
            pstmt.setString(2, user_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 레포지토리 생성 메소드
    public String createRepository(String userId, String name, String description, String visibility) {
        String checkSql = "SELECT id FROM repositories WHERE user_id = ? AND name = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, userId);
            checkStmt.setString(2, name);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                return "/#/error 저장소 이름 중복";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/error 저장소 생성 실패"; 
        }

        String sql = "INSERT INTO repositories (id, name, description, user_id, visibility) VALUES (UUID(), ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setString(3, userId);
            stmt.setString(4, visibility);
            stmt.executeUpdate();
            return "/#/repo_create 저장소 생성 성공";
        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/error 저장소 생성 실패";
        }
    }
    public String recordFileUpload(String username, String repoName, String filePath, String hash) {
        String getRepoIdSQL = "SELECT id FROM repositories WHERE user_id = ? AND name = ?";
        String deleteSQL = "DELETE FROM files WHERE filename = ? AND path = ?";
        String insertSQL = "INSERT INTO files (id, user_id, filename, path, repository_id, hash) VALUES (UUID(), ?, ?, ?, ?, ?)";

        try (
            PreparedStatement repoStmt = conn.prepareStatement(getRepoIdSQL);
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL);
            PreparedStatement insertStmt = conn.prepareStatement(insertSQL)
        ) {
            repoStmt.setString(1, username);
            repoStmt.setString(2, repoName);
            ResultSet rs = repoStmt.executeQuery();

            if (!rs.next()) {
                return "/#/push_error 저장소 ID 조회 실패";
            }

            String repoId = rs.getString("id");
            String filename = Paths.get(filePath).getFileName().toString();

            // delete previous record
            deleteStmt.setString(1, filename);
            deleteStmt.setString(2, filePath);
            deleteStmt.executeUpdate();

            // insert new record
            insertStmt.setString(1, username);
            insertStmt.setString(2, filename);
            insertStmt.setString(3, filePath);
            insertStmt.setString(4, repoId);
            insertStmt.setString(5, hash);
            insertStmt.executeUpdate();

            return "/#/push_success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/push_error DB 기록 실패";
        }
    }
    
    public JSONArray searchPublicRepositories(String keyword) {
        JSONArray result = new JSONArray();
        String sql = "SELECT * FROM repositories WHERE visibility = 'public' AND (name LIKE ? OR user_id LIKE ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String like = keyword + "%";
            pstmt.setString(1, like); // name LIKE '%keyword%'
            pstmt.setString(2, like); // user_id LIKE '%keyword%'

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String repoName = rs.getString("name");
                String userId = rs.getString("user_id");
                JSONObject repo = new JSONObject();
                repo.put("id", rs.getString("id"));
                repo.put("name", rs.getString("name"));
                repo.put("description", rs.getString("description"));
                repo.put("user", rs.getString("user_id"));
                repo.put("created_at", rs.getTimestamp("created_at").toString());
                repo.put("visibility", rs.getString("visibility"));
                result.put(repo);
                double size = calculateRepositorySize(userId, repoName);
                repo.put("size", size);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // 파일 해시 조회 메서드
    public JSONArray getFileHash(String owner, String repoName) {
        JSONArray hashArray = new JSONArray();
        String repoUUID = getRepositoryIdByOwnerAndName(owner, repoName);
        if (repoUUID == null) {
            return hashArray; // 빈 배열 반환
        }

        String sql = "SELECT path, hash FROM files WHERE repository_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repoUUID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("path", rs.getString("path"));
                obj.put("hash", rs.getString("hash") == null ? "" : rs.getString("hash"));
                hashArray.put(obj);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hashArray;
    }
    public String updateRepositoryVisibility(String requester, String repoName, String newVisibility) {
    if (!newVisibility.equals("public") && !newVisibility.equals("private")) {
        return "/#/visibility_update_fail 잘못된 공개 설정 값";
    }

    // 1. 요청자가 해당 저장소의 소유자인지 확인
    String checkSql = "SELECT user_id FROM repositories WHERE name = ?";
    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setString(1, repoName);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            String owner = rs.getString("user_id");
            if (!requester.equals(owner)) {
                return "/#/visibility_update_fail 권한이 없습니다.";
            }
        } else {
            return "/#/visibility_update_fail 저장소를 찾을 수 없습니다.";
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return "/#/visibility_update_fail 서버 오류";
    }

    // 2. 공개 여부 변경
    String updateSql = "UPDATE repositories SET visibility = ? WHERE name = ? AND user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, newVisibility);
            stmt.setString(2, repoName);
            stmt.setString(3, requester);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                return "/#/visibility_update_success " + newVisibility;
            } else {
                return "/#/visibility_update_fail 업데이트 실패";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "/#/visibility_update_fail 서버 오류";
        }
    }
}

