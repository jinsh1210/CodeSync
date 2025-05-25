/* package utils;

import java.io.*;
import java.util.Properties;

public class UserSettings {
    private static final String FILE_NAME = "darkmode.properties";

    public static void saveDarkMode(String username, boolean isDarkMode) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(FILE_NAME)) {
            props.load(in);
        } catch (IOException ignored) {}

        props.setProperty(username + ".darkMode", String.valueOf(isDarkMode));
        try (FileOutputStream out = new FileOutputStream(FILE_NAME)) {
            props.store(out, "User Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadDarkMode(String username) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(FILE_NAME)) {
            props.load(in);
            return Boolean.parseBoolean(props.getProperty(username + ".darkMode", "false"));
        } catch (IOException e) {
            return false; // 기본값 false
        }
    }
}
 */
//TODO: 이건 DB에서 사용하면 필요없는 로직