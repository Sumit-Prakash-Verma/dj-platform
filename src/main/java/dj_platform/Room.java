package dj_platform;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "music_rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String genre;
    private int listeners;

    // JPA ke liye no-argument constructor zaroori hai
    public Room() {
    }

    public Room(String name, String genre, int listeners) {
        this.name = name;
        this.genre = genre;
        this.listeners = listeners;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getListeners() {
        return listeners;
    }

    public void setListeners(int listeners) {
        this.listeners = listeners;
    }
}