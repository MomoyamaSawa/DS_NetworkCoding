package service.info.infoservice.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Info {
    @Id
    private String id;
    private String filename;
    private String hash;
    private int blocksize;
    private int num;
    private int size;
    private int state;

    public Info(String id, String fileName, String hash, int blocksize, int num, int size, int state) {
        this.id = id;
        this.filename = fileName;
        this.hash = hash;
        this.blocksize = blocksize;
        this.num = num;
        this.size = size;
        this.state = state;
    }

    public Info() {
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getHash() {
        return hash;
    }

    public int getBlocksize() {
        return blocksize;
    }

    public int getNum() {
        return num;
    }

    public int getSize() {
        return size;
    }

    public int getState() {
        return state;
    }
}