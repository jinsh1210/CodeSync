package views.repositoryView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
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
import utils.Style;
@Getter
@Setter

public class RepoMainView extends JFrame {
    private Repository repository;
    private User currentUser;
    private String targetUser;
    private String lastSelectedPath = "";

    private JTree fileTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JProgressBar progressBar;

    private JButton uploadButton, downloadButton, deleteButton, localButton;

    private Timer refreshTimer;

	private CollaboratorListView collaboratorListView;
	private RepoFunc repoFunc;

    public RepoMainView(Repository repository, User currentUser, String targetUser) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.targetUser = targetUser;

        initializeUI();

		this.repoFunc = new RepoFunc(repository, currentUser, fileTree, rootNode, treeModel, progressBar, refreshTimer);
		this.collaboratorListView = new CollaboratorListView(repository);
        repoFunc.loadFiles(targetUser);
    }

    private void initializeUI() {
        // 프레임 제목, 크기, 닫힘 동작 등 기본 설정
		setTitle("J.S.Repo - Repository");
		setSize(650, 600);
		setMinimumSize(new Dimension(650, 600));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		// 전체 구성요소를 담는 메인 패널 설정 (여백 및 배경색 포함)
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBackground(Style.BACKGROUND_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 저장소 이름을 보여주는 제목 라벨 생성
		JLabel titleLabel = new JLabel("저장소 : " + repository.getName(), SwingConstants.LEFT);
		titleLabel.setFont(Style.TITLE_FONT);
		titleLabel.setForeground(Style.PRIMARY_COLOR);

		// 저장소 설명을 HTML 형식으로 변환하여 표시할 라벨 생성
		String html = "<html>" + repository.getDescription().replace("\n", "<br>") + "</html>";
		JLabel descLabel = new JLabel(html);
		descLabel.setFont(Style.LABEL_FONT);
		descLabel.setForeground(new Color(80, 80, 80));

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
			JButton collaborateButton = new JButton("");
			collaborateButton.setFocusable(false);
			ImageIcon icon = new ImageIcon("src/icons/collabor.png");
			Image scaledIcon = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
			collaborateButton.setIcon(new ImageIcon(scaledIcon));
			collaborateButton.setContentAreaFilled(false);
			collaborateButton.setBorderPainted(false);
			collaborateButton.setOpaque(false);
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
		mainPanel.add(headerWrapper, BorderLayout.NORTH);

		// 업로드/다운로드/삭제 버튼 생성 및 색상 설정
		uploadButton = Style.createStyledButton("푸쉬", Style.PRIMARY_COLOR, Color.WHITE);
		downloadButton = Style.createStyledButton("풀", new Color(41, 128, 185), Color.WHITE);
		deleteButton = Style.createStyledButton("삭제", new Color(231, 76, 60), Color.WHITE);
		localButton = Style.createStyledButton("로컬 저장소", new Color(231, 76, 60), Color.WHITE);

		// 하단 버튼 영역 패널 구성
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
		buttonPanel.setBackground(Style.BACKGROUND_COLOR);
		buttonPanel.add(uploadButton);
		buttonPanel.add(downloadButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(localButton);

		// 버튼 패널을 포함한 전체 하단 패널 설정
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(Style.BACKGROUND_COLOR);
		bottomPanel.add(buttonPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		// 파일 트리의 루트 노드 및 모델 구성
		rootNode = new DefaultMutableTreeNode(repository.getName());
		treeModel = new DefaultTreeModel(rootNode);
		// 파일 트리 컴포넌트 생성 및 다크모드 스타일 지정
		fileTree = new JTree(treeModel);
		//
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
				if (Style.isDarkMode) {
					setForeground(Style.DARK_TEXT_COLOR);
					setBackgroundNonSelectionColor(Style.DARK_BACKGROUND_COLOR);
					setBackgroundSelectionColor(new Color(70, 70, 70));
				}
				if (obj instanceof String && "[비어 있음]".equals(obj)) {
					setIcon(null);
				}
				return c;
			}
		});
		// 트리를 감싸는 스크롤 패널 생성 및 테두리 설정
		JScrollPane scrollPane = new JScrollPane(fileTree);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// 모든 구성 요소가 담긴 mainPanel을 프레임에 추가
		add(mainPanel);
		// 다크 모드 적용
		applyDarkMode();

		// 버튼에 업로드/다운로드/삭제 기능 연결
		uploadButton.addActionListener(e -> repoFunc.handleUpload());
		downloadButton.addActionListener(e -> repoFunc.handleDownload());
		deleteButton.addActionListener(e -> repoFunc.handleDelete());
		localButton.addActionListener(e -> repoFunc.handlesetLocalFolder());

		// 자동 새로고침 타이머 설정 (3초 주기)
		refreshTimer = new Timer(3000, e -> repoFunc.loadFiles(targetUser));
		refreshTimer.start();
		// 창이 닫힐 때 타이머 종료 이벤트 설정
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (refreshTimer != null && refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});
	}

    private void applyDarkMode() {
        Color bg = Style.isDarkMode ? Style.DARK_BACKGROUND_COLOR : Style.BACKGROUND_COLOR;
        Color fg = Style.isDarkMode ? Style.DARK_TEXT_COLOR : Style.TEXT_PRIMARY_COLOR;
        getContentPane().setBackground(bg);
        applyComponentDarkMode(getContentPane(), bg, fg);
    }

    private void applyComponentDarkMode(Component comp, Color bg, Color fg) {
        if (comp instanceof Container) {
            comp.setBackground(bg);
            comp.setForeground(fg);
            for (Component child : ((Container) comp).getComponents()) {
                applyComponentDarkMode(child, bg, fg);
            }
        }
    }
}
