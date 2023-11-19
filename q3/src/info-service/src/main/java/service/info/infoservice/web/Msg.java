package service.info.infoservice.web;

import java.io.Serializable;

public class Msg implements Serializable {
    private int index;
    private int size;
    private String uuid;
    private String sign;

    public Msg() {
    }

    public Msg(int index, String uuid, int size, String sign) {
        this.index = index;
        this.uuid = uuid;
        this.size = size;
        this.sign = sign;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "index=" + index +
                ", uuid=" + uuid +
                ", size=" + size +
                ", sign='" + sign +
                '}';
    }
}