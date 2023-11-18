package client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

public class Main {
    private static Map<String, Object> _config = null;
    private static String _addr = null;

    public static void main(String[] args) {
        config();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine();

                switch (command) {
                    case "hello":
                        hello();
                        break;
                    case "check":
                        check();
                        break;
                    case "exit":
                        System.out.println("Exiting...");
                        System.exit(0);
                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        }
    }

    public static void hello() {
        try {
            // 创建URL对象
            URI uri = new URI(_addr + "/data/hello");
            URL url = uri.toURL();
            // 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置请求方法
            conn.setRequestMethod("GET");
            // 获取响应码
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 打印结果
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void check() {
        try {
            // 创建URL对象
            URI uri = new URI(_addr + "/info/check");
            URL url = uri.toURL();
            // 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置请求方法
            conn.setRequestMethod("GET");
            // 获取响应码
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 打印结果
            System.out.println(response.toString());
            String jsonString = response.toString();
            ObjectMapper mapper = new ObjectMapper();

            // 将JSON字符串转换为List
            List<String> urls = mapper.readValue(jsonString, List.class);

            // 遍历URL列表
            for (String urlStr : urls) {
                uri = new URI("http://" + urlStr + "/data/hello");
                url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int code = connection.getResponseCode();
                if (code == 200) {
                    System.out.println(urlStr + " is reachable.");
                } else {
                    System.out.println(urlStr + " is not reachable.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void config() {
        Yaml yaml = new Yaml();
        InputStream inputStream = Main.class
                .getClassLoader()
                .getResourceAsStream("application.yml");
        _config = yaml.load(inputStream);
        _addr = (String) _config.get("addr");
    }
}