package models;

import java.sql.Timestamp;

import lombok.*;
@Getter
@Setter

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private Timestamp createdAt;
    
    public User() {}
    
    public User(int id, String username, String password, String email, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
    }
    
    //롬복이 없을 경우 사용
//    // Getters and Setters
//    public int getId() {
//        return id;
//    }
//    
//    public String getUsername() {
//        return username;
//    }
//    
//    public void setUsername(String username) {
//        this.username = username;
//    }
//    
//    public String getPassword() {
//        return password;
//    }
//    
//    public void setPassword(String password) {
//        this.password = password;
//    }
//    
//    public String getEmail() {
//        return email;
//    }
//    
//    public void setEmail(String email) {
//        this.email = email;
//    }
//    
//    public Timestamp getCreatedAt() {
//        return createdAt;
//    }
} 