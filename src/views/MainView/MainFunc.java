package views.MainView;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import models.Repository;
import models.User;
import net.miginfocom.swing.MigLayout;
import utils.ClientSock;
import utils.Style;
import views.repositoryView.RepoMainPanel;

@Getter
@Setter

public class MainFunc {
    private User currentUser;
    private JPanel detailPanel;
    private DefaultListModel<Repository> listModel;
    private MainView mainView;

    // 생성자
    public MainFunc(DefaultListModel<Repository> listModel, JPanel detailPanel, User currentUser, MainView mainView) {
        this.listModel = listModel;
        this.detailPanel = detailPanel;
        this.currentUser = currentUser;
        this.mainView = mainView;
    }

    // 저장소 목록을 불러와 리스트에 표시
    public void loadRepositories() {
        listModel.clear();
        try {
            ClientSock.sendCommand("/repo_list");

            String line;
            StringBuilder jsonBuilder = new StringBuilder();
            boolean inRepoList = false;

            while ((line = ClientSock.receiveResponse()) != null) {
                if (line.equals("/#/repo_SOL")) {
                    inRepoList = true;
                    continue;
                } else if (line.equals("/#/repo_EOL")) {
                    break;
                }

                if (inRepoList) {
                    jsonBuilder.append(line);
                }
            }

            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            Set<Integer> addedIds = new HashSet<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                String description = obj.getString("description");
                String visibility = obj.getString("visibility");
                String username = obj.getString("user");
                double filesize = obj.getDouble("size");

                int id = i;
                if (!addedIds.contains(id)) {
                    Repository repo = new Repository(id, name, description, visibility, username, filesize);
                    listModel.addElement(repo);
                    addedIds.add(id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "저장소 로딩 실패");
        }
    }

    // 저장소 생성 다이얼로그를 띄우고 서버에 생성 요청
    public void showCreateRepositoryDialog() {
        JTextField nameField = new JTextField();
        JTextArea descField = new JTextArea(3, 20);
        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);
        JComboBox<String> visibilityComboBox = new JComboBox<>(new String[] { "private", "public" });

        JPanel panel = new JPanel(new MigLayout("wrap 2", "[right][grow,fill]", "[]10[]10[]10[]"));
        panel.setBackground(Style.BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("이름:"));
        panel.add(nameField, "growx, wmin 150");

        panel.add(new JLabel("설명:"));
        panel.add(new JScrollPane(descField), "growx, h 80!");

        panel.add(new JLabel("권한:"));
        panel.add(visibilityComboBox, "growx, wmin 150");

        int result = JOptionPane.showConfirmDialog(null, panel, "저장소 생성", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String rawName = nameField.getText().trim();
            String name = rawName.replaceAll("\\s+", "_");
            String description = descField.getText().trim();
            String selected = (String) visibilityComboBox.getSelectedItem();

            if (!name.equals(rawName)) {
                JOptionPane.showMessageDialog(null, "저장소 이름에 포함된 공백은 밑줄(_)로 자동 변경됩니다.\n변경된 이름: " + name, "이름 자동 수정",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(null, "저장소 이름을 입력해주세요.");
                return;
            }

            try {
                String safeDescription = description.replace("\n", "\\n");
                ClientSock.sendCommand("/repo_create " + name + " \"" + safeDescription + "\" " + selected);
                String response = ClientSock.receiveResponse();

                if (response != null && response.contains("/#/repo_create 저장소 생성 성공")) {
                    JOptionPane.showMessageDialog(null, "저장소 생성 성공");
                    loadRepositories();
                } else if (response != null && response.startsWith("/#/error")) {
                    String msg = response.replace("/#/error", "").trim();
                    showErrorDialog("저장소 생성 실패: " + msg);
                } else {
                    showErrorDialog("알 수 없는 서버 응답: " + response);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "서버 연결 실패");
            }
        }
    }

    // 저장소 패널 로딩
    public Timer openRepositoryPanel(Repository repository) {
        try {
            if (detailPanel.getComponentCount() > 0 && detailPanel.getComponent(0) instanceof RepoMainPanel) {
                ((RepoMainPanel) detailPanel.getComponent(0)).stopRefreshTimer();
            }
            RepoMainPanel repoView = new RepoMainPanel(repository, currentUser);
            detailPanel.removeAll();
            detailPanel.add(repoView);
            detailPanel.revalidate();
            detailPanel.repaint();
            return repoView.getRefreshTimer();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "저장소 상세 패널 로딩 실패: " + e.getMessage());
            return null;
        }
    }

    // 저장소 리스트 셀 렌더링 (형식 지정)
    public static class RepositoryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Repository) {
                Repository repo = (Repository) value;

                setIcon(getVisibilityIcon(repo.getVisibility()));
                String description = repo.getDescription();
                if (description.length() > 5) {
                    description = description.substring(0, 5) + "...";
                }
                setText("저장소: " + repo.getName() + " | " + description);
            }
            return this;
        }
    }

    // 저장소 Public | Private 이미지 추가
    private static ImageIcon getVisibilityIcon(String visibility) {
        String filename = visibility.equalsIgnoreCase("public")
                ? "unlocked.png"
                : "locked.png";

        // 상대 경로로 src/icons/ 접근
        String path = "src/icons/" + filename;
        java.io.File file = new java.io.File(path);

        if (!file.exists()) {
            System.err.println("⚠️ 아이콘 파일을 찾을 수 없습니다: " + path);
            return new ImageIcon();
        }

        // 리사이징 처리
        ImageIcon originalIcon = new ImageIcon(path);
        Image scaledImage = originalIcon.getImage().getScaledInstance(30, 40, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage); // 파일 경로에서 직접 로딩
    }

    // 저장소 삭제 요청 및 응답 처리
    public void handleDeleteRepository(Repository selected) {
        int confirm = JOptionPane.showConfirmDialog(null, "정말로 '" + selected.getName() + "' 저장소를 삭제하시겠습니까?",
                "저장소 삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ClientSock.sendCommand("/repo_delete " + selected.getName());
                String response = ClientSock.receiveResponse();
                if (response.startsWith("/#/repo_delete_success")) {
                    JOptionPane.showMessageDialog(null, "저장소가 삭제되었습니다.");
                    loadRepositories();
                } else if (response != null && response.startsWith("/#/repo_delete_fail")) {
                    showErrorDialog("삭제 실패: 저장소 삭제 권한이 없습니다.");
                } else {
                    showErrorDialog(response);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "서버 연결 실패");
            }
        }
    }

    // 저장소 검색 기능
    public void searchRepositories() {
        // String keyword = JOptionPane.showInputDialog(null, "검색할 키워드를 입력하세요:");
        String keyword = mainView.getSearchField().getText();
        System.out.println(keyword);
        if (keyword == null || keyword.trim().isEmpty())
            return;

        listModel.clear();
        try {
            ClientSock.sendCommand("/search_repos " + keyword);

            StringBuilder rawBuilder = new StringBuilder();
            while (true) {
                String part = ClientSock.receiveResponse();
                rawBuilder.append(part);
                if (part.contains("/#/search_repo_EOL"))
                    break;
            }

            String fullResponse = rawBuilder.toString().trim();
            System.out.println("[서버 응답]: " + fullResponse);

            // ✅ JSON 부분 추출
            int startIdx = fullResponse.indexOf("/#/search_repo_SOL") + "/#/search_repo_SOL".length();
            int endIdx = fullResponse.indexOf("/#/search_repo_EOL");

            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                throw new RuntimeException("JSON 응답 파싱 실패: 구분자 오류");
            }

            String jsonText = fullResponse.substring(startIdx, endIdx).trim();
            System.out.println("[추출된 JSON]: " + jsonText);

            JSONArray jsonArray = new JSONArray(jsonText);
            Set<Integer> addedIds = new HashSet<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int id = obj.optInt("id", i);
                String name = obj.getString("name");
                String description = obj.getString("description");
                String visibility = obj.getString("visibility");
                String username = obj.getString("user");
                double filesize = obj.getDouble("size");

                if (!addedIds.contains(id)) {
                    Repository repo = new Repository(id, name, description, visibility, username, filesize);
                    listModel.addElement(repo);
                    addedIds.add(id);
                }
            }

            if (jsonArray.length() == 0) {
                JOptionPane.showMessageDialog(null, "검색 결과가 없습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "저장소 검색 실패");
        }
    }

    // 에러 메시지 일괄 처리 팝업
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void handleChangeVisible(String username, String repoName, String visible) {
        ClientSock.sendCommand("/change_visible " + repoName + " " + visible);
        String result = ClientSock.receiveResponse();
        if (result.startsWith("/#/visibility_update_fail")) {
            String message = result.replaceFirst("/#/visibility_update_fail\\s*", "").trim();
            JOptionPane.showMessageDialog(null, "설정 실패: " + message);
        } else if (result.startsWith("/#/visibility_update_success")) {
            String message = result.replaceFirst("/#/visibility_update_success\\s*", "").trim();
            JOptionPane.showMessageDialog(null, "변경됨: " + message);
        }
        loadRepositories();
    }

    public void handleRmCollabo(String repoName, String curUser, String owner) {
        ClientSock.sendCommand("/remove_collaborator " + repoName + " " + curUser + " " + owner);
        System.out.println("/remove_collaborator " + repoName + " " + curUser + " " + owner);
        String result = ClientSock.receiveResponse();
        if (result.startsWith("/#/error")) {
            String message = result.replaceFirst("/#/error\\s*", "").trim();
            JOptionPane.showMessageDialog(null, "콜라보 해제 실패\n" + message);
        } else if (result.startsWith("/#/remove_collaborator")) {
            JOptionPane.showMessageDialog(null, "콜라보 해제 성공");
        }
        loadRepositories();
    }
}
