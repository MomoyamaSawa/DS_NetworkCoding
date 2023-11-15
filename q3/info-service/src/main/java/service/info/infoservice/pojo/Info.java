package service.info.infoservice.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Info {
    @Id
    private String id;
    private String fileName;
    private String hash;
    private int blocksize;
    private int num;
    private int size;
    private int state;

    public Info(String id, String fileName, String hash, int blocksize, int num, int size, int state) {
        this.id = id;
        this.fileName = fileName;
        this.hash = hash;
        this.blocksize = blocksize;
        this.num = num;
        this.size = size;
        this.state = state;
    }

    public int getState() {
        return state;
    }
}