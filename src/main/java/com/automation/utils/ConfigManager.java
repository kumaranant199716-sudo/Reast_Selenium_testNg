package com.automation.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Properties properties;
    private static final String CONFIG_FILE = "src/main/resources/config.properties";

    static {
        properties = new Properties();
        boolean loaded = false;
        // 1) Try to load from classpath (target/classes) which is correct for Maven/Surefire
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                properties.load(is);
                loaded = true;
            }
        } catch (IOException ignored) { }

        // 2) Fallback to loading from source path for IDE runs
        if (!loaded) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key) {
        return Integer.parseInt(getProperty(key));
    }

    public static int getIntProperty(String key, String defaultValue) {
        return Integer.parseInt(getProperty(key, defaultValue));
    }

    public static boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    public static boolean getBooleanProperty(String key, String defaultValue) {
        return Boolean.parseBoolean(getProperty(key, defaultValue));
    }
}
