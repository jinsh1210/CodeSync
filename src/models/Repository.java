package models;

import java.sql.Timestamp;
import lombok.*;

@Getter
@Setter

public class Repository {
    private int id;
    private String name;
    private String description;
    private int userId;
    private Timestamp createdAt;
    private String visibility;
    
    public Repository(int id, String name, String description, int userId, Timestamp createdAt, String visibility) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.createdAt = createdAt;
        this.visibility = visibility;
    }

    public Repository(int id, String name, String description, String visibility) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    
    //롬복이 없을 경우 사용
//    // Getters and Setters
//    public int getId() {
//        return id;
//    }
//    
//    public String getName() {
//        return name;
//    }
//    
//    public void setName(String name) {
//        this.name = name;
//    }
//    
//    public String getDescription() {
//        return description;
//    }
//    
//    public void setDescription(String description) {
//        this.description = description;
//    }
//    
//    public int getUserId() {
//        return userId;
//    }
//    
//    public Timestamp getCreatedAt() {
//        return createdAt;
//    }
} 