package utils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import models.FileInfo;
import models.Repository;

public class ClientSock {
    private static final String SERVERIP = "sjc07250.iptime.org";
    private static final int PORT = 9969;
    private static final String VERISION = "v0.0.1b";
    private static final String TOKEN = "fjk123#%k2!lsd!234!%^^f17!@#sdfs!@$3$*s1s56!@#";

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static InputStream inputStream;
    private static OutputStream outputStream;

    public static void connect() {
        try {
            socket = new Socket(SERVERIP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // 초기 인증
            out.println(TOKEN);
            out.println(VERISION);
            
            try {
                byte[] buffer = new byte[1024];
                int bytesRead=inputStream.read(buffer);
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

    private static void receiveFile(String headerLine, File baseFolder) throws IOException {
        String[] tokens = headerLine.substring(13).trim().split(" ");
        if (tokens.length < 2) throw new IOException("pull_file 명령어 파싱 실패: " + headerLine);

        int size = Integer.parseInt(tokens[tokens.length - 1]);
        String filePath = String.join(" ", Arrays.copyOf(tokens, tokens.length - 1));

        File targetFile = new File(baseFolder, filePath);
        File parent = targetFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        System.out.println("[파일 수신 시작]: " + filePath + " (" + size + " bytes)");

        byte[] buffer = new byte[size];
        int offset = 0;
        while (offset < size) {
            int read = inputStream.read(buffer, offset, size - offset);
            if (read == -1) throw new EOFException("파일 수신 도중 스트림 종료됨: " + filePath);
            offset += read;
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(buffer);
        }

        System.out.println("[파일 저장 완료]: " + targetFile.getPath());
    }



    private static void handleSingleFilePull(String header, File baseFolder) throws IOException {
        receiveFile(header, baseFolder);
    }

    private static void handleDirectoryPull(BufferedReader reader, File baseFolder) throws IOException {
        while (true) {
            String line = reader.readLine();
            if(line.startsWith("/#/")) System.out.println("[명령어 수신]: " + line);
            if (line == null) throw new IOException("서버 연결 끊김");

            if (line.equals("/#/pull_dir_EOL")) {
                System.out.println("[디렉토리 수신 종료]");
                break;
            }

            if (line.startsWith("/#/pull_dir ")) {
                System.out.println("디렉토리 생성요청");
                String dirPath = line.substring(12).trim();
                File dir = new File(baseFolder, dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                    System.out.println("[디렉토리 생성]: " + dir.getPath());
                }
                continue;
            }

            if (line.startsWith("/#/pull_file ")) {
                receiveFile(line, baseFolder);
            }
        }
    }



    public static void pull(String repoName, String relPath, File targetFolder) {
        try {
            // 우선 서버에 요청 보냄
            sendCommand("/pull " + repoName + " " + relPath);

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
                handleSingleFilePull(line, targetFolder);
            } else if (line.equals("/#/pull_dir_SOL")) {
                handleDirectoryPull(reader, targetFolder);
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

    public static void push(File file, String repoName, int userId, String serverPath) { //파일형식 오버로드된 메소드
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            int fileSize = data.length;

            sendCommand("/push " + repoName + " \"" + serverPath + "\" " + fileSize);
            String response = receiveResponse();
            if (!"/#/push_ready".equals(response.trim())) {
                System.err.println("[Client] push 실패: 서버 응답 = " + response);
                return;
            }

            outputStream.write(data);
            outputStream.flush();
            String result = receiveResponse();
            System.out.println("[서버 응답] " + result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static void push(File folder, String basePath, String repository, int userId) throws IOException { //폴더 형식 전송 오버로드 메소드
        File[] contents = folder.listFiles();
        boolean isEmpty = (contents == null || contents.length == 0);

        // 상대 경로 계산
        String relativePath = basePath.isEmpty() ? folder.getName() : basePath + "/" + folder.getName();

        // 빈 폴더면 mkdir 명령어 전송
        if (isEmpty) {
            sendCommand("/mkdir " + repository + " " + relativePath);
            String response = receiveResponse();
            System.out.println("[서버 mkdir 응답] " + response);
            return;
        }

        // 폴더 안에 내용이 있을 경우
        for (File file : contents) {
            if (file.isFile()) {
                String filePath = relativePath + "/" + file.getName();
                push(file, repository, userId, filePath);
            } else if (file.isDirectory()) {
                push(file, relativePath,repository,userId);  // 재귀 호출
            }
        }
    }

    public static String receiveResponse() {
    	try {
            byte[] buffer = new byte[1024];
            int bytesRead=inputStream.read(buffer);
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

            // TODO: JSON 파싱 필요 시 여기에 파싱 로직 작성
            System.out.println("[파일 목록 수신]: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public static void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
