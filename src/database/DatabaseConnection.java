package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/file_sharing_db";
    private static final String USER = "fileuser";
    private static final String PASSWORD = "filepass";
    
    private static DatabaseConnection instance;
    private Connection connection;
    
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    private void createTables() {
        try {
            // users 테이블 생성
            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL," +
                "email VARCHAR(100) UNIQUE NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            // repositories 테이블 생성
            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS repositories (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "description TEXT," +
                "user_id INT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "UNIQUE KEY unique_repo (user_id, name)" +
                ")"
            );
            
            // files 테이블 생성
            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS files (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "repository_id INT NOT NULL," +
                "name VARCHAR(255) NOT NULL," +
                "path VARCHAR(500) NOT NULL," +
                "size BIGINT NOT NULL," +
                "commit_message TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (repository_id) REFERENCES repositories(id)" +
                ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 