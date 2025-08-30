package com.automation.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class OutlookPage {
    private final WebDriver driver;
    private final WebDriverWait wait;


    private final By usernameField = By.xpath("//label[text()='Email or phone number']");
    private final By NextButton = By.xpath("//button[text()='Next']");

    private void log(String msg) { System.out.println("[OutlookPage] " + msg); }

    public void Enterusername(String Email) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField)).sendKeys(Email);
    }
    public void ClickNextButton() {
        wait.until(ExpectedConditions.elementToBeClickable(NextButton)).click();
    }

    public OutlookPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public void login(String email, String password) {
        try {
            log("Navigating to OAuth entry page");
            driver.get("https://outlook.live.com/owa/?nlp=1");
            log("Trying to click a visible 'Sign in' entrypoint on landing page");
            try {
                clickIfPresent(
                        By.cssSelector("a[data-task='signin']"),
                        By.cssSelector("a[aria-label='Sign in']"),
                        By.cssSelector("a[href*='login.live.com']"),
                        By.xpath("//a[contains(.,'Sign in') or contains(@aria-label,'Sign in')]")
                );
            } catch (Exception ignored) { }

            log("Looking for account tiles matching email");
            try {
                List<WebElement> tiles = driver.findElements(By.cssSelector("div[role='button'][data-username], div.tile[role='button'], div[role='button'][data-test-id*='account']"));
                for (WebElement tile : tiles) {
                    String dataUser = tile.getAttribute("data-username");
                    if (dataUser != null && dataUser.toLowerCase().contains(email.toLowerCase())) {
                        try { tile.click(); } catch (WebDriverException e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tile); }
                        break;
                    }
                }
            } catch (Exception ignored) { }

            log("Waiting for Microsoft login domain");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(20))
                        .until(d -> d.getCurrentUrl().contains("login.live.com") || d.getCurrentUrl().contains("login.microsoftonline.com"));
            } catch (Exception ignored) { }

            log("Trying to switch to frame containing email field");
            trySwitchToFrameContaining(
                    By.id("i0116"), By.name("loginfmt"), By.cssSelector("input[type='email']"), By.id("usernameEntry")
            );

            log("Waiting for email field to be visible");
            WebElement emailField;
            try {
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(7))
                            .until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("i0116")),
                                    ExpectedConditions.visibilityOfElementLocated(By.name("loginfmt"))
                            ));
                } catch (TimeoutException fastMiss) {
                    log("Email field not visible quickly; navigating directly to login.live.com");
                    driver.get("https://login.live.com/");
                }
                emailField = waitAnyVisible(
                        By.id("i0116"),
                        By.name("loginfmt"),
                        By.cssSelector("input[type='email']"),
                        By.id("usernameEntry")
                );
                try { emailField.click(); } catch (WebDriverException e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", emailField); }
                emailField.clear();
                log("Setting email value");
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input',{bubbles:true})); arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                            emailField, email);
                } catch (Exception jsEx) {
                    emailField.sendKeys(email);
                }
                log("Clicking Next after email");
                try {
                    WebElement nextBtn = new WebDriverWait(driver, Duration.ofSeconds(15))
                            .until(ExpectedConditions.elementToBeClickable(
                                    By.id("idSIButton9")
                            ));
                    try { nextBtn.click(); } catch (WebDriverException e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextBtn); }
                } catch (TimeoutException te) {
                    log("Primary Next not clickable; using fallbacks or Enter");
                    if (!clickIfPresent(
                            By.cssSelector("button[type='submit']"),
                            By.xpath("//button[.='Next' or .//span[text()='Next']]"),
                            By.xpath("//input[@type='submit' and (@value='Next' or @id='idSIButton9')]")
                    )) {
                        emailField.sendKeys(Keys.ENTER);
                    }
                }
                log("Waiting for password field to appear");
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(25))
                            .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password'], #i0118")));
                } catch (TimeoutException te) {
                    log("Password did not appear; retrying email step");
                    try {
                        WebElement ef = waitAnyVisible(By.id("i0116"), By.name("loginfmt"));
                        ef.clear();
                        try {
                            ((JavascriptExecutor) driver).executeScript(
                                    "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input',{bubbles:true})); arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                                    ef, email);
                        } catch (Exception js2) { ef.sendKeys(email); }
                        clickIfPresent(By.id("idSIButton9"));
                        new WebDriverWait(driver, Duration.ofSeconds(20))
                                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password'], #i0118")));
                    } catch (Exception ignoredRetry) { }
                }
            } catch (TimeoutException ignored) { /* might have selected account tile already */ }

            log("Handling account type prompt if present");
            clickIfPresent(By.xpath("//div[@role='button' and .//div[text()='Personal account']]"),
                    By.xpath("//div[@role='button' and .//div[contains(text(),'Personal')]]"));

            log("Filling password and continuing");
            WebElement pwdField = waitAnyVisible(
                    By.id("i0118"),
                    By.cssSelector("input[type='password']"),
                    By.id("passwordInput")
            );
            pwdField.clear();
            pwdField.sendKeys(password);
            if (!clickIfPresent(By.id("idSIButton9"), By.cssSelector("input[type='submit']"))) {
                pwdField.sendKeys(Keys.ENTER);
            }

            log("Handling 'Stay signed in' prompt if present");
            if (!clickIfPresent(By.id("idBtn_Back"))) {
                clickIfPresent(By.id("idSIButton9")); // sometimes Yes continues the flow
            }

            try { driver.switchTo().defaultContent(); } catch (Exception ignored) { }

            log("Navigating to Outlook mailbox UI");
            driver.get("https://outlook.live.com/mail/");

            log("Dismissing cookie/consent dialog if present");
            dismissIfClickable(By.cssSelector("button#accept, button[aria-label*='Accept'], button[title*='Accept']"));

            log("Waiting for mailbox UI to be visible");
            waitAnyVisible(
                    By.cssSelector("[role='grid']"),
                    By.cssSelector("div[aria-label*='Message list']"),
                    By.cssSelector("div[role='main']"),
                    By.cssSelector("div[aria-label*='Reading pane']")
            );
            log("Mailbox UI visible; login complete");
        } catch (Exception e) {
            throw new RuntimeException("Outlook login failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simple check to verify Inbox/reading pane is visible, indicating a logged-in state.
     */
    public boolean isLoggedIn() {
        try {
            waitAnyVisible(
                    By.cssSelector("[role='grid']"),
                    By.cssSelector("div[aria-label*='Message list']"),
                    By.cssSelector("div[role='main']"),
                    By.cssSelector("div[aria-label*='Reading pane']")
            );
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    public int replyYesToAllUnread() {
        try {
            return replyToAllUnread("Yes");
        } catch (Exception e) {
            throw new RuntimeException("Failed replying to unread: " + e.getMessage(), e);
        }
    }

    /**
     * Reply to all unread messages with the provided reply text.
     * @param replyText text to send in the reply
     * @return number of messages processed
     */
    public int replyToAllUnread(String replyText) {
        try {
            // Ensure Inbox selected
            try {
                WebElement inbox = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[title='Inbox'], a[aria-label='Inbox']")));
                inbox.click();
            } catch (TimeoutException ignored) { }

            // Collect unread rows
            List<WebElement> unreadRows = driver.findElements(By.cssSelector(
                    "[role='row'][data-is-read='false'], [aria-label*='Unread message'], div[aria-label*='Unread']"));

            int processed = 0;
            // Optional cap: stop after N messages if property is set
            int maxToProcess = Integer.MAX_VALUE;
            try {
                String capStr = com.automation.utils.ConfigManager.getProperty("outlook.max.unread.process");
                if (capStr != null && !capStr.isEmpty()) {
                    maxToProcess = Integer.parseInt(capStr);
                }
            } catch (Exception ignored) { }
            for (int i = 0; i < unreadRows.size(); i++) {
                // Re-fetch rows each iteration to avoid stale elements
                List<WebElement> rows = driver.findElements(By.cssSelector(
                        "[role='row'][data-is-read='false'], [aria-label*='Unread message'], div[aria-label*='Unread']"));
                if (rows.isEmpty()) break;
                WebElement row = rows.get(0); // always pick the first unread
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", row);
                row.click();

                // Wait for reading pane
                waitAnyVisible(
                        By.cssSelector("[role='article']"),
                        By.cssSelector("div[aria-label*='Reading pane']")
                );

                // Click Reply
                WebElement replyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[aria-label^='Reply'], button[title^='Reply'], div[title^='Reply']")));
                replyBtn.click();

                // Type in editor (compose box)
                WebElement editor = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div[role='textbox'], div[contenteditable='true']")));
                editor.click();
                editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                editor.sendKeys(replyText);

                // Send
                WebElement sendBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[aria-label^='Send'], button[title^='Send']")));
                sendBtn.click();

                // Wait for the unread row to disappear or be marked read (faster than fixed sleep)
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.or(
                                    ExpectedConditions.stalenessOf(row),
                                    ExpectedConditions.invisibilityOf(row),
                                    d -> {
                                        try { return !row.getAttribute("data-is-read").equals("false"); }
                                        catch (Exception e) { return true; }
                                    }
                            ));
                } catch (TimeoutException ignored) { }
                processed++;
                if (processed >= maxToProcess) break;
            }
            return processed;
        } catch (Exception e) {
            throw new RuntimeException("Failed replying to unread: " + e.getMessage(), e);
        }
    }

    private WebElement waitAnyVisible(By... locators) {
        // Use a single combined wait to check all locators, avoiding 30s per locator
        return new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            for (By by : locators) {
                try {
                    List<WebElement> els = d.findElements(by);
                    for (WebElement el : els) {
                        if (el.isDisplayed()) {
                            return el;
                        }
                    }
                } catch (StaleElementReferenceException ignored) { }
            }
            return null; // keep waiting
        });
    }

    private void clickAny(By... locators) {
        if (!clickIfPresent(locators)) {
            throw new TimeoutException("No clickable element found for provided locators");
        }
    }

    private boolean clickIfPresent(By... locators) {
        // Short per-locator timeout to prevent long cumulative waits
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        for (By by : locators) {
            try {
                WebElement el = shortWait.until(ExpectedConditions.elementToBeClickable(by));
                try {
                    el.click();
                } catch (WebDriverException e) { // fallback to JS click on any WebDriver click issue
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                }
                return true;
            } catch (TimeoutException ignored) { }
        }
        return false;
    }

    private void dismissIfClickable(By by) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
            el.click();
        } catch (TimeoutException ignored) { }
    }
    /**
     * Try to switch into the first iframe/frame that contains any of the provided locators.
     * If none found, remain on default content.
     */
    private void trySwitchToFrameContaining(By... locators) {
        try {
            // Ensure starting from top
            driver.switchTo().defaultContent();
        } catch (Exception ignored) { }
        List<WebElement> frames = driver.findElements(By.cssSelector("iframe, frame"));
        for (WebElement frame : frames) {
            try {
                driver.switchTo().frame(frame);
                boolean found = false;
                for (By by : locators) {
                    if (!driver.findElements(by).isEmpty()) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    return; // stay in this frame
                }
            } catch (NoSuchFrameException | StaleElementReferenceException ignored) {
            } finally {
                try { driver.switchTo().defaultContent(); } catch (Exception ignored2) { }
            }
        }
        // If not found, ensure we're at default content
        try { driver.switchTo().defaultContent(); } catch (Exception ignored) { }
    }
}
