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
import javax.swing.JFrame;
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
import utils.ClientSock;
import utils.Style;

public class ColView extends JDialog {

    private Repository repository;

    public ColView(Repository repository){
        this.repository = repository;
    }

    public void handleViewCollaborators() {
        try {
            ClientSock.sendCommand("/list_collaborators " + repository.getName());

            StringBuilder responseBuilder = new StringBuilder();
            boolean started = false;

            while (true) {
                String line = ClientSock.receiveResponse();
                if (line == null) break;

                if (line.contains("/#/collaborator_list_SOL")) {
                    started = true;
                    continue;
                }
                if (line.contains("/#/collaborator_list_EOL")) {
                    line = line.replace("/#/collaborator_list_EOL", "");
                    responseBuilder.append(line);
                    break;
                }
                if (started) {
                    responseBuilder.append(line);
                }
            }

            String jsonText = responseBuilder.toString().trim();
            JSONArray collaborators = new JSONArray(jsonText);

            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (int i = 0; i < collaborators.length(); i++) {
                JSONObject userObj = collaborators.getJSONObject(i);
                String userId = userObj.getString("user_id");
                listModel.addElement((i + 1) + ". " + userId);
            }

            JList<String> collaboratorList = new JList<>(listModel);
            collaboratorList.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(collaboratorList);
            scrollPane.setPreferredSize(new Dimension(300, 200));

            JPopupMenu menu = new JPopupMenu();
            JMenuItem removeItem = new JMenuItem("삭제");
            removeItem.addActionListener(ev -> {
                int index = collaboratorList.getSelectedIndex();
                if (index >= 0) {
                    String selected = listModel.get(index);
                    String targetId = selected.substring(selected.indexOf(" ") + 1);
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "[" + targetId + "] 사용자를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            ClientSock.sendCommand("/remove_collaborator " + repository.getName() + " " + targetId);
                            String res = ClientSock.receiveResponse();
                            if (res.startsWith("/#/remove_collaborator")) {
                                listModel.remove(index);
                            } else {
                                JOptionPane.showMessageDialog(null, "❌ 삭제 실패: " + res.replace("/#/error", "").trim());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "서버 오류");
                        }
                    }
                }
            });
            menu.add(removeItem);
            collaboratorList.setComponentPopupMenu(menu);

            JButton addButton = Style.createStyledButton("추가", Style.PRIMARY_COLOR, Color.WHITE);
            addButton.setPreferredSize(new Dimension(80, 30));
            addButton.addActionListener(e -> {
                String newUser = showCustomInputDialog("추가할 사용자 아이디 입력:");
                if (newUser != null && !newUser.trim().isEmpty()) {
                    try {
                        ClientSock.sendCommand("/add_collaborator " + repository.getName() + " " + newUser.trim());
                        String response = ClientSock.receiveResponse();
                        if (response.startsWith("/#/add_collaborator")) {
                            listModel.addElement((listModel.size() + 1) + ". " + newUser.trim());
                        } else {
                            JOptionPane.showMessageDialog(null, "❌ 추가 실패: " + response.replace("/#/error", "").trim());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "서버 오류");
                    }
                }
            });

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            controlPanel.add(addButton);
            panel.add(controlPanel, BorderLayout.SOUTH);

            JDialog dialog = new JDialog(this, "콜라보레이터 목록", true);
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

    private String showCustomInputDialog(String message) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(message);
        label.setFont(Style.LABEL_FONT);
        panel.add(label, BorderLayout.NORTH);

        JTextField textField = Style.createStyledTextField();
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel marginPanel = new JPanel(new BorderLayout());
        marginPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        marginPanel.add(textField, BorderLayout.CENTER);
        panel.add(marginPanel, BorderLayout.CENTER);

        JButton okButton = Style.createStyledButton("확인", Style.PRIMARY_COLOR, Color.WHITE);
        JButton cancelButton = Style.createStyledButton("취소", new Color(192, 57, 43), Color.WHITE);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog((JFrame) null, "입력", true);
        dialog.setContentPane(panel);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        okButton.addActionListener(e -> dialog.dispose());
        cancelButton.addActionListener(e -> {
            textField.setText(null);
            dialog.dispose();
        });

        dialog.setVisible(true);
        return textField.getText().isEmpty() ? null : textField.getText().trim();
    }
}
