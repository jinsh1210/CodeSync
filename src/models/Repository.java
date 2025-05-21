package models;

import lombok.*;

@Getter
@Setter

public class Repository {
    private int id;
    private String name;
    private String description;
    private String visibility;
    private String username;
    private double size;
    
    public Repository(int id, String name, String description, String visibility, String username, double size) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.username = username;
        this.size = size;
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