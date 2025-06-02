package views.login_register;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import models.User;
import utils.ClientSock;
import views.MainView.MainView;

public class LRFunc {

    private static LRView lrView;

    public static void setLRView(LRView view) {
        lrView = view;
    }

    // 회원가입 처리 로직
    public static void handleSignUp() {

        // 입력 필드에서 사용자 이름, 비밀번호, 비밀번호 확인 값을 가져옴
        String username = lrView.getUpusernameField().getText().trim();
        String password = new String(lrView.getUppasswordField().getPassword()).trim();
        String confirmPassword = new String(lrView.getUpconfirmPasswordField().getPassword()).trim();

        // 정보 입력을 하지 않은 경우
        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("정보를 입력해주세요.");
            return;
        }
        // 허용하지 않은 문자 포함될 경우 (영문자, 숫자만 허용)
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            showErrorDialog("아이디는 영문자, 숫자만 사용할 수 있습니다.");
            return;
        }
        // 비밀번호와 재입력이 일치하지 않을 경우
        if (!password.equals(confirmPassword)) {
            showErrorDialog("비밀번호가 일치하지 않습니다.");
            return;
        }

        // 비밀번호 복잡성 검사 (대문자, 소문자, 숫자, 특수문자 중 2종류 이상 포함)
        int count = 0;
        // 대문자 포함 여부
        if (password.matches(".*[A-Z].*"))
            count++;
        // 소문자 포함 여부
        if (password.matches(".*[a-z].*"))
            count++;
        // 숫자 포함 여부
        if (password.matches(".*\\d.*"))
            count++;
        // 특수문자 포함 여부
        if (password.matches(".*[^a-zA-Z0-9].*"))
            count++;

        // 비밀번호 길이 및 복잡성 조건 확인
        if (password.length() < 8 || count < 2) {
            showErrorDialog("비밀번호는 8자 이상이며 숫자, 대문자, 소문자, 특수문자 중 2종류 이상을 포함해야 합니다.");
            return;
        }

        // 서버에 회원가입 시도
        try {
            String id = username;
            String pwd = password;

            // 서버에 회원가입 명령 전송
            ClientSock.sendCommand(":c:sign_up");
            ClientSock.sendCommand(id);
            ClientSock.sendCommand(pwd);

            // 서버 응답 수신
            String response = ClientSock.receiveResponse();
            // 회원가입 성공 시
            if (response != null && response.startsWith("/#/info")) {
                showSuccessDialog("회원가입이 완료되었습니다."); // 성공 메시지 표시
                lrView.showRegister(false); // 회원가입 창 닫기
                ClientSock.setUser(username); // 클라이언트에 사용자 정보 저장
                User user = new User(); // User 객체 생성
                user.setUsername(username); // User 객체에 사용자 이름 설정
                SwingUtilities.getWindowAncestor(lrView).dispose(); // 창 닫기
                new MainView(user).setVisible(true); // 메인 화면 표시
            // 실패한 경우 에러창 표시
            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim(); // 에러 메시지 추출
                showErrorDialog("회원가입 실패: " + msg); // 에러 메시지 표시
            } else {
                showErrorDialog(response); // 기타 응답에 대한 에러 메시지 표시
            }
        } catch (Exception e) {
            e.printStackTrace(); // 예외 출력
            showErrorDialog("서버 연결 실패"); // 서버 연결 실패 메시지 표시
        }
    }

    // 로그인 처리 로직
    public static void handleLogin() {
        // 입력 필드에서 사용자 이름과 비밀번호를 가져옴
        String username = lrView.getInusernameField().getText().trim();
        String password = new String(lrView.getInpasswordField().getPassword()).trim();
        // 정보 입력을 다 하지 않은 경우
        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("사용자 이름과 비밀번호를 입력해주세요."); // 에러 메시지 표시
            return;
        }
        // 서버에 로그인 시도
        try {
            ClientSock.sendCommand(":c:login"); // 로그인 명령 전송
            ClientSock.sendCommand(username);   // 사용자 이름 전송
            ClientSock.sendCommand(password);   // 비밀번호 전송
            String response = ClientSock.receiveResponse(); // 서버 응답 수신

            // 로그인 성공 시
            if (response != null && response.startsWith("/#/info")) {
                ClientSock.setUser(username); // 클라이언트에 사용자 정보 저장
                User user = new User();       // User 객체 생성
                user.setUsername(username);   // User 객체에 사용자 이름 설정
                SwingUtilities.getWindowAncestor(lrView).dispose(); // 로그인 창 닫기
                new MainView(user).setVisible(true); // 메인 화면 표시
            // 실패한 경우 에러창 표시
            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim(); // 에러 메시지 추출
                showErrorDialog(msg + ": 사용자 이름 또는 비밀번호가 일치하지 않습니다."); // 에러 메시지 표시
            } else {
                showErrorDialog(response); // 기타 응답에 대한 에러 메시지 표시
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // 예외 출력
            showErrorDialog("서버 연결 실패"); // 서버 연결 실패 메시지 표시
        }
    }

    // 에러 메시지 팝업 표시
    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    // 성공 메시지 팝업 표시
    private static void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "성공", JOptionPane.INFORMATION_MESSAGE);
    }
}
