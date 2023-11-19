package test.client;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;

public class Main {
    private static Map<String, Object> _config = null;
    private static String _addr = null;
    private static String _host = null;
    private static Integer _port = null;
    private static OkHttpClient _client = new OkHttpClient.Builder().build();

    public static void main(String[] args) {
        // TODO 还要检查一下大文件的时候对面服务器崩了
        config();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter command: ");
                String input = scanner.nextLine();
                String[] parts = input.split(" "); // 分割输入为命令和参数
                String command = parts[0];
                String[] arguments = Arrays.copyOfRange(parts, 1, parts.length); // 如果存在参数，获取参数

                switch (command) {
                    case "hello":
                        hello();
                        break;
                    case "checkNetwork":
                        checkNetwork();
                        break;
                    case "checkFile":
                        printInfo(checkFile(arguments));
                        break;
                    case "put":
                        put(arguments);
                        break;
                    case "cput":
                        cput(arguments);
                        break;
                    case "get":
                        get(arguments);
                        break;
                    case "cget":
                        cget(arguments);
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

    public static void cput(String[] args) {
        long startTime = System.currentTimeMillis();
        // cput D:\Study\DistributedSystem\test\test2\test big.test
        try {
            String filepath = args[0];
            String filename = args[1];
            String[] arg = new String[1];
            arg[0] = filename;
            List<Map<String, Object>> list = checkFile(arg);
            List<Integer> nums = list.stream()
                    .map(chunk -> (int) chunk.get("num"))
                    .collect(Collectors.toList());

            long fileSize = Files.size(Paths.get(filepath)); // 获取文件大小
            int chunkSize = 1024 * 1024; // 块大小为1024KB
            int chunks = (int) Math.ceil((double) fileSize / chunkSize); // 计算块数量

            List<byte[]> fileChunks = new ArrayList<>(chunks); // 创建一个列表来保存所有的块

            try (RandomAccessFile file = new RandomAccessFile(filepath, "r")) {
                for (int i = 0; i < chunks; i++) {
                    int bufferSize = i < chunks - 1 ? chunkSize : (int) (fileSize - chunkSize * i);
                    byte[] buffer = new byte[bufferSize];
                    file.read(buffer); // 读取一块
                    fileChunks.add(buffer); // 将这块添加到列表中
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
                e.printStackTrace();
            }

            if (list.size() == chunks) {
                System.out.println("File already exists. Skipping...");
                return;
            }

            String uuid = null;
            if (list.size() == 0) {
                HttpUrl url = new HttpUrl.Builder()
                        .scheme("http")
                        .host(_host)
                        .port(_port)
                        .addPathSegment("info")
                        .addPathSegment("up")
                        .addQueryParameter("filename", filename)
                        .addQueryParameter("hash", "hashtest")
                        .addQueryParameter("blocksize", String.valueOf(chunkSize))
                        .addQueryParameter("num", String.valueOf(chunks))
                        .addQueryParameter("size", String.valueOf(fileSize))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = _client.newCall(request).execute();
                ObjectMapper mapper = new ObjectMapper();
                String responseBody = response.body().string();
                Map<String, Object> map = mapper.readValue(responseBody, Map.class);
                uuid = (String) map.get("uuid");
            } else {
                uuid = getUUID(filename);
            }

            // 创建一个固定大小的线程池
            ExecutorService executor = Executors.newFixedThreadPool(24);
            // 创建一个列表来保存所有的Future对象
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < fileChunks.size(); i++) {
                if (i == 50) {
                    long endTime = System.currentTimeMillis();
                    long timeElapsed = endTime - startTime;
                    System.out.println("Execution time in milliseconds: " + timeElapsed);
                    System.out.println("暂停");
                }
                // 如果已经存在，跳过
                if (nums.contains(i)) {
                    System.out.println("Chunk " + i + " already exists. Skipping...");
                    continue;
                }
                byte[] chunk = fileChunks.get(i);
                int finalI = i;
                String uuidString = uuid;
                // 创建一个新的异步任务
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        RequestBody requestBody = RequestBody.create(chunk,
                                MediaType.parse("application/octet-stream"));
                        HttpUrl _url = new HttpUrl.Builder()
                                .scheme("http")
                                .host(_host)
                                .port(_port)
                                .addPathSegment("data")
                                .addPathSegment("up")
                                .addQueryParameter("filename", filename)
                                .addQueryParameter("uuid", uuidString)
                                .addQueryParameter("index", String.valueOf(finalI))
                                .build();
                        Request _request = new Request.Builder()
                                .url(_url)
                                .post(requestBody)
                                .build();
                        try (Response response = _client.newCall(_request).execute()) {
                            if (!response.isSuccessful()) {
                                throw new Exception("Unexpected code " + response);
                            }
                            System.out.println("Chunk " + finalI + "uploaded");
                        }
                    } catch (Exception e) {
                        System.out.println("Error type: " + e.getClass().getName());
                        System.out.println(String.valueOf(finalI) + "of" + String.valueOf(chunks) + "failed");
                    }
                }, executor);
                // 将Future对象添加到列表中
                futures.add(future);
            }
            // 等待所有的异步任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            // 关闭线程池
            executor.shutdown();
            long endTime = System.currentTimeMillis();
            long timeElapsed = endTime - startTime;
            System.out.println("Execution time in milliseconds: " + timeElapsed);
        } catch (Exception e) {

        }
    }

    public static void cget(String[] args) {
        long startTime = System.currentTimeMillis();
        // cget big.test D:\Study\DistributedSystem\test\test2\ big
        try {
            String filename = args[0];
            String filedir = args[1];
            String file = args[2];
            String uuid = getUUID(filename);
            String[] arg = new String[1];
            arg[0] = filename;
            List<Map<String, Object>> list = checkFile(arg);
            // 按照分块序号排序
            list.sort(Comparator.comparing(map -> (int) map.get("num")));
            // 创建一个OkHttpClient实例
            OkHttpClient client = new OkHttpClient();
            // 创建一个固定大小的线程池
            ExecutorService executor = Executors.newFixedThreadPool(24);
            // 创建一个列表来保存所有的Future对象
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Map<String, Object> chunk : list) {
                String sign = chunk.get("sign").toString();
                String[] parts = sign.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                int index = (int) chunk.get("num");
                if (index == 50) {
                    long endTime = System.currentTimeMillis();
                    long timeElapsed = endTime - startTime;
                    System.out.println("Execution time in milliseconds: " + timeElapsed);
                    System.out.println("暂停");
                }
                // 创建一个新的异步任务
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Path tempFile = Paths.get(filedir, "temp" + uuid + index + ".dat");
                        // 如果临时文件已经存在，那么跳过下载和写入操作
                        if (Files.exists(tempFile)) {
                            System.out.println("Chunk " + index + " already exists. Skipping...");
                            return;
                        }
                        HttpUrl url = new HttpUrl.Builder()
                                .scheme("http")
                                .host(host)
                                .port(port)
                                .addPathSegment("data")
                                .addPathSegment("down")
                                .addQueryParameter("uuid", uuid)
                                .addQueryParameter("index", Integer.toString(index))
                                .build();
                        Request request = new Request.Builder().url(url).build();
                        try (Response response = client.newCall(request).execute()) {
                            if (!response.isSuccessful())
                                throw new IOException("Unexpected code " + response);
                            // 获取数据块的内容
                            byte[] content = Objects.requireNonNull(response.body()).bytes();
                            // 将数据块的内容写入到临时文件中
                            Files.write(tempFile, content);
                            System.out.println("Chunk " + index + " downloaded");
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to download chunk " + index + ": " + e.getMessage());
                    }
                }, executor);
                // 将Future对象添加到列表中
                futures.add(future);
            }
            // 等待所有的异步任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            // 关闭线程池
            executor.shutdown();
            // 检查所有的临时文件是否都存在
            boolean allFilesExist = IntStream.range(0, list.size())
                    .allMatch(i -> Files.exists(Paths.get(filedir, "temp" + uuid + i + ".dat")));
            if (allFilesExist) {
                // 合并临时文件
                try (FileOutputStream fos = new FileOutputStream(filedir + file)) {
                    for (int i = 0; i < list.size(); i++) {
                        Path tempFile = Paths.get(filedir, "temp" + uuid + i + ".dat");
                        byte[] content = Files.readAllBytes(tempFile);
                        fos.write(content);
                        // 删除临时文件
                        Files.delete(tempFile);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to merge temporary files: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Some temporary files are missing, skipping merge operation.");
            }
        } catch (Exception e) {
            System.out.println("Error type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        Long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in milliseconds: " + timeElapsed);
    }

    public static void get(String[] args) {
        long startTime = System.currentTimeMillis();
        // get demo.test D:\Study\DistributedSystem\test\test2 demo
        try {
            String filename = args[0];
            String filedir = args[1];
            String file = args[2];
            String uuid = getUUID(filename);
            String[] arg = new String[1];
            arg[0] = filename;
            List<Map<String, Object>> list = checkFile(arg);

            // 按照分块序号排序
            list.sort(Comparator.comparing(map -> (int) map.get("num")));

            // 创建一个OkHttpClient实例
            OkHttpClient client = new OkHttpClient();

            // 创建一个固定大小的线程池
            ExecutorService executor = Executors.newFixedThreadPool(24);

            // 创建一个列表来保存所有的Future对象
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Map<String, Object> chunk : list) {
                String sign = chunk.get("sign").toString();
                String[] parts = sign.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                int index = (int) chunk.get("num");
                Path tempFile = Paths.get(filedir, "temp" + uuid + index + ".dat");
                // 创建一个新的异步任务
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        HttpUrl url = new HttpUrl.Builder()
                                .scheme("http")
                                .host(host)
                                .port(port)
                                .addPathSegment("data")
                                .addPathSegment("down")
                                .addQueryParameter("uuid", uuid)
                                .addQueryParameter("index", Integer.toString(index))
                                .build();
                        Request request = new Request.Builder().url(url).build();
                        try (Response response = client.newCall(request).execute()) {
                            if (!response.isSuccessful())
                                throw new IOException("Unexpected code " + response);
                            // 获取数据块的内容
                            byte[] content = Objects.requireNonNull(response.body()).bytes();
                            // 将数据块的内容写入到临时文件中
                            Files.write(tempFile, content);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                // 将Future对象添加到列表中
                futures.add(future);
            }
            // 等待所有的异步任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            // 关闭线程池
            executor.shutdown();
            // 合并临时文件
            try (FileOutputStream fos = new FileOutputStream(filedir + "\\" + file)) {
                for (int i = 0; i < list.size(); i++) {
                    Path tempFile = Paths.get(filedir, "temp" + uuid + i + ".dat");
                    byte[] content = Files.readAllBytes(tempFile);
                    fos.write(content);
                    // 删除临时文件
                    Files.delete(tempFile);
                }
            }
        } catch (Exception e) {
            System.out.println("Error type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in milliseconds: " + timeElapsed);
    }

    public static String getUUID(String filename) {
        try {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(_host)
                    .port(_port)
                    .addPathSegment("info")
                    .addPathSegment("id")
                    .addQueryParameter("filename", filename)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = _client.newCall(request).execute();
            String responseBody = response.body().string();
            return responseBody;

        } catch (Exception e) {
            System.out.println("Error type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, Object>> checkFile(String[] args) {
        // checkFile photo.test
        String filename = args[0];
        String uuid = getUUID(filename);
        try {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(_host)
                    .port(_port)
                    .addPathSegment("info")
                    .addPathSegment("info")
                    .addQueryParameter("uuid", uuid)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = _client.newCall(request).execute();
            String responseBody = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, List.class);

        } catch (Exception e) {
            System.out.println("Error type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static void printInfo(List<Map<String, Object>> list) {
        // 统计每个服务器下的数据块数量
        Map<String, Long> serverCounts = list.stream()
                .collect(Collectors.groupingBy(map -> (String) map.get("sign"), Collectors.counting()));

        // 输出每个服务器下的数据块数量
        serverCounts.forEach((server, count) -> System.out.println("服务器：" + server + "; 数据块数量：" + count));

        // 按照分块序号排序并输出每个数据块的信息
        list.stream()
                .sorted(Comparator.comparing(map -> (int) map.get("num")))
                .forEach(map -> System.out.println(
                        "分块序号：" + map.get("num") + "; 分块大小：" + map.get("size") + "B; 所在服务器：" + map.get("sign")));
    }

    public static void put(String[] args) {
        // put D:\Study\DistributedSystem\test\test2\test demo.test
        long startTime = System.currentTimeMillis();
        try {
            String filepath = args[0];
            String filename = args[1];

            long fileSize = Files.size(Paths.get(filepath)); // 获取文件大小
            int chunkSize = 1024 * 1024; // 块大小为1024KB
            int chunks = (int) Math.ceil((double) fileSize / chunkSize); // 计算块数量

            List<byte[]> fileChunks = new ArrayList<>(chunks); // 创建一个列表来保存所有的块

            try (RandomAccessFile file = new RandomAccessFile(filepath, "r")) {
                for (int i = 0; i < chunks; i++) {
                    int bufferSize = i < chunks - 1 ? chunkSize : (int) (fileSize - chunkSize * i);
                    byte[] buffer = new byte[bufferSize];
                    file.read(buffer); // 读取一块
                    fileChunks.add(buffer); // 将这块添加到列表中
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
                e.printStackTrace();
            }
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(_host)
                    .port(_port)
                    .addPathSegment("info")
                    .addPathSegment("up")
                    .addQueryParameter("filename", filename)
                    .addQueryParameter("hash", "hashtest")
                    .addQueryParameter("blocksize", String.valueOf(chunkSize))
                    .addQueryParameter("num", String.valueOf(chunks))
                    .addQueryParameter("size", String.valueOf(fileSize))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = _client.newCall(request).execute();
            ObjectMapper mapper = new ObjectMapper();
            String responseBody = response.body().string();
            Map<String, Object> map = mapper.readValue(responseBody, Map.class);
            String uuid = (String) map.get("uuid");

            // 创建一个固定大小的线程池
            ExecutorService executor = Executors.newFixedThreadPool(24);
            // 创建一个列表来保存所有的Future对象
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < fileChunks.size(); i++) {
                byte[] chunk = fileChunks.get(i);
                int finalI = i;
                // 创建一个新的异步任务
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        RequestBody requestBody = RequestBody.create(chunk,
                                MediaType.parse("application/octet-stream"));
                        HttpUrl _url = new HttpUrl.Builder()
                                .scheme("http")
                                .host(_host)
                                .port(_port)
                                .addPathSegment("data")
                                .addPathSegment("up")
                                .addQueryParameter("filename", filename)
                                .addQueryParameter("uuid", uuid)
                                .addQueryParameter("index", String.valueOf(finalI))
                                .build();
                        Request _request = new Request.Builder()
                                .url(_url)
                                .post(requestBody)
                                .build();
                        try (Response _response = _client.newCall(_request).execute()) {
                            // 使用响应
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                // 将Future对象添加到列表中
                futures.add(future);
            }
            // 等待所有的异步任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            // 关闭线程池
            executor.shutdown();
        } catch (Exception e) {
            System.out.println("Error type: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in milliseconds: " + timeElapsed);
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

    public static void checkNetwork() {
        try {
            OkHttpClient client = new OkHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            // 创建URL对象
            HttpUrl _url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(_host)
                    .port(_port)
                    .addPathSegment("info")
                    .addPathSegment("check")
                    .build();
            Request request = new Request.Builder().url(_url).build();

            // 发送请求并获取响应
            try (Response response = client.newCall(request).execute()) {
                // 打印响应码
                System.out.println("Response Code : " + response.code());

                // 读取响应内容
                String jsonString = response.body().string();
                System.out.println(jsonString);

                // 将JSON字符串转换为List
                List<String> urls = mapper.readValue(jsonString, List.class);

                // 遍历URL列表
                for (String urlStr : urls) {
                    URI uri = new URI("http://" + urlStr + "/data/hello");
                    request = new Request.Builder().url(uri.toURL()).build();

                    try (Response urlResponse = client.newCall(request).execute()) {
                        if (urlResponse.isSuccessful()) {
                            System.out.println(urlStr + " is reachable.");
                        } else {
                            System.out.println(urlStr + " is not reachable.");
                        }
                    }
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
        _host = (String) _config.get("host");
        _port = (Integer) _config.get("port");
    }
}