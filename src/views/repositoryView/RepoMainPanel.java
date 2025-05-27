package views.repositoryView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

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

    private ColView collaboratorListView;
    private RepoFunc repoFunc;
    private IconConv ic = new IconConv();

    public RepoMainPanel(Repository repository, User currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;

        initializeUI();

        this.repoFunc = new RepoFunc(repository, currentUser, fileTree, rootNode, treeModel, progressBar, refreshTimer);
        this.collaboratorListView = new ColView(repository);
        repoFunc.loadFiles(
                repository.getUsername().equals(currentUser.getUsername()) ? null : repository.getUsername());
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Style.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 저장소 이름을 보여주는 제목 라벨 생성
        JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
        titleLabel.setFont(Style.TITLE_FONT);
        titleLabel.setForeground(Style.PRIMARY_COLOR);

        // 저장소 설명을 HTML 형식으로 변환하여 표시할 라벨 생성
        String html = "<html>" + repository.getDescription().replace("\n", "<br>") + "</html>";
        JLabel descLabel = new JLabel(html);
        descLabel.setFont(Style.LABEL_FONT);
        descLabel.setForeground(Color.BLACK);

        // 제목과 설명을 세로로 배치한 상단 헤더 패널 구성
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Style.BACKGROUND_COLOR);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 8))); // 제목과 설명 사이 간격
        headerPanel.add(descLabel);

        // 우측 상단 콜라보레이터 버튼 (본인 저장소일 때만 표시)
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.setBackground(Style.BACKGROUND_COLOR);
        if (repository.getUsername().equals(currentUser.getUsername())) {
            JButton collaborateButton = ic.createImageButton("src/icons/col.png", null, 50, 50, null,"콜라보");
            collaborateButton.addActionListener(e -> collaboratorListView.handleViewCollaborators());
            topRightPanel.add(collaborateButton);
        }

        // 업로드/다운로드 상태를 표시할 진행 바 생성
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 20));

        // 진행 바, 제목/설명, 콜라보 버튼을 포함한 헤더 전체 래퍼 구성
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setBackground(Style.BACKGROUND_COLOR);
        headerWrapper.add(progressBar, BorderLayout.NORTH);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);
        headerWrapper.add(topRightPanel, BorderLayout.EAST);
        add(headerWrapper, BorderLayout.NORTH);

        // 업로드/다운로드/삭제 버튼 생성 및 색상 설정
        int btn_width = 70;
        int btn_height = 60;
        uploadButton = ic.createImageButton("src/icons/upload.png", Style.PRIMARY_COLOR, btn_width, btn_height, null,"푸시");
        downloadButton = ic.createImageButton("src/icons/download.png", Style.PRIMARY_COLOR, btn_width, btn_height,
                null,"풀");
        freezingButton = ic.createImageButton("src/icons/freeze.png", Style.PRIMARY_COLOR, btn_width, btn_height, null,"파일프리징");
        localButton = ic.createImageButton("src/icons/local.png", Style.PRIMARY_COLOR, btn_width - 10, btn_height - 10,
                null,"로컬저장소 설정");
        deleteButton = ic.createImageButton("src/icons/delete.png", Style.WARNING_COLOR, btn_width - 5, btn_height - 15,
                null,"삭제");

        // 하단 버튼 영역 패널 구성
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        buttonPanel.setBackground(Style.BACKGROUND_COLOR);
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(freezingButton);
        buttonPanel.add(localButton);
        buttonPanel.add(deleteButton);

        // 버튼 패널을 포함한 전체 하단 패널 설정
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Style.BACKGROUND_COLOR);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 파일 트리의 루트 노드 및 모델 구성
        rootNode = new DefaultMutableTreeNode(repository.getName());
        treeModel = new DefaultTreeModel(rootNode);
        // 파일 트리 컴포넌트 생성 및 다크모드 스타일 지정
        fileTree = new JTree(treeModel);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(UIManager.getIcon("FileView.directoryIcon"));
        renderer.setClosedIcon(UIManager.getIcon("FileView.directoryIcon"));
        renderer.setLeafIcon(UIManager.getIcon("FileView.fileIcon")); // ✅ 핵심
        fileTree.setCellRenderer(renderer); // ✅ 꼭 호출

        fileTree.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object obj = node.getUserObject();
                setForeground(Color.BLACK);
                setBackgroundNonSelectionColor(Style.BACKGROUND_COLOR);
                setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
                setOpaque(false);
                if (obj instanceof String && "[비어 있음]".equals(obj)) {
                    setIcon(null);
                }
                return c;
            }
        });
        // 트리를 감싸는 스크롤 패널 생성 및 테두리 설정
        JScrollPane scrollPane = new JScrollPane(fileTree);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // 버튼에 업로드/다운로드/삭제 기능 연결
        uploadButton.addActionListener(e -> repoFunc.handleUpload());
        downloadButton.addActionListener(e -> repoFunc.handleDownload());
        deleteButton.addActionListener(e -> repoFunc.handleDelete());
        localButton.addActionListener(e -> repoFunc.handlesetLocalFolder());
        freezingButton.addActionListener(e -> new FreezingView(
                ClientSock.getFrozenPaths(currentUser.getUsername(), repository.getName(), repository.getUsername()),
                repository, currentUser.getUsername()).setVisible(true));

        refreshTimer = new Timer(3000, e -> repoFunc.loadFiles(
                repository.getUsername().equals(currentUser.getUsername()) ? null : repository.getUsername()));
        refreshTimer.start();
    }

    public void stopRefreshTimer() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }
}