package service.info.infoservice.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Data {
    @Id
    private String id;
    private int index;
    private String sign;
    private int size;
    private String fileid;

    public String getID() {
        return id;
    }
}