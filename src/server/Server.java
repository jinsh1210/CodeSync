package server;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

class msgth extends Thread {
	private final String serverVersion = "v0.0.1b";
	private UserDAO userDAO = new UserDAO();
	private final String token = System.getenv("ServTOKEN");
	static ArrayList<Socket> users = new ArrayList<Socket>();
	static Hashtable<String, Socket> usrNick = new Hashtable<String, Socket>();
	private String myName;
	public static final Map<String, PrintWriter> clientWriters = new HashMap<>();
	private PrintWriter pwriter;
	Socket socket;
	OutputStream output = null;
	InputStream input = null;
	BufferedReader reader = null;
	private String iport="0.0.0.0:0000";
	public msgth(Socket socket) {
		this.socket = socket;
		users.add(socket);
		try {
			output = socket.getOutputStream();
			input = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(input));
			iport=socket.getLocalAddress() + ":" + socket.getLocalPort();
		} catch (Exception e) {
			System.out.println("쓰레드 생성 에러");
		}
	}
	
	private void send(String msg) throws Exception {
		output.write(msg.getBytes());
		output.flush();
	}
	private void send(JSONArray msg) throws Exception {
		output.write(msg.toString().getBytes());
		output.flush();
	}
	public void deleteDirectoryRecursively(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		}
	}
	public void run() {
		try {
			String msg;
			pwriter = new PrintWriter(socket.getOutputStream(), true);
			if (!token.equals(msg = reader.readLine())) {
				System.out.println("접근 권한 없음");
				System.out.println("전달 받은 토큰값: "+msg);
				socket.close();
				return;
			} else if (!serverVersion.equals(msg = reader.readLine())) {
				send("업데이트 필요! 버전: " + serverVersion);
				System.out.println("업데이트가 필요한 버전을 사용하고 있습니다");
				socket.close();
				return;
			}
			send("버전: " + serverVersion);
			firstAccess();
			clientWriters.put(myName, pwriter);
			while (true) {
				if ((msg = reader.readLine()) != null) {
					//명령어구현
					if (msg.startsWith("/repo_create ")) {
						Pattern pattern = Pattern.compile("^/repo_create\\s+(\\S+)\\s+\"((?:\\\\\"|[^\"])*)\"\\s+(public|private)$");
						Matcher matcher = pattern.matcher(msg);

						if (!matcher.matches()) {
							send("/#/error 형식: /repo_create <이름> \"<설명>\" <공개여부>");
							continue;
						}
						String repoName = matcher.group(1);
						if (repoName.contains("..") || repoName.startsWith("/") || repoName.contains("\\")
							||repoName.contains("repos/"+myName+"/"+repoName+"/"+".backup")) {
							send("/#/repo_create 잘못된 이름 형식입니다");
							continue;
						}
						String rawDescription = matcher.group(2).replace("\\\"", "\"");
						String description = rawDescription.replace("\\n", "\n");
						String visibility = matcher.group(3);
						String result = userDAO.createRepository(myName, repoName, description, visibility);
						if (result.equals("/#/repo_create 저장소 생성 성공")) {
							String baseDir = "repos/" + myName + "/" + repoName + "/";
							Files.createDirectories(Paths.get(baseDir));
						}
						send(result);
					}else if (msg.equals("/repo_list")) {
						JSONArray repos = userDAO.getRepositories(myName);
						send("/#/repo_SOL");
						Thread.sleep(100);
						send(repos);
						Thread.sleep(100);
						send("/#/repo_EOL");
					}else if (msg.startsWith("/repo_delete ")) {
						String[] parts = msg.split(" ", 2);
						if (parts.length < 2) {
							send("/#/error 잘못된 명령어형식입니다.");
							continue;
						}
						String repoName = parts[1];
						String result = userDAO.deleteRepository(myName, repoName);
						if(result.equals("/#/repo_delete_success")){
							Path repoPath = Paths.get("repos", myName, repoName);
            				deleteDirectoryRecursively(repoPath); // 유틸 메서드 호출
						}
						send(result);
					}else if (msg.startsWith("/mkdir ")) {
						// 정규식으로 파싱: /mkdir <repoName> "<dirPath>" <owner>
						Pattern pattern = Pattern.compile("^/mkdir\\s+(\\S+)\\s+\"(.+?)\"\\s+(\\S+)$");
						Matcher matcher = pattern.matcher(msg);

						if (!matcher.matches()) {
							send("/#/error 잘못된 명령어 형식입니다. (/mkdir <repo> \"<path>\" <owner>)");
							continue;
						}

						String repoName = matcher.group(1);
						String dirPath = matcher.group(2);
						String tmpOwner = matcher.group(3);
						String owner;
						if (userDAO.repositoryExists(myName, repoName) && tmpOwner.equals(myName)) {
							owner = myName;
						} else {
							String realOwner = userDAO.getRepositoryOwner(repoName,tmpOwner);
							System.out.println(repoName+" | "+tmpOwner);
							if (realOwner == null) {
								send("/#/mkdir_error 해당 저장소가 존재하지 않습니다");
								continue;
							}
							String repoId = userDAO.getRepositoryIdByOwnerAndName(realOwner, repoName);
							if (!userDAO.isCollaborator(repoId, myName)) {
								send("/#/mkdir_error 이 저장소에 디렉토리를 생성할 권한이 없습니다");
								continue;
							}
							owner = realOwner;
						}
						if (dirPath.contains("..") || dirPath.startsWith("/") || dirPath.contains("\\")
							||dirPath.contains("repos/"+owner+"/"+repoName+"/"+".backup")) {
							send("/#/mkdir_error 잘못된 경로형식입니다.");
							continue;
						}
						String fullPath = "repos/" + owner + "/" + repoName + "/" + dirPath;
						try {
							Files.createDirectories(Paths.get(fullPath));
							send("/#/mkdir_success " + dirPath);
						} catch (IOException e) {
							e.printStackTrace();
							send("/#/mkdir_error 디렉토리 생성 실패");
							continue;
						}
					}else if (msg.startsWith("/push ")) {
						// 정규식으로 따옴표 안 경로 추출
						Pattern pattern = Pattern.compile("^/push\\s+(\\S+)\\s+\"(.+?)\"\\s+(\\d+)\\s+(\\S+)$");
						Matcher matcher = pattern.matcher(msg);

						if (!matcher.matches()) {
							send("/#/push_error 형식: /push <repo> \"<경로>\" <size> <owner>");
							continue;
						}

						String repoName = matcher.group(1);
						String filePath = matcher.group(2);
						int fileSize = Integer.parseInt(matcher.group(3));
						String tmpowner=matcher.group(4);
						if (filePath.contains("..") || filePath.startsWith("/") || filePath.contains("\\")) {
							send("/#/push_error 잘못된 경로형식입니다.");
							continue;
						}
						String owner;
						if (userDAO.repositoryExists(myName, repoName)&&tmpowner.equals(myName)) {
							owner = myName;
						} else {
							String realOwner = userDAO.getRepositoryOwner(repoName,tmpowner);
							System.out.println(msg);
							
							if (realOwner == null) {
								send("/#/push_error 저장소를 찾을 수 없습니다.");
								continue;
							}

							String repoId = userDAO.getRepositoryIdByOwnerAndName(realOwner, repoName);
							if (!userDAO.isCollaborator(repoId, myName)) {
								send("/#/push_error 이 저장소에 푸시할 권한이 없습니다.");
								continue;
							}
							owner = realOwner;
						}
						if(fileSize==0&&filePath.equals("checkPermission")){
							send("/#/push_permission_Confirmed");
							continue;
						}
						send("/#/push_ready");

						String basePath = "repos/" + owner + "/" + repoName + "/";
						Path fullFilePath = Paths.get(basePath, filePath).normalize();						// 백업
						
						Files.createDirectories(fullFilePath.getParent());
						try (OutputStream out = new FileOutputStream(fullFilePath.toFile())) {
							byte[] buffer = new byte[4096];
							int remaining = fileSize;
							while (remaining > 0) {
								int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
								if (read == -1) break;
								out.write(buffer, 0, read);
								remaining -= read;
							}
							String hash = computeFileHash(fullFilePath);
							// DB에 기록
							String recordResult = userDAO.recordFileUpload(owner, repoName, fullFilePath.toString(),hash);
							send(recordResult);
							if(reader.readLine().equals("/ACK")){}
						} catch (IOException e) {
							e.printStackTrace();
							send("/#/push_error 파일 저장 실패");
						}
					} else if (msg.startsWith("/pull ")) {
						Pattern pattern = Pattern.compile("^/pull\\s+(\\S+)\\s+\"([^\"]*)\"\\s+(\\S+)$");

						Matcher matcher = pattern.matcher(msg);
						if (!matcher.matches()) {
							send("/#/pull_error 형식: /pull <repo> \"<path>\" <owner>\n");
							continue;
						}
						String repoName = matcher.group(1);
						String relPath = matcher.group(2);
						String tmpOwner = matcher.group(3);

						String owner;
						if (userDAO.repositoryExists(myName, repoName)&& tmpOwner.equals(myName)) {
							owner = myName;
						} else {
							String realOwner = userDAO.getRepositoryOwner(repoName, tmpOwner);
							if (realOwner == null) {
								send("/#/pull_error 저장소를 찾을 수 없습니다.\n");
								continue;
							}

							String repoId = userDAO.getRepositoryIdByOwnerAndName(realOwner, repoName);
							boolean isPublic = "public".equals(userDAO.isPublicRepository(realOwner, repoName));
							boolean isCollaborator = userDAO.isCollaborator(repoId, myName);

							if (!isPublic && !isCollaborator) {
								send("/#/pull_error 저장소에 접근할 권한이 없습니다.\n");
								continue;
							}

							owner = realOwner;
						}

						Path basePath = Paths.get("repos", owner, repoName);
						Path fullPath = basePath.resolve(relPath).normalize();
												// ✅ 해시 정보 먼저 전송
						JSONArray hashList = userDAO.getFileHash(owner, repoName);
						send("/#/pull_hashes_SOL\n");
						send(hashList.toString());
						send("/#/pull_hashes_EOL\n");

						if (!Files.exists(fullPath)) {
							send("/#/pull_error 요청한 경로가 존재하지 않습니다\n");
							continue;
						}

						if (Files.isRegularFile(fullPath)) {
							long size = Files.size(fullPath);
							send("/#/pull_file " + relPath + " " + size + "\n");
							try (InputStream in = Files.newInputStream(fullPath)) {
								byte[] buffer = new byte[8192];
								int read;
								while ((read = in.read(buffer)) != -1) {
									output.write(buffer, 0, read);
								}
								output.flush();
							}
							continue;
						}

						try (Stream<Path> stream = Files.walk(fullPath)) {
							List<Path> allPaths = stream.toList();
							send("/#/pull_dir_SOL\n");
							for (Path p : allPaths) {
								Path relative = basePath.relativize(p).normalize();
								String relativeStr;
								if(relPath.equals("")) relativeStr = repoName+"/"+relative.toString().replace("\\", "/");
								else relativeStr = relative.toString().replace("\\", "/");
								if (Files.isDirectory(p)) {
									send("/#/pull_dir " + relativeStr + "/\n");
								} else if (Files.exists(p) && Files.isRegularFile(p)) {
									if (p.toString().contains("/.backup")) continue;

									long size = Files.size(p);
									send("/#/pull_file " + relativeStr + " " + size + "\n");
									if(reader.readLine().equals("/ACK"))
									try (InputStream in = Files.newInputStream(p)) {
										byte[] buffer = new byte[4096];
										int read;
										while ((read = in.read(buffer)) != -1) {
											output.write(buffer, 0, read);
										}
										output.flush();
									}
								}
							}
							send("/#/pull_dir_EOL\n");
						} catch (IOException e) {
							e.printStackTrace();
							send("/#/pull_error 폴더 전송 중 오류 발생\n");
						}
					}else if (msg.startsWith("/repo_content ")) {
						String[] parts = msg.split(" ");
						if (parts.length < 2) {
							send("/#/error 잘못된 명령어형식입니다.");
							continue;
						}

						String owner, repoName;

						if (parts.length == 2) {
							// 형식: /repo_content <repoName> → 본인 저장소
							owner = myName;
							repoName = parts[1];
							// 존재 여부 확인
							if (!userDAO.repositoryExists(owner, repoName)) {
								send("/#/repo_content_error 저장소가 존재하지 않습니다");
								continue;
							}
						}else if (parts.length == 3) {
							owner = parts[1];
							repoName = parts[2];

							String repoId = userDAO.getRepositoryIdByOwnerAndName(owner, repoName);
							String visibility = userDAO.isPublicRepository(owner, repoName);
							boolean isPublic = "public".equals(visibility);
							boolean isCollaborator = userDAO.isCollaborator(repoId, myName);

							if (!isPublic && !isCollaborator) {
								send("/#/repo_content_error 접근 권한이 없습니다");
								continue;
							}
						}else {
							send("/#/repo_content_error 명령어 형식이 잘못되었습니다");
							continue;
						}

						// 경로 확인
						Path repoPath = Paths.get("repos", owner, repoName);
						if (!Files.exists(repoPath)) {
							send("/#/repo_content_error 경로가 없습니다");
							continue;
						}

						JSONArray list = new JSONArray();
						try (Stream<Path> paths = Files.walk(repoPath)) {
							paths
								.filter(p -> !p.equals(repoPath)) // 루트 제외
								.filter(p -> !p.toString().contains("/.backup")) // .backup 제외
								.forEach(p -> {
									try {
										String relPath = repoPath.relativize(p).toString().replace("\\", "/");
										JSONObject entry = new JSONObject();
										entry.put("path", relPath + (Files.isDirectory(p) ? "/" : ""));
										entry.put("type", Files.isDirectory(p) ? "dir" : "file");
										list.put(entry);
									} catch (Exception e) {
										e.printStackTrace();
									}
								});
						} catch (IOException e) {
							e.printStackTrace();
							send("/#/repo_content_error 파일 탐색 실패");
							continue;
						}

						try {
							send("/#/repo_content_SOL");
							Thread.sleep(100);
							send(list.toString());
							Thread.sleep(100);
							send("/#/repo_content_EOL");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}else if (msg.startsWith("/search_repos ")) {
						String[] parts = msg.split(" ", 2);
						if (parts.length < 2) {
							send("/#/search_repo_error 형식: /search_repos <키워드>");
							continue;
						}
						String keyword = parts[1];
						JSONArray found = userDAO.searchPublicRepositories(keyword);
						send("/#/search_repo_SOL");
						send(found.toString());
						send("/#/search_repo_EOL");
					}else if (msg.startsWith("/delete_file ")) {
						Pattern pattern = Pattern.compile("^/delete_file\\s+(\\S+)\\s+\"([^\"]+)\"\\s+(\\S+)$");
						Matcher matcher = pattern.matcher(msg);

						if (!matcher.matches()) {
							send("/#/error 삭제 명령 형식 오류");
							continue;
						}
						String repoName = matcher.group(1);
						String relPath = matcher.group(2);
						String userId = matcher.group(3);

						String repoOwner = userDAO.getRepositoryOwner(repoName,userId);
						if (repoOwner == null) {
							send("/#/error 저장소 소유자를 찾을 수 없습니다");
							continue;
						}

						// 🔑 저장소 ID 가져오기
						String repoId = userDAO.getRepositoryIdByOwnerAndName(userId, repoName);
						if (repoId == null) {
							send("/#/error 저장소 ID 조회 실패");
							continue;
						}
						
						// ✅ 자기 자신 또는 콜라보레이터만 허용
						boolean isAllowed =
							myName.equals(userId) ||
							userDAO.isCollaborator(repoId, myName);

						if (!isAllowed) {
							send("/#/error 권한이 없습니다");
							continue;
						}

						Path basePath = Paths.get("repos", repoOwner, repoName);
						Path targetPath = basePath.resolve(relPath).normalize();
						try {
							if (!Files.exists(targetPath)) {
								send("/#/error 삭제 대상이 존재하지 않습니다");
								continue;
							}

							if (Files.isDirectory(targetPath)) {
								try (Stream<Path> entries = Files.walk(targetPath)) {
									long fileCount = entries.skip(1).count(); // 폴더 자체 제외
									if (fileCount > 0) {
										send("/#/error 폴더가 비어있지 않습니다");
										continue;
									}
								}
								Files.delete(targetPath);
							} else {
								Files.delete(targetPath);
							}

							// ✅ DB 삭제 처리
							String deleteResult = userDAO.deleteFileRecord(userId, repoName, relPath);
							System.out.println("DB 삭제 결과: " + deleteResult);

							send("/#/delete_success " + relPath);
						} catch (Exception e) {
							e.printStackTrace();
							send("/#/error 삭제 중 오류 발생");
						}
					}else if (msg.startsWith("/list_collaborators ")) {
						String[] parts = msg.split(" ", 2);
						if (parts.length < 2) {
							send("/#/error 형식: /list_collaborators <repoName>");
							continue;
						}

						String repoName = parts[1];

						String repoId = userDAO.getRepositoryIdByOwnerAndName(myName, repoName);
						if (repoId == null) {
							send("/#/error 저장소가 존재하지 않거나 권한이 없습니다.");
							continue;
						}

						JSONArray collaborators = userDAO.getCollaborators(repoId);
						send("/#/collaborator_list_SOL");
						send(collaborators.toString());
						send("/#/collaborator_list_EOL");
					}else if (msg.startsWith("/add_collaborator ")) {
						String[] parts = msg.split(" ", 3);
						if (parts.length < 3) {
							send("/#/error 형식: /add_collaborator <repoName> <userId>");
							continue;
						}

						String repoName = parts[1];
						String targetUser = parts[2];

						String repoId = userDAO.getRepositoryIdByOwnerAndName(myName, repoName);
						if (repoId == null) {
							send("/#/error 저장소가 존재하지 않거나 권한이 없습니다.");
							continue;
						}

						if(!userDAO.searchUser(targetUser)){
							send("/#/error 유저를 검색할 수 없습니다.");
							continue;
						}

						if (userDAO.addCollaborator(repoId, targetUser)) {
							send("/#/add_collaborator 콜라보레이터가 추가되었습니다.");
						} else {
							send("/#/error 콜라보레이터 추가에 실패했습니다.");
						}
					}
					else if (msg.startsWith("/merge ")) {
					    String[] parts = msg.split(" ");
					    if (parts.length != 3) {
					        send("/#/merge_fail 명령어 형식 오류 (/merge <repo> <owner>)");
					        continue;
					    }

					    String repoName = parts[1];
					    String owner = parts[2];

					    // 해시 데이터 수신 준비
					    String startLine = reader.readLine();
						System.out.println(startLine);
					    if (!startLine.equals("/#/merge_hashes_SOL")) {
					        send("/#/merge_fail 해시 시작 지점 오류");
					        continue;
					    }
						System.out.println("Merge 요청");
					    StringBuilder hashDataBuilder = new StringBuilder();
					    String hashLine;
					    while ((hashLine = reader.readLine()) != null && !hashLine.equals("/#/merge_hashes_EOL")) {
					        hashDataBuilder.append(hashLine);
					    }

					    try {
					        JSONArray receivedHashes = new JSONArray(hashDataBuilder.toString());
					        JSONArray serverHashes = userDAO.getFileHash(owner, repoName);

					        Set<String> mismatchPaths = new HashSet<>();
					        Map<String, String> serverMap = new HashMap<>();
					        for (int i = 0; i < serverHashes.length(); i++) {
					            JSONObject obj = serverHashes.getJSONObject(i);
					            serverMap.put(obj.getString("path"), obj.getString("hash"));
					        }

					        for (int i = 0; i < receivedHashes.length(); i++) {
					            JSONObject clientFile = receivedHashes.getJSONObject(i);
					            String path = clientFile.getString("path");
					            String hash = clientFile.getString("hash");
								boolean freeze=clientFile.getBoolean("freeze");
								if(freeze) continue;
					            if (!serverMap.containsKey(path) || !serverMap.get(path).equals(hash)) {
					                mismatchPaths.add(path);
					            }
					        }

					        if (mismatchPaths.isEmpty()) {
					            send("/#/merge_success");
					        } else {
					            send("/#/merge_fail");
					            send(new JSONArray(mismatchPaths));
					        }
					    } catch (Exception e) {
					        e.printStackTrace();
					        send("/#/merge_fail 해시 비교 중 오류 발생");
					    }
					}else if (msg.startsWith("/remove_collaborator ")) {
						String[] parts = msg.split(" ");
						if (parts.length == 3) {
							String repoName = parts[1];
							String targetUser = parts[2];

							String repoId = userDAO.getRepositoryIdByOwnerAndName(myName, repoName);
							if (repoId == null) {
								send("/#/error 저장소가 존재하지 않거나 권한이 없습니다.");
								continue;
							}

							if (userDAO.removeCollaborator(repoId, targetUser)) {
								send("/#/remove_collaborator 콜라보레이터가 삭제되었습니다.");
							} else {
								send("/#/error 콜라보레이터 삭제에 실패했습니다.");
							}
						}else if (parts.length == 4) {
							// 형식: /remove_collaborator <repoName> <targetUser> <owner>
							String repoName = parts[1];
							String targetUser = parts[2];
							String owner = parts[3];

							String repoId = userDAO.getRepositoryIdByOwnerAndName(owner, repoName);
							if (repoId == null) {
								send("/#/error 저장소가 존재하지 않거나 권한이 없습니다.");
								continue;
							}

							// 소유자 또는 자기 자신만 제거 가능
							if (!myName.equals(owner) && !myName.equals(targetUser)) {
								send("/#/error 콜라보레이터는 자신만 삭제할 수 있습니다.");
								continue;
							}

							if (userDAO.removeCollaborator(repoId, targetUser)) {
								send("/#/remove_collaborator 콜라보레이터가 삭제되었습니다.");
							} else {
								send("/#/error 콜라보레이터 삭제에 실패했습니다.");
							}
						} else send("/#/error 형식: /remove_collaborator <repoName> <targetUser> [owner]");
					}else if(msg.startsWith("/hashJson ")){
						System.out.println("해쉬값 요청");
						String[] parts=msg.split(" ",3);
						if (parts.length < 3) {
							send("/#/error 형식: /hashJson <repoName> <userId>");
							continue;
						}
						String repoName=parts[1];
						String userId=parts[2];
												// ✅ 해시 정보 먼저 전송
						JSONArray hashList = userDAO.getFileHash(userId, repoName);
						send("/#/pull_hashes_SOL");
						send(hashList.toString());
						send("/#/pull_hashes_EOL\n");
						System.out.println("전송 완료");
					}else if(msg.startsWith("/change_visible ")){
						String[] parts=msg.split(" ",3);
						if (parts.length < 3) {
							send("/#/error 형식: /change_visible <repoName> <visible>");
							continue;
						}
						String repoName=parts[1];
						String visible=parts[2];
						send(userDAO.updateRepositoryVisibility(myName,repoName,visible));
					}else if(msg.equals("/logout")){
						System.out.println("종료 명령어 호출");
						break;
					}
				}else{
					System.out.println("연결 종료 감지");
					break;
				}
				
			}

		} catch (Exception e) {
			e.printStackTrace();
			
		}finally{
			System.out.println(iport+ " 연결해제");
			clientWriters.remove(myName);
			usrNick.remove(myName);
			userDAO.disconnect();
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String encrypt(String text) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(text.getBytes());
		} catch (NoSuchElementException e) {
			System.out.println("패스워드 암호화 실패");
		} finally {
			return bytesToHex(md.digest());
		}
	}
	// SHA-256 해시 계산 메소드
	private String computeFileHash(Path path) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (InputStream is = Files.newInputStream(path)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : digest.digest()) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b : bytes)
			builder.append(String.format("%02x", b));
		return builder.toString();
	}

	private void firstAccess() {
		String msg;
		int loginAccess = -1;
		do {
			try {
				msg = reader.readLine();
				if (msg.startsWith(":c:login")) {
					String id = reader.readLine();
					String pwd = encrypt(reader.readLine());
					String result = userDAO.login(id, pwd);
					if(result.startsWith("/#/info")) {
						myName = id;
						loginAccess = 1;
						send(result);
					} else send(result);
				} else if (msg.startsWith(":c:sign_up")) {	
					String id = reader.readLine();
					String pwd = encrypt(reader.readLine());
					String result = userDAO.signup(id, pwd);
					if(result.startsWith("/#/info")) {
						myName = id;
						loginAccess = 1;
						String baseDir = "repos/" + myName + "/";
						Files.createDirectories(Paths.get(baseDir));
						send(result);
					} else send(result);
				}else if(msg.equals(":c:logout")){
					System.out.println("종료 명령어 호출");
					userDAO.disconnect();
					socket.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

		} while (loginAccess == -1);
		usrNick.put(myName, socket);
	}
}

public class Server {
	static String serverVersion = "v0.0.1b";

	public static void main(String[] args) {
		int socket = 9969;
		try {
			ServerSocket ss = new ServerSocket(socket);
			System.out.println("서버열림 " + serverVersion);
			while (true) {
				Socket user = ss.accept();
				System.out.println("클라이언트 입장 " + user.getInetAddress().getHostAddress() + " : " + user.getPort());
				msgth th = new msgth(user);
				th.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
