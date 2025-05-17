package utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
//아직 미구현 서버 클라이언트간 통신 방식 맞춰줘야함
public class ClientSock{
    final String SERVERIP = "sjc07250.iptime.org"; // 접속할 서버 IP
    final int PORT = 9969; // 서버 포트
    final String VERISION="v0.0.1b";
    final String TOKEN="fjk123#%k2!lsd!234!%^^f17!@#sdfs!@$3$*s1s56!@#";
    private Socket socket=null;
    private PrintWriter out=null;
    private BufferedReader in=null;
    public ClientSock(){
        try{
            socket = new Socket(SERVERIP,PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void Send(String msg){
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String userInput = scanner.nextLine();
            out.println(userInput);
            if ("exit".equalsIgnoreCase(userInput)) {
                try {
                    socket.close();
                    System.out.println("연결 종료");
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void main() {
        try {
            Thread receiveThread = new Thread(() -> {
                try{
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        String received = new String(buffer, 0, bytesRead, "UTF-8");
                        System.out.println("받은 내용: " + received);
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            });
            // 사용자 입력 송신 쓰레드
            
            receiveThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
