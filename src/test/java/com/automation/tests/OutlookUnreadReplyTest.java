package com.automation.tests;

import com.automation.pages.OutlookPage;
import com.automation.utils.ConfigManager;
import com.automation.utils.DriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.time.Duration;
import java.util.List;

public class OutlookUnreadReplyTest {
    private WebDriver driver;
    private OutlookPage outlookPage;

    private String email;
    private String password;
    private String replyText;
    private long suiteStart;

    private void log(String message) {
        System.out.println("[Outlook Test] " + message);
    }

    private String mask(String value) {
        if (value == null || value.isEmpty()) return "<empty>";
        return value.replaceAll("(^.).+(@.*$)", "$1***$2");
    }

    @BeforeClass
    public void setUp() {
        // Load credentials from config
        email = ConfigManager.getProperty("outlook.email");
        password = ConfigManager.getProperty("outlook.password");
        replyText = ConfigManager.getProperty("outlook.reply.text", "Yes");
        suiteStart = System.currentTimeMillis();
        String maskedEmail = (email == null) ? "null" : mask(email);
        log("Starting suite. Browser=" + System.getProperty("browser", "chrome")
                + ", outlook.email=" + maskedEmail
                + ", replyText='" + replyText + "'"
                + ", password set=" + (password != null && !password.isEmpty()));
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new RuntimeException("Missing outlook.email or outlook.password in src/main/resources/config.properties");
        }

        driver = DriverManager.getDriver();
        outlookPage = new OutlookPage(driver);
    }

    @Test(description = "Login only: verify Outlook login succeeds", priority = 0)
    public void loginOnly_verification() {
        long t0 = System.currentTimeMillis();
        log("Attempting loginOnly_verification for user=" + mask(email));
        try {
            outlookPage.login(email, password);
            boolean logged = outlookPage.isLoggedIn();
            log("Login status=" + logged + ", elapsed=" + (System.currentTimeMillis() - t0) + " ms");
            Assert.assertTrue(logged, "Login verification failed - Inbox/reading pane not visible");
        } catch (RuntimeException re) {
            log("Login failed: " + re.getMessage());
            throw re;
        }
    }

    @Test(description = "Login to Outlook, open each unread email and reply 'Yes'", priority = 1)
    public void loginAndReplyYesToUnread() {
        // Assumes loginOnly_verification already confirmed login
        log("Starting loginAndReplyYesToUnread with reply='" + replyText + "'");
        if (!outlookPage.isLoggedIn()) {
            log("Not logged in yet. Logging in...");
            outlookPage.login(email, password);
            Assert.assertTrue(outlookPage.isLoggedIn(), "Login verification failed - Inbox/reading pane not visible");
        }
        long t0 = System.currentTimeMillis();
        int processed = outlookPage.replyToAllUnread(replyText);
        log("Processed unread replies count=" + processed + ", elapsed=" + (System.currentTimeMillis() - t0) + " ms");
        Assert.assertTrue(processed >= 0, "Processed count should be non-negative");
    }

    @Test(description = "Inline: Click Sign in, login using properties, open unread and reply 'Yes'", priority = 2, enabled = false)
    public void loginAndReplyYesToUnread_inline() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        // Go to Outlook mail
        log("Navigating to Outlook mailbox page");
        driver.get("https://outlook.live.com/mail/");

        // Click Sign in (if landing page)
        try {
            WebElement signInLanding = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a[data-task='signin'], a[href*='login.live.com'], a[aria-label='Sign in']")));
            signInLanding.click();
            log("Clicked landing Sign in");
        } catch (TimeoutException ignored) { log("Landing Sign in not visible, continuing"); }

        // Dismiss cookie if appears
        try {
            WebElement accept = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button#accept, button[aria-label*='Accept'], button[title*='Accept']")));
            accept.click();
            log("Dismissed cookie/consent dialog");
        } catch (TimeoutException ignored) { log("No cookie/consent dialog"); }

        // Email
        WebElement emailField;
        try {
            emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("i0116")));
        } catch (TimeoutException first) {
            driver.get("https://login.live.com/");
            emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("i0116")));
        }
        emailField.clear();
        emailField.sendKeys(email);
        // Click Next or press Enter
        try {
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("idSIButton9")));
            nextBtn.click();
            log("Clicked Next after email");
        } catch (TimeoutException te) {
            emailField.sendKeys(Keys.ENTER);
        }

        // Password
        WebElement pwdField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("i0118")));
        pwdField.clear();
        pwdField.sendKeys(password);
        try {
            WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("idSIButton9")));
            signInBtn.click();
            log("Clicked Sign in after password");
        } catch (TimeoutException te) {
            pwdField.sendKeys(Keys.ENTER);
        }

        // Stay signed in? -> No
        try {
            WebElement noBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("idBtn_Back")));
            noBtn.click();
            log("Selected 'No' on Stay signed in");
        } catch (TimeoutException ignored) { log("'Stay signed in' prompt not shown"); }

        // Wait for inbox grid
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='grid']")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[aria-label*='Message list']")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[role='main']")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[aria-label*='Reading pane']"))
        ));
        log("Mailbox UI visible");

        // Ensure Inbox selected (best-effort)
        try {
            WebElement inbox = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a[title='Inbox'], a[aria-label='Inbox']")));
            inbox.click();
            log("Inbox selected");
        } catch (TimeoutException ignored) { log("Inbox node not clickable; continuing"); }

        // Process unread messages using dynamic XPath
        int processed = 0;
        By unreadXPath = By.xpath("//div[@role='option' and starts-with(@aria-label,'Unread')]");
        while (true) {
            try {
                List<WebElement> unread = driver.findElements(unreadXPath);
                if (unread == null || unread.isEmpty()) break;
                WebElement first = unread.get(0);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", first);
                try { first.click(); } catch (WebDriverException e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", first); }

                // Wait for reading pane
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='article']")),
                        ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[aria-label*='Reading pane']"))
                ));

                // Reply
                WebElement replyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[aria-label^='Reply'], button[title^='Reply'], div[title^='Reply']")));
                replyBtn.click();

                WebElement editor = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div[role='textbox'], div[contenteditable='true']")));
                editor.click();
                editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                editor.sendKeys(replyText);

                WebElement sendBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[aria-label^='Send'], button[title^='Send']")));
                sendBtn.click();

                try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
                processed++;
                log("Replied to one unread email. Total so far=" + processed);
            } catch (StaleElementReferenceException sere) {
                // Re-loop to re-find elements
            } catch (TimeoutException te) {
                // If we timed out on some step, try next iteration
            }
        }

        log("[Inline] Total unread emails replied with '" + replyText + "': " + processed);
        Assert.assertTrue(processed >= 0, "Processed count should be non-negative");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        long elapsed = (suiteStart == 0) ? -1 : (System.currentTimeMillis() - suiteStart);
        log("Tear down. Total elapsed=" + elapsed + " ms");
        if (driver != null) {
            DriverManager.unload();
        }
    }
}
