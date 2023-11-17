package service.data.dataservice.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data")
public class WebController {

    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${server.port}")
    private int port;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @Autowired
    private SendMsg sendMsg;

    @PostMapping("/up")
    public boolean up(@RequestParam String filename, @RequestParam String uuid, @RequestParam int index,
            @RequestBody byte[] data) {
        try {
            String dir = serviceName + String.valueOf(port);
            // 创建目录路径
            Path dirPath = Paths.get(dir, uuid);

            // 如果目录不存在，创建它
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 输出绝对路径
            System.out.println("Directory absolute path: " + dirPath.toAbsolutePath().toString());

            // 创建文件路径
            Path filePath = dirPath.resolve(String.valueOf(index));

            // 写入文件
            Files.write(filePath, data);

            // 这边告知info微服务
            sendMsg.send(new Msg(index, uuid));

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
            String dir = serviceName + String.valueOf(port);
            // 创建文件路径
            Path filePath = Paths.get(dir, uuid, String.valueOf(index));

            // 获取并打印绝对路径
            System.out.println("Absolute path: " + filePath.toAbsolutePath().toString());

            // 读取文件
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return null;
        }
    }

}