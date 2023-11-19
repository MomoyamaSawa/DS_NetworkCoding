package service.info.infoservice.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Info {
    @Id
    private String id;
    @Column(unique = true)
    private String filename;
    private String hash;
    private int blocksize;
    private int num;
    private long size; // 修改这里
    private int state;

    public Info(String id, String fileName, String hash, int blocksize, int num, long size, int state) { // 修改这里
        this.id = id;
        this.filename = fileName;
        this.hash = hash;
        this.blocksize = blocksize;
        this.num = num;
        this.size = size; // 修改这里
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

    public long getSize() { // 修改这里
        return size;
    }

    public int getState() {
        return state;
    }

    public void incrementNum() {
        this.state += 1;
    }
}