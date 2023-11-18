package service.info.infoservice.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Data {
    @Id
    private String id;
    private int num;
    private String sign;
    private int size;
    private String fileid;

    // 空构造函数
    public Data() {
    }

    // 包含所有字段的构造函数
    public Data(String id, int num, String sign, int size, String fileid) {
        this.id = id;
        this.num = num;
        this.sign = sign;
        this.size = size;
        this.fileid = fileid;
    }

    public String getID() {
        return id;
    }
}