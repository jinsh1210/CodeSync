package views.repositoryView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import lombok.Getter;
import lombok.Setter;
import models.Repository;
import models.User;
import utils.*;

@Getter
@Setter

public class RepoMainPanel extends JPanel {
        private Repository repository;
        private User currentUser;

        private String lastSelectedPath = "";

        private JTree fileTree;
        private DefaultMutableTreeNode rootNode;
        private DefaultTreeModel treeModel;
        private JProgressBar progressBar;

        private JButton uploadButton, downloadButton, deleteButton, localButton, freezingButton;

        private Timer refreshTimer;

        private ColView colView;
        private RepoFunc repoFunc;
        private IconConv ic = new IconConv();

        // UI 초기화 및 파일 목록 불러오기
        public RepoMainPanel(Repository repository, User currentUser) {
                this.repository = repository;
                this.currentUser = currentUser;

                initializeUI(); // UI 생성

                this.repoFunc = new RepoFunc(repository, currentUser, fileTree, rootNode, treeModel, progressBar,
                                refreshTimer);
                this.colView = new ColView(repository);
                repoFunc.loadFiles(
                                repository.getUsername().equals(currentUser.getUsername()) ? null
                                                : repository.getUsername());
        }

        // UI 구성 요소 생성 및 배치
        private void initializeUI() {
                setLayout(new BorderLayout(10, 10));
                setBackground(Style.BACKGROUND_COLOR);
                setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
                titleLabel.setFont(Style.TITLE_FONT);
                titleLabel.setForeground(Style.PRIMARY_COLOR);

                String html = "<html>" + repository.getDescription().replace("\n", "<br>") + "</html>";
                JLabel descLabel = new JLabel(html);
                descLabel.setFont(Style.LABEL_FONT);
                descLabel.setForeground(Color.BLACK);

                JPanel headerPanel = new JPanel();
                headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
                headerPanel.setBackground(Style.BACKGROUND_COLOR);
                headerPanel.add(titleLabel);
                headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                headerPanel.add(descLabel);

                JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                topRightPanel.setBackground(Style.BACKGROUND_COLOR);
                if (repository.getUsername().equals(currentUser.getUsername())) {
                        JButton collaborateButton = ic.createImageButton("src/icons/col.png", Style.PRIMARY_COLOR, 30,
                                        30, null,
                                        "콜라보", true);
                        collaborateButton.addActionListener(e -> colView.handleViewCollaborators());
                        topRightPanel.add(collaborateButton);
                }
                progressBar = new JProgressBar(0, 100);
                progressBar.setVisible(false);
                progressBar.setStringPainted(true);
                progressBar.setPreferredSize(new Dimension(400, 20));

                JPanel headerWrapper = new JPanel(new BorderLayout());
                headerWrapper.setBackground(Style.BACKGROUND_COLOR);
                headerWrapper.add(progressBar, BorderLayout.NORTH);
                headerWrapper.add(headerPanel, BorderLayout.CENTER);
                headerWrapper.add(topRightPanel, BorderLayout.EAST);
                add(headerWrapper, BorderLayout.NORTH);

                int btn_width = 60;
                int btn_height = 60;
                uploadButton = ic.createImageButton("src/icons/upload.png", Style.PRIMARY_COLOR, btn_width, btn_height,
                                null,
                                "푸시", true);
                downloadButton = ic.createImageButton("src/icons/download.png", Style.PRIMARY_COLOR, btn_width,
                                btn_height,
                                null, "풀", true);
                freezingButton = ic.createImageButton("src/icons/freeze.png", Style.PRIMARY_COLOR, btn_width,
                                btn_height, null,
                                "파일프리징", true);
                localButton = ic.createImageButton("src/icons/local.png", Style.PRIMARY_COLOR, btn_width - 10,
                                btn_height - 10,
                                null, "로컬저장소 설정", true);
                deleteButton = ic.createImageButton("src/icons/delete.png", Style.WARNING_COLOR, btn_width - 5,
                                btn_height - 10,
                                null, "삭제", true);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
                buttonPanel.setBackground(Style.BACKGROUND_COLOR);
                buttonPanel.add(uploadButton);
                buttonPanel.add(downloadButton);
                buttonPanel.add(freezingButton);
                buttonPanel.add(localButton);
                buttonPanel.add(deleteButton);

                JPanel bottomPanel = new JPanel(new BorderLayout());
                bottomPanel.setBackground(Style.BACKGROUND_COLOR);
                bottomPanel.add(buttonPanel, BorderLayout.CENTER);
                add(bottomPanel, BorderLayout.SOUTH);

                rootNode = new DefaultMutableTreeNode(repository.getName());
                treeModel = new DefaultTreeModel(rootNode);
                fileTree = new JTree(treeModel);

                DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
                renderer.setOpenIcon(UIManager.getIcon("FileView.directoryIcon"));
                renderer.setClosedIcon(UIManager.getIcon("FileView.directoryIcon"));
                renderer.setLeafIcon(UIManager.getIcon("FileView.fileIcon"));
                fileTree.setCellRenderer(renderer);

                fileTree.setFont(Style.DESC_FONT);
                fileTree.setRootVisible(true);
                fileTree.setShowsRootHandles(true);
                fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
                        @Override
                        // 트리 노드 렌더링 설정
                        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                        boolean expanded,
                                        boolean leaf, int row, boolean hasFocus) {
                                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                                                hasFocus);
                                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                                Object obj = node.getUserObject();
                                setForeground(Color.BLACK);
                                setBackgroundNonSelectionColor(Style.BACKGROUND_COLOR);
                                setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
                                setOpaque(false);
                                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
                                if (obj instanceof String && "[비어 있음]".equals(obj)) {
                                        setIcon(null);
                                }
                                return c;
                        }
                });
                JScrollPane scrollPane = new JScrollPane(fileTree);
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
                scrollPane.getViewport().setOpaque(false);
                scrollPane.setOpaque(false);
                add(scrollPane, BorderLayout.CENTER);

                uploadButton.addActionListener(e -> repoFunc.handleUpload());
                downloadButton.addActionListener(e -> repoFunc.handleDownload());
                deleteButton.addActionListener(e -> repoFunc.handleDelete());
                localButton.addActionListener(e -> repoFunc.handlesetLocalFolder());
                freezingButton.addActionListener(e -> new FreezingView(
                                ClientSock.getFrozenPaths(currentUser.getUsername(), repository.getName(),
                                                repository.getUsername()),
                                repository, currentUser.getUsername()).setVisible(true));

                refreshTimer = new Timer(3000, e -> repoFunc.loadFiles(
                                repository.getUsername().equals(currentUser.getUsername()) ? null
                                                : repository.getUsername()));
                refreshTimer.start();
        }

        // 새로고침 타이머 중지
        public void stopRefreshTimer() {
                if (refreshTimer != null && refreshTimer.isRunning()) {
                        refreshTimer.stop();
                }
        }
}