package utils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import models.FileInfo;

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

    public static void uploadFileToServer(FileInfo file) {
        try {
            sendCommand("UPLOAD|" + file.getFilename() + "|" + file.getBranch() + "|" + file.getFileData().length);
            outputStream.write(file.getFileData());
            outputStream.flush();

            String response = receiveResponse();
            if (response != null) {
                System.out.println("[서버 응답] " + response);
            }
        } catch (Exception e) {
            System.err.println("[Client] 파일 업로드 중 오류 발생");
            e.printStackTrace();
        }
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
