package com.automation.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExtentTestManager {
    static Map<Integer, ExtentTest> extentTestMap = new HashMap<>();
    private static ExtentReports extent;
    private static final String dateName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    private static final String REPORT_PATH = System.getProperty("user.dir") + "/test-output/ExtentReport/" + dateName + ".html";

    public synchronized static ExtentReports setUpExtentReport() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
            spark.config().setDocumentTitle("Automation Test Report");
            spark.config().setReportName("Hybrid Framework Test Report");
            spark.config().setTheme(Theme.STANDARD);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Framework", "Selenium WebDriver");
            extent.setSystemInfo("Author", "Automation Team");
        }
        return extent;
    }

    public static synchronized ExtentTest startTest(String testName) {
        ExtentTest test = extent.createTest(testName);
        extentTestMap.put((int) Thread.currentThread().getId(), test);
        return test;
    }

    public static synchronized ExtentTest getTest() {
        return extentTestMap.get((int) Thread.currentThread().getId());
    }

    public static synchronized void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}
