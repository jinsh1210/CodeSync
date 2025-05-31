package views.MainView;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import models.Repository;
import models.User;
import net.miginfocom.swing.MigLayout;
import utils.ClientSock;
import utils.IconConv;
import utils.Style;
import views.repositoryView.RepoMainPanel;

@Getter
@Setter

public class MainFunc {
    private User currentUser;
    private JPanel detailPanel;
    private DefaultListModel<Repository> listModel;
    private MainView mainView;
    private IconConv ic = new IconConv();
    private JPanel overlayPanel;
    private Animator animator;

    // 생성자
    public MainFunc(DefaultListModel<Repository> listModel, JPanel detailPanel, User currentUser, MainView mainView,
            JPanel overlayPanel) {
        this.listModel = listModel;
        this.detailPanel = detailPanel;
        this.currentUser = currentUser;
        this.mainView = mainView;
        this.overlayPanel = overlayPanel;
    }

    // 저장소 목록을 불러와 리스트에 표시
    public void loadRepositories() {
        // 기존 내역 초기화
        listModel.clear();
        // 서버에 저장소 불러오기 시도
        try {
            ClientSock.sendCommand("/repo_list");

            String line;
            StringBuilder jsonBuilder = new StringBuilder();
            boolean inRepoList = false;

            while ((line = ClientSock.receiveResponse()) != null) {
                // 진행중
                if (line.equals("/#/repo_SOL")) {
                    inRepoList = true;
                    continue;
                    // 불러오기가 다 끝났을 떄
                } else if (line.equals("/#/repo_EOL")) {
                    break;
                }
                // 서버로 부터 받은 리스트를 jsonBuilder에 저장
                if (inRepoList) {
                    jsonBuilder.append(line);
                }
            }
            // jsonBuilder를 배열로 저장
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
    public JPanel showCreateRepositoryPanel() {
        JTextField nameField = Style.createStyledTextField();
        JTextArea descField = Style.createStyledTextArea(3, 20);
        JComboBox<String> visibilityComboBox = new JComboBox<>(new String[] {
                "private", "public" });
        JLabel titleLabel = new JLabel("저장소 생성");
        titleLabel.setForeground(Style.PRIMARY_COLOR);
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel(new MigLayout("wrap 2", "[right][grow,fill]",
                "[]10[]10[]10[]"));
        panel.setBackground(Style.FIELD_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setSize(new Dimension(400, 335));

        JScrollPane scrollPane = new JScrollPane(descField);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel, "span 2, center, gapbottom 20");

        panel.add(new JLabel("이름:"));
        panel.add(nameField, "growx, wmin 150");

        panel.add(new JLabel("설명:"));
        panel.add(scrollPane, "growx, h 80!");

        panel.add(new JLabel("권한:"));
        panel.add(visibilityComboBox, "growx, wmin 150");

        JButton cancelButton = Style.createStyledButton("취소", Style.PRIMARY_COLOR, Style.FIELD_BACKGROUND);
        JButton saveButton = Style.createStyledButton("생성", Style.WARNING_COLOR, Style.FIELD_BACKGROUND);

        saveButton.addActionListener(e -> {
            String rawName = nameField.getText().trim();
            String name = rawName.replaceAll("\\s+", "_");
            String description = descField.getText().trim();
            String selected = (String) visibilityComboBox.getSelectedItem();

            if (!name.equals(rawName)) {
                JOptionPane.showMessageDialog(null, "저장소 이름에 포함된 공백은 '_'로 자동 변경됩니다.\n변경된 이름: " + name, "이름 자동 수정",
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
                    toggleOverlayPanel();
                    nameField.setText("");
                    descField.setText("");
                } else if (response != null && response.startsWith("/#/error")) {
                    String msg = response.replace("/#/error", "").trim();
                    showErrorDialog("저장소 생성 실패: " + msg);
                } else {
                    showErrorDialog("알 수 없는 서버 응답: " + response);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "서버 연결 실패");
            }
        });
        cancelButton.addActionListener(e -> {
            loadRepositories();
            toggleOverlayPanel();
            nameField.setText("");
            descField.setText("");
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, "span 2, center, gapy 10");

        return panel;
    }

    // 저장소 패널 여는 로직
    public Timer openRepositoryPanel(Repository repository) {
        try {
            if (detailPanel.getComponentCount() > 0 && detailPanel.getComponent(0) instanceof RepoMainPanel) {
                ((RepoMainPanel) detailPanel.getComponent(0)).stopRefreshTimer();
            }
            // 저장소 띄울 detailPanel 초기화 이후 덮어쓰기
            RepoMainPanel repoView = new RepoMainPanel(repository, currentUser);
            detailPanel.removeAll();
            detailPanel.add(repoView);
            detailPanel.revalidate();
            detailPanel.repaint();
            return repoView.getRefreshTimer();
        } catch (Exception e) {
            // 실패할 경우
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "저장소 상세 패널 로딩 실패: " + e.getMessage());
            return null;
        }
    }

    // 저장소 표시 형식
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

    // 저장소 공개 여부 이미지 추가
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

    // 저장소 삭제 로직
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

    // 저장소 검색 로직
    public void searchRepositories() {
        // 검색창 텍스트 받아와서
        String keyword = mainView.getSearchField().getText();
        System.out.println(keyword);
        // 공백일 경우
        if (keyword == null || keyword.trim().isEmpty())
            return;
        // 초기화
        listModel.clear();
        // 서버에 검색 시도
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

            // JSON 부분 추출
            int startIdx = fullResponse.indexOf("/#/search_repo_SOL") + "/#/search_repo_SOL".length();
            int endIdx = fullResponse.indexOf("/#/search_repo_EOL");

            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                throw new RuntimeException("JSON 응답 파싱 실패: 구분자 오류");
            }
            // 정보를 String으로 변환
            String jsonText = fullResponse.substring(startIdx, endIdx).trim();
            // 디버깅
            System.out.println("[추출된 JSON]: " + jsonText);

            // 배열로 저장
            JSONArray jsonArray = new JSONArray(jsonText);
            Set<Integer> addedIds = new HashSet<>();

            // detailPanel에 불러올 정보 표현
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

            // 결과가 0일 경우 일치하는 항목 없음.
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

    // 공개여부 변경 로직
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

    // 콜라보 해제 로직
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

    // 저장소 추가 애니메이션 로직
    public void toggleOverlayPanel() {
        if (animator != null && animator.isRunning()) {
            animator.stop();
        }

        boolean isVisible = overlayPanel.isVisible();
        int startHeight = overlayPanel.getHeight();
        int targetHeight = (!isVisible || startHeight == 0) ? 335 : 0; // visible이 false거나 높이가 0이면 열림

        if (!isVisible) {
            overlayPanel.setVisible(true); // 🌟 패널이 닫힌 상태라면 열기
        }

        int frameWidth = mainView.getWidth();
        int x = (frameWidth - 400) / 2;
        int y = 100;

        animator = new Animator(500);
        animator.setAcceleration(0.4f);
        animator.setDeceleration(0.4f);
        animator.setResolution(10);

        animator.addTarget(new TimingTargetAdapter() {
            @Override
            public void timingEvent(float fraction) {
                int newHeight = (int) (startHeight + (targetHeight - startHeight) * fraction);
                overlayPanel.setBounds(x, y, 400, newHeight);
                overlayPanel.revalidate();
                overlayPanel.repaint();
            }

            @Override
            public void end() {
                if (targetHeight == 0) {
                    overlayPanel.setVisible(false); // 닫힘 시에만 감춤
                }
            }
        });
        animator.start();
    }
}
