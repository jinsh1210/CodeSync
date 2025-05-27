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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClientSock {
    private static final String SERVERIP = "sjc07250.iptime.org";
    private static final int PORT = 9969;
    private static final String VERISION = "v0.0.1b";
    private static final String TOKEN = "fjk123#%k2!lsd!234!%^^f17!@#sdfs!@$3$*s1s56!@#";
    private static final String CONFIG_PATH = "config.json";
    private static JSONArray configEntries;

    public static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static InputStream inputStream;
    private static String currentUser;

    public static void setUser(String user){
        currentUser=user;
    }

    private static void loadConfig() {
        File configFile = new File("TeamProject/" + CONFIG_PATH);
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("[]"); // 빈 JSON 배열로 초기화
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(configFile)) {
            char[] buffer = new char[(int) configFile.length()];
            reader.read(buffer);
            String rawJson = new String(buffer);
            try {
                configEntries = new JSONArray(rawJson);
            } catch (org.json.JSONException e) {
                System.err.println("[loadConfig] JSON 파싱 실패: config.json 초기화");
                e.printStackTrace();
                configEntries = new JSONArray();
            }
        } catch (IOException e) {
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

    // Owner 인자를 추가하여 frozen 파일 처리 지원
    private static void receiveFile(String headerLine, File baseFolder, JProgressBar bar, String repoName, String Owner) throws IOException {
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

        // 프리징된 파일인지 확인
        Set<String> frozenPaths = getFrozenPaths(currentUser, repoName, Owner);
        boolean isFrozen = frozenPaths.contains(targetFile.getAbsolutePath());

        bar.setVisible(true);
        SwingUtilities.invokeLater(() -> bar.setValue(0));
        try (OutputStream fileOut = isFrozen ? OutputStream.nullOutputStream() : new FileOutputStream(targetFile)) {
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
        if (isFrozen) {
            System.out.println("[프리징됨 - 무시했지만 데이터는 수신함]: " + filePath);
        }
        return;
    }

    private static void handleSingleFilePull(String header, File baseFolder, JProgressBar bar, String repoName, String Owner) throws IOException {
        receiveFile(header, baseFolder, bar, repoName, Owner);
    }

    private static void handleDirectoryPull(BufferedReader reader, File baseFolder, JProgressBar bar, String repoName, String Owner)
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
                receiveFile(line, baseFolder, bar, repoName, Owner);
            }
        }
    }
    
    public static void pull(String repoName, String relPath, File targetFolder, String Owner, JProgressBar bar, JSONArray pathAll) {
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
            System.out.println(line);
            StringBuilder jsonBuilder = new StringBuilder();
            String hashLine = "";
            if (line.startsWith("/#/pull_hashes_SOL")) {
                System.out.println(hashLine);
                do {
                    hashLine += reader.readLine();
                    if (hashLine == null || hashLine.endsWith("/#/pull_hashes_EOL\n")) break;
                    jsonBuilder.append(hashLine);
                } while (hashLine.endsWith("/#/pull_hashes_EOL\n"));
                // --- [로컬에만 있는 파일/폴더 제거] ---
                // 서버 repo_content 결과(pathAll) 기준으로 로컬에만 있는 파일/폴더 제거
                String basePath = getPath(currentUser, repoName);
                if (basePath != null) {
                    Set<String> frozenPaths = getFrozenPaths(currentUser, repoName, Owner);
                    File localRoot = new File(basePath);
                    java.util.Set<String> serverPaths = new java.util.HashSet<>();
                    for (int i = 0; i < pathAll.length(); i++) {
                        serverPaths.add(pathAll.getJSONObject(i).getString("path"));
                    }
                    try {
                        Files.walk(localRoot.toPath())
                            .map(Path::toFile)
                            .filter(f -> {
                                if (f.getName().equals(".jsRepohashed.json")) return false;
                                String rel = localRoot.toPath().relativize(f.toPath()).toString().replace("\\", "/");
                                if (f.isDirectory()) rel += "/";
                                if (!serverPaths.contains(rel)) {
                                    return !frozenPaths.contains(f.getAbsolutePath()); // 프리징된 항목 제외
                                }
                                return false;
                            })
                            .sorted((a, b) -> b.getAbsolutePath().length() - a.getAbsolutePath().length())
                            .forEach(f -> {
                                if (f.delete()) {
                                    System.out.println("[로컬 제거됨] " + f.getPath());
                                }
                            });
                    } catch (Exception e) {
                        System.err.println("[로컬 파일/폴더 정리 중 오류]");
                        e.printStackTrace();
                    }
                }
                // --- [로컬에만 있는 파일/폴더 제거 끝] ---
            }
            line = reader.readLine();
            if (line != null && line.startsWith("/#/pull_file ")) {
                handleSingleFilePull(line, targetFolder, bar, repoName, Owner);
            } else if (line != null && line.equals("/#/pull_dir_SOL")) {
                handleDirectoryPull(reader, targetFolder, bar, repoName, Owner);
            } else if (line != null) {
                System.err.println("[오류] 알 수 없는 응답: " + line);
            }
            try {
                JSONArray serverHashList = new JSONArray(jsonBuilder.toString());
                // 기존 해시 목록 불러오기
                String localRepoPath = getPath(currentUser, repoName);
                File hashFile = new File(localRepoPath, ".jsRepohashed.json");
                java.util.Map<String, JSONObject> freezeMap = new java.util.HashMap<>();

                if (hashFile.exists()) {
                    JSONArray existingList = new JSONArray(Files.readString(hashFile.toPath()));
                    for (int i = 0; i < existingList.length(); i++) {
                        JSONObject obj = existingList.getJSONObject(i);
                        String path = obj.getString("path");
                        if (obj.optBoolean("freeze", false)) {
                            freezeMap.put(path, obj); // freeze == true인 항목은 전체 보존
                        }
                    }
                }

                // 서버 목록에 freeze/hash 값 보존
                for (int i = 0; i < serverHashList.length(); i++) {
                    JSONObject obj = serverHashList.getJSONObject(i);
                    String path = obj.getString("path");
                    if (freezeMap.containsKey(path)) {
                        JSONObject frozen = freezeMap.get(path);
                        obj.put("freeze", true);
                        obj.put("hash", frozen.getString("hash")); // 해시도 보존
                    } else {
                        obj.put("freeze", false);
                    }
                }

                saveHashSnapshot(currentUser, repoName, serverHashList);
            } catch (Exception e) {
                System.err.println("[클라이언트] 해시 JSON 파싱 오류");
                e.printStackTrace();
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

    public static void push(File folder, String basePath, String repository, int userId, String Owner, JProgressBar bar,JSONArray pathall)
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
            return;
        }
        
        // --- Get frozenPaths before file processing loop ---
        Set<String> frozenPaths = getFrozenPaths(currentUser, repository, Owner);

        // 폴더 안에 내용이 있을 경우
        for (File file : contents) {
            // Skip .jsRepohashed.json file
            if (file.getName().equals(".jsRepohashed.json")) continue;
            if(file.getName().equals(".DS_Store")) continue;
            if (file.isFile()) {
                String filePath = relativePath.equals("") ? file.getName() : relativePath + "/" + file.getName();
                File absFile = new File(getPath(currentUser, repository), filePath);
                if (frozenPaths.contains(absFile.getAbsolutePath())) {
                    System.out.println("[프리징됨 - push 건너뜀] " + absFile.getAbsolutePath());
                    continue;
                }
                push(file, repository, userId, filePath, Owner, bar);
            } else if (file.isDirectory()) {
                push(file, relativePath, repository, userId, Owner, bar, pathall); // 재귀 호출
            }
        }

        List<String> deletedPaths = getDeletedPaths(pathall, currentUser, repository);
        for (String path : deletedPaths) {
            sendCommand("/delete_file " + repository + " \"" + path + "\" " + Owner);
            String response = receiveResponse();
            if (!response.startsWith("/#/delete_success")) {
                System.err.println("[삭제 실패] " + path + ": " + response);
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
        for (int i = 0; i < configEntries.length(); i++) {
            org.json.JSONObject obj = configEntries.getJSONObject(i);
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
            configEntries.put(newEntry);
        }

        // Save back to file
        try (FileWriter writer = new FileWriter("TeamProject/" + CONFIG_PATH)) {
            writer.write(configEntries.toString(2)); // pretty print
            System.out.println("[config.json] 저장 완료");
        } catch (IOException e) {
            System.err.println("[config.json] 저장 실패");
            e.printStackTrace();
        }
    }

    public static String getPath(String user, String repoName) {
        loadConfig(); // Ensure config is loaded
        for (int i = 0; i < configEntries.length(); i++) {
            org.json.JSONObject obj = configEntries.getJSONObject(i);
            if (obj.getString("user").equals(user) && obj.getString("repoName").equals(repoName)) {
                return obj.getString("path");
            }
        }
        return null; // Not found
    }
    // --- 해시 스냅샷 저장 메서드 ---
    public static void saveHashSnapshot(String user, String repoName, org.json.JSONArray hashList) {
        try {
            String localRepoPath = getPath(user, repoName);
            if (localRepoPath != null) {
                File hashFile = new File(localRepoPath, ".jsRepohashed.json");
                java.util.Map<String, JSONObject> freezeMap = new java.util.HashMap<>();

                // 이전 freeze 및 hash 값 보존
                if (hashFile.exists()) {
                    JSONArray existingList = new JSONArray(Files.readString(hashFile.toPath()));
                    for (int i = 0; i < existingList.length(); i++) {
                        JSONObject obj = existingList.getJSONObject(i);
                        String path = obj.getString("path");
                        if (obj.optBoolean("freeze", false)) {
                            freezeMap.put(path, obj); // freeze == true인 항목만 보존
                        }
                    }
                }

                for (int i = 0; i < hashList.length(); i++) {
                    JSONObject obj = hashList.getJSONObject(i);
                    String path = obj.getString("path");
                    if (freezeMap.containsKey(path)) {
                        JSONObject frozen = freezeMap.get(path);
                        obj.put("freeze", true);
                        obj.put("hash", frozen.getString("hash")); // 해시값도 기존 그대로
                    } else {
                        obj.put("freeze", false);
                    }
                }

                java.nio.file.Path jsonPath = java.nio.file.Paths.get(localRepoPath, ".jsRepohashed.json");
                java.nio.file.Files.writeString(jsonPath, hashList.toString(2));
                System.out.println("[해시 스냅샷 저장 완료 - 로컬]");
            } else {
                System.err.println("[해시 스냅샷 저장 실패: 로컬 경로를 찾을 수 없음]");
            }
        } catch (Exception e) {
            System.out.println("해시스냅샷의 알 수 없는 오류");
            e.printStackTrace();
        }
    }

    public static List<String> getDeletedPaths(JSONArray pathAll, String user, String repoName) {
        List<String> deletedPaths = new ArrayList<>();
        String basePath = getPath(user, repoName);
        System.out.println("getPath: "+basePath);
        if (basePath == null) {
            System.err.println("[경고] 로컬 경로를 찾을 수 없습니다.");
            return deletedPaths;
        }

        for (int i = 0; i < pathAll.length(); i++) {
            String jtreePath = pathAll.getJSONObject(i).getString("path");
            File file = new File(basePath, jtreePath);
            if (!file.exists()) {
                deletedPaths.add(jtreePath);
            }
        }
        // Sort deletedPaths by depth (deepest first)
        deletedPaths.sort((a, b) -> Integer.compare(b.split("/").length, a.split("/").length));
        return deletedPaths;
    }

    public static JSONArray mergeFailed=null;
    
    // --- /merge 병합 체크 메서드 ---
    public static boolean mergeCheck(String repoName, String owner) {
        mergeFailed=null;
        try {
            String localRepoPath = getPath(currentUser, repoName);
            if (localRepoPath == null) {
                System.err.println("[mergeCheck] 로컬 경로를 찾을 수 없습니다.");
                return false;
            }
    
            File hashFile = new File(localRepoPath, ".jsRepohashed.json");
            if (!hashFile.exists()) {
                System.err.println("[mergeCheck] .jsRepohashed.json 파일이 존재하지 않습니다.");
                return false;
            }
    
            String hashContent = Files.readString(hashFile.toPath());
            sendCommand("/merge " + repoName + " " + owner);
            out.println("/#/merge_hashes_SOL");
            out.println(hashContent);
            out.println("/#/merge_hashes_EOL");
            System.out.println("merge 전송완료");
            String response = receiveResponse();
            System.out.println("merge 응답: "+response);
            if (response != null && response.trim().equals("/#/merge_success")) {
                System.out.println("[클라이언트] 병합 가능: 모든 해시 일치");
                return true;
            } else if (response != null && response.trim().equals("/#/merge_fail")) {
                String mismatchJson = receiveResponse();
                mergeFailed = new JSONArray(mismatchJson);
                System.out.println("[클라이언트] 병합 실패: 일치하지 않는 파일 목록");
                for (int i = 0; i < mergeFailed.length(); i++) {
                    System.out.println(" - " + mergeFailed.getString(i));
                }
            } else {
                System.err.println("[클라이언트] 예기치 못한 응답: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[mergeCheck 예외]");
            e.printStackTrace();
            return false;
        }
        return false;
    }
    
    public static Set<String> getFrozenPaths(String user, String repoName,String repoOwner) {
        Set<String> frozenPaths = new java.util.HashSet<>();
        try {
            String localRepoPath = getPath(user, repoName);
            if (localRepoPath == null){System.out.println("1.종료됨");return frozenPaths;}

            File hashFile = new File(localRepoPath, ".jsRepohashed.json");
            if (!hashFile.exists()){System.out.println("2.종료됨");return frozenPaths;}

            String hashText = Files.readString(hashFile.toPath());
            JSONArray json;
            try {
                json = new JSONArray(hashText);
            } catch (org.json.JSONException e) {
                System.err.println("[getFrozenPaths] .jsRepohashed.json 파싱 오류 - 무시됨");
                e.printStackTrace();
                return frozenPaths;
            }
            System.out.println(json);
            String prefix = "repos/" + repoOwner + "/" + repoName + "/";

            for (int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                if (obj.optBoolean("freeze", false)) {
                    String fullPath = obj.getString("path");
                    System.out.println(fullPath+" | "+obj.getBoolean("freeze"));
                    if (fullPath.startsWith(prefix)) {
                        String relPath = fullPath.substring(prefix.length());
                        Path localPath = Paths.get(getPath(currentUser, repoName), relPath.split("/"));
                        System.out.println("추가됨: "+localPath.toFile().getAbsolutePath());
                        frozenPaths.add(localPath.toFile().getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[getFrozenPaths 오류]");
            e.printStackTrace();
        }
        return frozenPaths;
    }
    
    public static void getHash(String repoName, String owner){
        sendCommand("/hashJson " + repoName + " " + owner);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder jsonBuilder = new StringBuilder();
            String hashLine = "";
            while(true){
                hashLine+=reader.readLine();
                if(hashLine.endsWith("/#/pull_hashes_EOL")) break;
            }
            String solMarker = "/#/pull_hashes_SOL";
            int solIndex = hashLine.indexOf(solMarker);
            if (solIndex != -1) {
                hashLine = hashLine.substring(solIndex + solMarker.length()); // SOL 제거
            }
            String eolMarker = "/#/pull_hashes_EOL";
            int eolIndex = hashLine.indexOf(eolMarker);
            if (eolIndex != -1) {
                hashLine = hashLine.substring(0, eolIndex); // EOL 제거
            }
            jsonBuilder.append(hashLine);
            

            JSONArray serverHashList = new JSONArray(jsonBuilder.toString());

            // 기존 해시 목록 불러오기
            String localRepoPath = getPath(currentUser, repoName);
            File hashFile = new File(localRepoPath, ".jsRepohashed.json");
            java.util.Map<String, Boolean> existingFreezeMap = new java.util.HashMap<>();

            if (hashFile.exists()) {
                JSONArray existingList = new JSONArray(Files.readString(hashFile.toPath()));
                for (int i = 0; i < existingList.length(); i++) {
                    JSONObject obj = existingList.getJSONObject(i);
                    String path = obj.getString("path");
                    boolean freeze = obj.optBoolean("freeze", false);
                    if (freeze) {
                        existingFreezeMap.put(path, true); // freeze가 true인 항목만 보존
                    }
                }
            }

            for (int i = 0; i < serverHashList.length(); i++) {
                JSONObject obj = serverHashList.getJSONObject(i);
                String path = obj.getString("path");
                if (existingFreezeMap.containsKey(path)) {
                    obj.put("freeze", true);
                } else {
                    obj.put("freeze", false);
                }
            }

            saveHashSnapshot(currentUser, repoName, serverHashList);
        } catch (Exception ex) {
            System.out.println("클라이언트의 알 수 없는 오류");
            ex.printStackTrace();
        }
    }

}
