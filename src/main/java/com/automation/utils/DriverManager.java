package com.automation.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DriverManager {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final String BROWSER = System.getProperty("browser", "chrome").toLowerCase();

    public static WebDriver getDriver() {
        if (driver.get() == null) {
            initializeDriver();
        }
        return driver.get();
    }

    private static void initializeDriver() {
        WebDriver webDriver;
        
        switch (BROWSER) {
            case "firefox":
                WebDriverManager.getInstance(DriverManagerType.FIREFOX).setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--start-maximized");
                webDriver = new FirefoxDriver(firefoxOptions);
                break;
                
            case "edge":
                WebDriverManager.getInstance(DriverManagerType.EDGE).setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                webDriver = new EdgeDriver(edgeOptions);
                break;
                
            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments(
                    "--start-maximized",
                    "--remote-allow-origins=*",
                    "--disable-notifications",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-features=FedCm"
                );
                // Faster page loads
                chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                // Optional headless from config.properties (default false)
                boolean headless = ConfigManager.getBooleanProperty("headless", "false");
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                // Reduce automation fingerprints
                chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                chromeOptions.setExperimentalOption("useAutomationExtension", false);
                webDriver = new ChromeDriver(chromeOptions);
                break;
        }
        
        // Avoid stacking implicit + explicit waits: set implicit to 0
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        // Use configurable page load timeout (default 60s)
        int plt = ConfigManager.getIntProperty("page.load.timeout", "60");
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(plt));
        driver.set(webDriver);
    }

    public static void unload() {
        if (driver.get() != null) {
            try {
                driver.get().quit();
            } finally {
                driver.remove();
            }
        }
    }
}
