package service.data.dataservice.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data")
public class WebController {

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/up")
    public boolean up(@RequestParam String filename, @RequestParam String uuid, @RequestParam int index,
            @RequestParam byte[] data) {
        try {
            // 创建目录路径
            Path dirPath = Paths.get(serviceName, uuid);

            // 如果目录不存在，创建它
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 创建文件路径
            Path filePath = dirPath.resolve(String.valueOf(index));

            // 写入文件
            Files.write(filePath, data);

            // TODO 这边要告知info微服务

            return true;
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return false;
        }
    }

    @GetMapping("/down")
    public byte[] down(@RequestParam String uuid, @RequestParam int index) {
        try {
            // 创建文件路径
            Path filePath = Paths.get(serviceName, uuid, String.valueOf(index));

            // 读取文件
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return null;
        }
    }

}