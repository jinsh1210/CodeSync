package views.repositoryView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.json.JSONArray;
import org.json.JSONObject;

import models.Repository;
import utils.ClientSock;
import utils.Style;

public class FreezingView extends JFrame {
    private DefaultListModel<String> frozenFileModel;
    private JList<String> frozenFileList;
    private JButton addButton;
    private String prefix;
    
    public FreezingView(Set<String> frozenFiles, Repository repository,String curUser) {
        this.prefix = "repos/" + repository.getUsername() + "/" + repository.getName() + "/";
        setTitle("프리징된 파일 목록");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        System.out.println(frozenFiles);
        frozenFileModel = new DefaultListModel<>();
        frozenFiles.stream().sorted().forEach(frozenFileModel::addElement);

        frozenFileList = new JList<>(frozenFileModel);
        frozenFileList.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(frozenFileList);

        addButton = Style.createStyledButton("파일추가", Style.PRIMARY_COLOR, Color.WHITE);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(addButton);

        // 파일 추가 버튼 동작: 파일 선택 후 프리징 처리
        addButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(ClientSock.getPath(curUser, repository.getName()));
            chooser.setDialogTitle("프리징할 파일 선택");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                String basePath = ClientSock.getPath(curUser, repository.getName());
                Path relativePath = Paths.get(basePath).toAbsolutePath().relativize(selectedFile.toPath().toAbsolutePath());
                String relPath = relativePath.toString().replace("\\", "/");
                String fullPath = prefix + relPath;
                String absPath = selectedFile.getAbsolutePath();

                // .jsRepohashed.json 업데이트
                try {
                    File hashFile = new File(basePath, ".jsRepohashed.json");
                    JSONArray json = new JSONArray(Files.readString(hashFile.toPath()));
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.getJSONObject(i);
                        if (obj.getString("path").equals(fullPath)) {
                            obj.put("freeze", true);
                            break;
                        }
                    }
                    Files.writeString(hashFile.toPath(), json.toString(2));
                    System.out.println("[프리징 추가됨] " + fullPath);
                    if (!frozenFileModel.contains(absPath)) {
                        frozenFileModel.addElement(absPath);
                    }
                } catch (Exception ex) {
                    System.err.println("[프리징 추가 오류]");
                    ex.printStackTrace();
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 우클릭 메뉴 구성
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem unfreezeItem = new JMenuItem("프리징 해제");
        contextMenu.add(unfreezeItem);

        frozenFileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                int index = frozenFileList.locationToIndex(e.getPoint());
                if (index != -1) {
                    frozenFileList.setSelectedIndex(index);
                    contextMenu.show(frozenFileList, e.getX(), e.getY());
                }
            }
        });

        // 메뉴 클릭 시 프리징 해제
        unfreezeItem.addActionListener(e -> {
            int selected = frozenFileList.getSelectedIndex();
            String localPath=ClientSock.getPath(curUser, repository.getName());
            if (selected != -1) {
                String relPath = frozenFileModel.getElementAt(selected);
                relPath=relPath.substring(localPath.length()+File.pathSeparator.length());
                String fullPath = prefix + relPath.replace(File.separator, "/");
                frozenFileModel.removeElementAt(selected);
                System.out.println("[프리징 해제됨] " + fullPath);
                // .jsRepohashed.json 업데이트
                try {
                    File hashFile = new File(localPath, ".jsRepohashed.json");
                    JSONArray json = new JSONArray(Files.readString(hashFile.toPath()));
                    for (int i = 0; i < json.length(); i++) {
                        org.json.JSONObject obj = json.getJSONObject(i);
                        if (obj.getString("path").equals(fullPath)) {
                            obj.put("freeze", false);
                            break;
                        }
                    }
                    Files.writeString(hashFile.toPath(), json.toString(2));
                    System.out.println("[해시 파일 업데이트 완료] " + fullPath);
                } catch (Exception ex) {
                    System.err.println("[해시 파일 업데이트 오류]");
                    ex.printStackTrace();
                }
            }
        });
    }

    public JButton getAddButton() {
        return addButton;
    }

    public DefaultListModel<String> getFrozenFileModel() {
        return frozenFileModel;
    }

    public void addFrozenFile(String filePath) {
        if (!frozenFileModel.contains(filePath)) {
            frozenFileModel.addElement(filePath);
        }
    }
}