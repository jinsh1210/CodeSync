package views.repositoryView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.json.JSONArray;
import org.json.JSONObject;

import models.Repository;
import utils.*;

public class ColView extends JDialog {

    private Repository repository;
    private IconConv ic = new IconConv();

    // 생성자
    public ColView(Repository repository) {
        this.repository = repository;
    }

    // 콜라보 목록 보기
    public void handleViewCollaborators() {
        JDialog dialog = new JDialog(this, "콜라보레이터 목록", true);
        // 콜라보 목록 서버에 요청
        try {
            ClientSock.sendCommand("/list_collaborators " + repository.getName());

            StringBuilder responseBuilder = new StringBuilder();
            boolean started = false;

            while (true) {
                String line = ClientSock.receiveResponse();
                if (line == null)
                    break;
                // 진행중
                if (line.contains("/#/collaborator_list_SOL")) {
                    started = true;
                    continue;
                }
                // 요청 끝
                if (line.contains("/#/collaborator_list_EOL")) {
                    line = line.replace("/#/collaborator_list_EOL", "");
                    responseBuilder.append(line);
                    break;
                }
                // responseBuilder에 서버로부터 받은 데이터 추가
                if (started) {
                    responseBuilder.append(line);
                }
            }
            // 문자열로 변환
            String jsonText = responseBuilder.toString().trim();
            JSONArray collaborators = new JSONArray(jsonText);
            // 콜라보 표시 형식
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (int i = 0; i < collaborators.length(); i++) {
                JSONObject userObj = collaborators.getJSONObject(i);
                String userId = userObj.getString("user_id");
                listModel.addElement((i + 1) + ". " + userId);
            }
            // 콜라보 목록
            JList<String> collaboratorList = new JList<>(listModel);
            collaboratorList.setFont(Style.DESC_FONT);
            JScrollPane scrollPane = new JScrollPane(collaboratorList);
            scrollPane.setPreferredSize(new Dimension(300, 200));

            // 우클릭시 삭제 팝 메뉴
            JPopupMenu menu = new JPopupMenu();
            JMenuItem removeItem = new JMenuItem("삭제");
            menu.add(removeItem);
            collaboratorList.setComponentPopupMenu(menu);

            // 콜라보 삭제 로직
            removeItem.addActionListener(ev -> {
                int index = collaboratorList.getSelectedIndex();
                if (index >= 0) {
                    String selected = listModel.get(index);
                    String targetId = selected.substring(selected.indexOf(" ") + 1);
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                            "[" + targetId + "] 사용자를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        // 서버로 콜라보 삭제 요청
                        try {
                            ClientSock.sendCommand("/remove_collaborator " + repository.getName() + " " + targetId);
                            String res = ClientSock.receiveResponse();
                            if (res.startsWith("/#/remove_collaborator")) {
                                listModel.remove(index);
                            } else {
                                JOptionPane.showMessageDialog(dialog, "❌ 삭제 실패: " + res.replace("/#/error", "").trim());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(dialog, "서버 오류");
                        }
                    }
                }
            });

            // 콜라보 추가 로직
            JButton addButton = ic.createImageButton("src/icons/coladd.png", Style.PRIMARY_COLOR, 40, 40, "", "콜라보 추가");
            addButton.addActionListener(e -> {
                String newUser = addColDialog("추가할 사용자 아이디 입력:", dialog);
                if (newUser != null && !newUser.trim().isEmpty()) {
                    try {
                        ClientSock.sendCommand("/add_collaborator " + repository.getName() + " " + newUser.trim());
                        String response = ClientSock.receiveResponse();
                        if (response.startsWith("/#/add_collaborator")) {
                            listModel.addElement((listModel.size() + 1) + ". " + newUser.trim());
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "❌ 추가 실패: " + response.replace("/#/error", "").trim());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dialog, "서버 오류");
                    }
                }
            });

            // 콜라보 목록 패널
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.CENTER);

            // 버튼 패널
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            controlPanel.add(addButton);
            panel.add(controlPanel, BorderLayout.SOUTH);

            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.getContentPane().add(panel, BorderLayout.CENTER);

            JButton okButton = Style.createStyledButton("확인", Style.PRIMARY_COLOR, Color.WHITE);
            okButton.setPreferredSize(new Dimension(80, 30));
            okButton.addActionListener(e -> dialog.dispose());
            JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            okPanel.add(okButton);
            dialog.getContentPane().add(okPanel, BorderLayout.SOUTH);

            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "콜라보레이터 목록을 불러오는 중 오류 발생");
        }
    }

    // 콜라보 추가 입력창
    private String addColDialog(String message, JDialog parentDialog) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(message);
        label.setFont(Style.LABEL_FONT);
        panel.add(label, BorderLayout.NORTH);

        JTextField textField = Style.createStyledTextField();
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JPanel marginPanel = new JPanel(new BorderLayout());
        marginPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        marginPanel.add(textField, BorderLayout.CENTER);
        panel.add(marginPanel, BorderLayout.CENTER);

        JButton okButton = Style.createStyledButton("확인", Style.PRIMARY_COLOR, Color.WHITE);
        JButton cancelButton = Style.createStyledButton("취소", Style.WARNING_COLOR, Color.WHITE);
        okButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(parentDialog, "입력", true);
        dialog.setContentPane(panel);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        okButton.addActionListener(e -> dialog.dispose());
        textField.addActionListener(e -> dialog.dispose());
        cancelButton.addActionListener(e -> {
            textField.setText(null);
            dialog.dispose();
        });

        dialog.setVisible(true);
        return textField.getText().isEmpty() ? null : textField.getText().trim();
    }
}
