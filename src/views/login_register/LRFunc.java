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

        String username = lrView.getUpusernameField().getText().trim();
        String password = new String(lrView.getUppasswordField().getPassword()).trim();
        String confirmPassword = new String(lrView.getUpconfirmPasswordField().getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("정보를 입력해주세요.");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            showErrorDialog("아이디는 영문자, 숫자만 사용할 수 있습니다.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showErrorDialog("비밀번호가 일치하지 않습니다.");
            return;
        }

        int count = 0;
        if (password.matches(".*[A-Z].*"))
            count++;
        if (password.matches(".*[a-z].*"))
            count++;
        if (password.matches(".*[^a-zA-Z0-9].*"))
            count++;

        if (password.length() < 8 || count < 2) {
            showErrorDialog("비밀번호는 8자 이상이며 대문자, 소문자, 특수문자 중 2종류 이상을 포함해야 합니다.");
            return;
        }

        try {
            String id = username;
            String pwd = password;

            ClientSock.sendCommand(":c:sign_up");
            ClientSock.sendCommand(id);
            ClientSock.sendCommand(pwd);

            String response = ClientSock.receiveResponse();
            if (response != null && response.startsWith("/#/info")) {
                showSuccessDialog("회원가입이 완료되었습니다.");
                lrView.showRegister(false);
                ClientSock.setUser(username);
                User user = new User();
                user.setUsername(username);
                SwingUtilities.getWindowAncestor(lrView).dispose();
                new MainView(user).setVisible(true);

            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim();
                showErrorDialog("회원가입 실패: " + msg);
            } else {
                showErrorDialog(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("서버 연결 실패");
        }
    }

    // 로그인 처리 로직
    public static void handleLogin() {
        String username = lrView.getInusernameField().getText().trim();
        String password = new String(lrView.getInpasswordField().getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("사용자 이름과 비밀번호를 입력해주세요.");
            return;
        }

        try {
            ClientSock.sendCommand(":c:login");
            ClientSock.sendCommand(username);
            ClientSock.sendCommand(password);
            String response = ClientSock.receiveResponse();

            if (response != null && response.startsWith("/#/info")) {
                ClientSock.setUser(username);
                User user = new User();
                user.setUsername(username);

                SwingUtilities.getWindowAncestor(lrView).dispose();
                new MainView(user).setVisible(true);

            } else if (response != null && response.startsWith("/#/error")) {
                String msg = response.replace("/#/error", "").trim();
                showErrorDialog(msg + ": 사용자 이름 또는 비밀번호가 일치하지 않습니다.");
            } else {
                showErrorDialog(response);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorDialog("서버 연결 실패");
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
