# [ **Code Sync** ]

---

## 👥 팀 구성 및 역할

| 학년/반 | 학번      | 이름   | 역할                      |
| ------- | --------- | ------ | ------------------------- |
| 2-B     | 202245066 | 진승현 | 클라이언트 및 GUI 개발    |
| 2-C     | 202245087 | 신재창 | 서버 및 데이터베이스 개발 |

---

## 📦 프로젝트 구조

```
src/
├── icons/                       # 아이콘 이미지 폴더
│
├── models/                      # 데이터 모델
│   ├── FileInfo.java            # 파일 정보 관리
│   ├── Repository.java          # 저장소 데이터 처리
│   └── User.java                # 사용자 정보 처리
│
├── utils/                       # 유틸리티 클래스
│   ├── Rounded/                 # 둥근 UI 요소
│   │   ├── RoundedButton.java   # 둥근 버튼
│   │   └── RoundedBorder.java   # 둥근 테두리
│   ├── ClientSock.java          # 클라이언트 소켓 통신
│   ├── IconConv.java            # 아이콘 색상 처리
│   └── Style.java               # 공통 스타일 관리
│
├── views/                       # 화면(UI) 구성
│   ├── login_register/          # 로그인 및 등록 화면
│   │   ├── LRCover.java
│   │   ├── LRFunc.java
│   │   ├── LRMain.java
│   │   └── LRView.java
│   │
│   ├── MainView/                # 메인 화면
│   │   ├── MainFunc.java
│   │   └── MainView.java
│   │
│   ├── repository/              # 저장소 관련 화면
│       ├── ColView.java         # 컬렉션 뷰
│       ├── FreezingView.java    # 프리징(동결) 뷰
│       ├── RepoFunc.java        # 저장소 기능 처리
│       └── RepoMainPanel.java   # 저장소 메인 패널
```

---

## 🗓️ 개발 일정 및 현황

- 🗓️ **5/17**: 데이터베이스 및 기본 GUI 구성 완료  
- 🗓️ **5/18**: ClientSock 추가 및 서버 연결 확인, 온라인 통신 테스트  
- 🗓️ **5/20**: 저장소, 다운로드, 삭제 기능 구현  
- 🗓️ **5/21**: UI 개선 및 리팩토링, 파일 구조 정리  
- 🗓️ **5/22**: 회원가입 복잡도 검사, 콜라보 추가/삭제/목록 구현, 프로그레스바 추가  
- 🗓️ **5/23**: 파일 탐색기 디자인 개선  
- 🗓️ **5/25**: 다운로드 속도 개선, 로컬 저장소 추가, UI 개선 및 풀 푸시 기능 추가  
- 🗓️ **5/26**: 버튼 오버 효과 추가, 버그 수정  
- 🗓️ **5/27**: 로그인/회원가입 화면, 메인/저장소 화면 통합 및 애니메이션 구현, 프리징 및 병합 기능 추가  
- 🗓️ **5/28**: 로그아웃 시 멈춤, 서버 푸시 멈춤 현상 해결 및 저장소 생성, 메뉴, 검색 UI 변경

---

## 🎯 애플리케이션 주요 기능
### 클라이언트
- 클라이언트 GUI 화면 구성 및 사용자 인터페이스
- 저장소 관리 및 파일 정보 처리
- 프리징(동결) 및 병합 기능
### 서버
- 서버 및 데이터베이스 연동
- 네트워크 클라이언트 소켓 통신

---

## 🖥️ 개발 환경

- 언어: Java
- 환경: macOS
- IDE: Eclipse, vsCode, NetBeans
- 버전: Java 11 이상
- UI: Swing, Java2D
- Maven:
```
<dependencies>
        <dependency>
            <groupId>com.fifesoft</groupId>
            <artifactId>rsyntaxtextarea</artifactId>
            <version>3.6.0</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.38</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
    		<groupId>mysql</groupId>
    		<artifactId>mysql-connector-java</artifactId>
    		<version>8.0.33</version>
		</dependency>

		<dependency>
    		<groupId>org.json</groupId>
    		<artifactId>json</artifactId>
    		<version>20210307</version>
		</dependency>

    <dependency>
        <groupId>com.miglayout</groupId>
        <artifactId>miglayout-swing</artifactId>
        <version>5.3</version>
    </dependency>

    <dependency>
        <groupId>net.java.dev.timingframework</groupId>
        <artifactId>timingframework</artifactId>
        <version>1.0</version>
    </dependency>

    </dependencies>
```
---
