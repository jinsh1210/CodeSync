package utils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

import models.FileInfo;

public class ClientSock {
    private static final String SERVERIP = "sjc07250.iptime.org";
    private static final int PORT = 9969;
    private static final String VERISION = "v0.0.1b";
    private static final String TOKEN = "fjk123#%k2!lsd!234!%^^f17!@#sdfs!@$3$*s1s56!@#";
    private static final String CONFIG_PATH = "config.json";
    private static JSONObject config;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static InputStream inputStream;

    private static void loadConfig() {
        File configFile = new File("TeamProject/" + CONFIG_PATH);
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("[]"); // 빈 JSON 배열로 초기화
                }
                System.out.println("[config.json] 설정 파일이 없어서 새로 생성했습니다.");
            } catch (IOException e) {
                System.err.println("[config.json] 파일 생성 실패");
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(configFile)) {
            char[] buffer = new char[(int) configFile.length()];
            reader.read(buffer);
            // 기존 구조에 맞게, 배열을 감싸는 객체로 변환
            config = new JSONObject("{\"entries\":" + new String(buffer) + "}");
            System.out.println("[config.json] 설정 파일 로드 완료");
        } catch (IOException e) {
            System.err.println("[config.json] 로드 실패");
            e.printStackTrace();
        }
    }

    public static void connect() {
        try {
            socket = new Socket(SERVERIP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            inputStream = socket.getInputStream();

            // 초기 인증
            out.println(TOKEN);
            out.println(VERISION);

            try {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("[Client] 서버에 연결되었습니다.");
        } catch (IOException e) {
            System.err.println("[Client] 서버 연결 실패");
            e.printStackTrace();
        }
    }

    public static void sendCommand(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    private static void receiveFile(String headerLine, File baseFolder, JProgressBar bar,String repoName) throws IOException {
        String[] tokens = headerLine.substring(13).trim().split(" ");
        if (tokens.length < 2)
            throw new IOException("pull_file 명령어 파싱 실패: " + headerLine);

        int size = Integer.parseInt(tokens[tokens.length - 1]);
        String filePath = String.join(" ", Arrays.copyOf(tokens, tokens.length - 1));
        if (filePath.startsWith(repoName + "/")) 
            filePath = filePath.replaceFirst(Pattern.quote(repoName + "/"), "");

        File targetFile = new File(baseFolder, filePath);
        File parent = targetFile.getParentFile();
        if (!parent.exists())
            parent.mkdirs();

        System.out.println("[파일 수신 시작]: " + filePath + " (" + size + " bytes)");

        bar.setVisible(true);
        SwingUtilities.invokeLater(() -> bar.setValue(0));
        try (FileOutputStream fileOut = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int offset = 0;
            int read;
            while (offset < size) {
                read = inputStream.read(buffer, 0, Math.min(buffer.length, size - offset));
                if (read == -1)
                    throw new EOFException("파일 수신 도중 스트림 종료됨: " + filePath);

                fileOut.write(buffer, 0, read);
                offset += read;

                int percent = (int) (100.0 * offset / size);
                SwingUtilities.invokeLater(() -> bar.setValue(percent));
            }
        }

        SwingUtilities.invokeLater(() -> bar.setValue(100));
        System.out.println("[파일 저장 완료]: " + targetFile.getPath());
    }

    private static void handleSingleFilePull(String header, File baseFolder, JProgressBar bar,String repoName) throws IOException {
        receiveFile(header, baseFolder, bar,repoName);
    }

    private static void handleDirectoryPull(BufferedReader reader, File baseFolder, JProgressBar bar,String repoName)
            throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line.startsWith("/#/"))
                System.out.println("[명령어 수신]: " + line);

            if (line.equals("/#/pull_dir_EOL")) {
                System.out.println("[디렉토리 수신 종료]");
                break;
            }

            if (line.startsWith("/#/pull_dir ")) {
                System.out.println("디렉토리 생성요청");
                String dirPath = line.substring(12).trim();
                if (dirPath.startsWith(repoName + "/")) {
                    dirPath = dirPath.replaceFirst(Pattern.quote(repoName + "/"), "");
                }
                File dir = new File(baseFolder, dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                    System.out.println("[디렉토리 생성]: " + dir.getPath());
                }
                continue;
            }
            if (line.startsWith("/#/pull_file ")) {
                sendCommand("/ACK");
                receiveFile(line, baseFolder, bar,repoName);
            }
        }
    }

    public static void pull(String repoName, String relPath, File targetFolder, String Owner, JProgressBar bar) {
        try {
            // 우선 서버에 요청 보냄
            sendCommand("/pull " + repoName + " \"" + relPath + "\" " + Owner);
            System.out.println("/pull " + repoName + " \"" + relPath + "\" " + Owner);//디버그
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line = reader.readLine();
            if (line == null) {
                System.err.println("[오류] 서버 응답 없음");
                return;
            }

            if (line.startsWith("/#/pull_error")) {
                System.err.println("[서버 오류]: " + line);
                return;
            }
            if (line.startsWith("/#/pull_file ")) {
                handleSingleFilePull(line, targetFolder, bar,repoName);
            } else if (line.equals("/#/pull_dir_SOL")) {
                handleDirectoryPull(reader, targetFolder, bar,repoName);
            } else {
                System.err.println("[오류] 알 수 없는 응답: " + line);
            }

        } catch (Exception e) {
            System.err.println("[pull 예외 발생]");
            e.printStackTrace();
        } finally {
            System.out.println("[pull 완료]");
        }
    }

    public static void push(File file, String repoName, int userId, String serverPath, String Owner, JProgressBar bar)
            throws SocketException {
        try {
            long fileSize = file.length(); // 파일 크기 측정

            sendCommand("/push " + repoName + " \"" + serverPath + "\" " + fileSize + " " + Owner);
            System.out.println("/push " + repoName + " \"" + serverPath + "\" " + fileSize + " " + Owner); // 디버그
            System.out.println("owner: " + Owner);
            String response = receiveResponse();
            if (!"/#/push_ready".equals(response.trim())) {
                System.err.println("[Client] push 실패: 서버 응답 = " + response);
                return;
            }
            bar.setVisible(true);
            SwingUtilities.invokeLater(() -> bar.setValue(0));
            // 스트림 방식으로 전송
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                long sent = 0;
                int read;
                OutputStream out = socket.getOutputStream();

                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    sent += read;
                    int percent = (int) (100 * sent / fileSize);
                    SwingUtilities.invokeLater(() -> bar.setValue(percent));
                }
                out.flush();
            }
            SwingUtilities.invokeLater(() -> bar.setValue(100));
            // 결과 수신
            String result = receiveResponse();
            System.out.println("[서버 응답] " + result);
            sendCommand("/ACK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void push(File folder, String basePath, String repository, int userId, String Owner, JProgressBar bar)
            throws IOException { // 폴더 형식 전송 오버로드 메소드
        File[] contents = folder.listFiles();
        boolean isEmpty = (contents == null || contents.length == 0);

        // 상대 경로 계산
        String relativePath = (basePath.isEmpty() ? folder.getName() : basePath + "/" + folder.getName());
        System.out.println("폴더 전용 relativePath: " + relativePath + " | basePath: " + basePath + " | owner: " + Owner); // 디버그
        if (relativePath.startsWith(repository + "/")) {
            relativePath = relativePath.substring(repository.length() + 1);
        } else if (relativePath.equals(repository)) {
            relativePath = "";  // repoName만 있을 경우 완전 제거
        } 
        // 빈 폴더면 mkdir 명령어 전송
        if (isEmpty) {

            sendCommand("/mkdir " + repository + " \"" + relativePath + "\" " + Owner);
            
            System.out.println("owner: " + Owner);
            System.out.println("/mkdir " + repository + " \"" + relativePath + "\" " + Owner);// 디버그
            String response = receiveResponse();
            System.out.println("[서버 mkdir 응답] " + response);
            return;
        }

        // 폴더 안에 내용이 있을 경우
        for (File file : contents) {
            if (file.isFile()) {
                String filePath = relativePath.equals("")? file.getName():relativePath + "/" + file.getName();
                push(file, repository, userId, filePath, Owner, bar);
            } else if (file.isDirectory()) {
                push(file, relativePath, repository, userId, Owner, bar); // 재귀 호출
            }
        }
    }

    public static String receiveResponse() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) {
                return new String(buffer, 0, bytesRead, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void startReceiver() {
        Thread receiveThread = new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("[서버 응답] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    public static List<FileInfo> receiveFileList(int repositoryId) {
        List<FileInfo> files = new ArrayList<>();
        try {
            sendCommand("LIST_FILES|" + repositoryId);
            String response = receiveResponse();

            // JSON 파싱 필요 시 여기에 파싱 로직 작성
            System.out.println("[파일 목록 수신]: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public static void disconnect() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPath(String user, String repoName, String path) {
        loadConfig(); // Ensure config is loaded

        boolean updated = false;
        for (int i = 0; i < config.getJSONArray("entries").length(); i++) {
            org.json.JSONObject obj = config.getJSONArray("entries").getJSONObject(i);
            if (obj.getString("user").equals(user) && obj.getString("repoName").equals(repoName)) {
                File configDir = new File(path);
                if (!configDir.exists()) {
                    configDir.mkdirs(); // 디렉토리 없으면 생성
                }
                obj.put("path", path); // Update path
                updated = true;
                break;
            }
        }

        if (!updated) {
            org.json.JSONObject newEntry = new org.json.JSONObject();
            File configDir = new File(path);
            if (!configDir.exists()) {
                configDir.mkdirs(); // 디렉토리 없으면 생성
            }

            newEntry.put("user", user);
            newEntry.put("repoName", repoName);
            newEntry.put("path", path);
            config.getJSONArray("entries").put(newEntry);
        }

        // Save back to file
        try (FileWriter writer = new FileWriter("TeamProject/" + CONFIG_PATH)) {
            writer.write(config.getJSONArray("entries").toString(2)); // pretty print
            System.out.println("[config.json] 저장 완료");
        } catch (IOException e) {
            System.err.println("[config.json] 저장 실패");
            e.printStackTrace();
        }
    }

    public static String getPath(String user, String repoName) {
        loadConfig(); // Ensure config is loaded

        for (int i = 0; i < config.getJSONArray("entries").length(); i++) {
            org.json.JSONObject obj = config.getJSONArray("entries").getJSONObject(i);
            if (obj.getString("user").equals(user) && obj.getString("repoName").equals(repoName)) {
                return obj.getString("path");
            }
        }

        return null; // Not found
    }
}
