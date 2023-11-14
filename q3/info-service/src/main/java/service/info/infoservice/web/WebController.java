package service.info.infoservice.web;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.info.infoservice.service.Service;
import service.info.infoservice.pojo.Info;

@RestController
@RequestMapping("/info")
public class WebController {

    @Autowired
    private Service service;

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
            // 返回一个包含自定义状态码和响应体的ResponseEntity
            body.put("message", "文件已存在");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
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
}