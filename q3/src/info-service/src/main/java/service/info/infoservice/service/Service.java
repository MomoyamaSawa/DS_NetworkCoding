package service.info.infoservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import service.info.infoservice.pojo.Data;
import service.info.infoservice.pojo.Info;
import service.info.infoservice.repository.DataRepository;
import service.info.infoservice.repository.InfoRepository;

import java.util.List;
import java.util.Optional;

@Component
public class Service {
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private InfoRepository infoRepository;

    public String getIdByFilename(String filename) {
        Info info = infoRepository.findByFilename(filename);
        return info != null ? info.getId() : null;
    }

    // 检查文件名字是否存在
    public boolean checkFileName(String fileName) {
        return infoRepository.existsByFilename(fileName);
    }

    // 添加Data
    public Data addData(Data data) {
        return dataRepository.save(data);
    }

    // 删除Data
    public void deleteData(String dataKey) {
        dataRepository.deleteById(dataKey);
    }

    // 查询Data
    public Optional<Data> findData(String dataKey) {
        return dataRepository.findById(dataKey);
    }

    // 添加Info
    public Info addInfo(Info info) {
        return infoRepository.save(info);
    }

    // 删除Info
    public void deleteInfo(String id) {
        infoRepository.deleteById(id);
    }

    // 查询Info
    public Optional<Info> findInfo(String id) {
        return infoRepository.findById(id);
    }

    public List<Data> getDataInfos(String uuid) {
        return dataRepository.findByFileid(uuid);
    }

    public boolean incrementNum(String id) {
        // 从数据库中获取Info实例
        Optional<Info> option = infoRepository.findById(id);
        Info info = null;
        if (option.isPresent()) {
            info = option.get();
            // 增加num的值
            info.incrementNum();
            // 保存Info实例
            infoRepository.save(info);
            return true;
        } else {
            return false;
        }
    }

    // 其他方法...
}