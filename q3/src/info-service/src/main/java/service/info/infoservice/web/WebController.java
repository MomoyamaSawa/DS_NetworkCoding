package service.info.infoservice.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.info.infoservice.service.Service;
import service.info.infoservice.pojo.Data;
import service.info.infoservice.pojo.Info;

@RestController
@RequestMapping("/info")
public class WebController {

    @Autowired
    private Service service;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/up")
    public ResponseEntity<Map<String, Object>> up(@RequestParam String filename,
            @RequestParam String hash,
            @RequestParam int blocksize,
            @RequestParam int num,
            @RequestParam int size) {

        // 创建响应体
        Map<String, Object> body = new HashMap<>();
        // 检查文件名是否已经存在
        if (service.checkFileName(filename)) {
            String id = service.getIdByFilename(filename);
            service.deleteInfo(id);
        }
        // 创建Info对象并设置其属性
        String uuid = UUID.randomUUID().toString();
        Info info = new Info(uuid, filename, hash, blocksize, num, size, 0);
        service.addInfo(info);
        // 创建响应体
        body.put("message", "success");
        body.put("uuid", uuid);

        // 返回一个包含自定义状态码和响应体的ResponseEntity
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @GetMapping("/check")
    public List<String> getDataserviceInstances() {
        return discoveryClient.getInstances("dataService").stream()
                .map(instance -> instance.getHost() + ":" + instance.getPort())
                .collect(Collectors.toList());
    }

    @GetMapping("/info")
    public List<Data> getDataInfos(@RequestParam String uuid) {
        return service.getDataInfos(uuid);
    }

    @GetMapping("/id")
    public String getId(@RequestParam String filename) {
        return service.getIdByFilename(filename);
    }

    @GetMapping("/delete")
    public boolean delete(@RequestParam String uuid) {
        List<Data> dataInfos = service.getDataInfos(uuid);
        for (Data dataInfo : dataInfos) {
            service.deleteData(dataInfo.getId());
        }
        service.deleteInfo(uuid);
        return true;
    }

    @GetMapping("/download")
    public List<Data> download(@RequestParam String uuid) {
        Optional<Info> infoOptional = service.findInfo(uuid);
        if (!infoOptional.isPresent()) {
            return null;
        }
        // TODO 没写检查状态码的代码
        return service.getDataInfos(uuid);
    }

    @GetMapping("/upcheck")
    public Info upcheck(@RequestParam String uuid) {
        // TODO 没检查hash之类的
        Optional<Info> infoOptional = service.findInfo(uuid);
        if (!infoOptional.isPresent()) {
            return null;
        }
        Info info = infoOptional.get();
        return info;
    }

}