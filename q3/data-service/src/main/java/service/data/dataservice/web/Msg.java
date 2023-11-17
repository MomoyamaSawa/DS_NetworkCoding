package service.data.dataservice.web;

import java.io.Serializable;

public class Msg implements Serializable {
    private int index;
    private String uuid;

    public Msg() {
    }

    public Msg(int index, String uuid) {
        this.index = index;
        this.uuid = uuid;
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

    @Override
    public String toString() {
        return "Msg{" +
                "index=" + index +
                ", String=" + uuid +
                '}';
    }
}