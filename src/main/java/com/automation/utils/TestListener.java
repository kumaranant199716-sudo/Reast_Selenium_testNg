package com.automation.utils;

import com.aventstack.extentreports.Status;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import static com.automation.utils.ExtentTestManager.*;

public class TestListener implements ITestListener {
    
    @Override
    public void onStart(ITestContext context) {
        ExtentTestManager.setUpExtentReport();
    }

    @Override
    public void onTestStart(ITestResult result) {
        startTest(result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getTest().log(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        getTest().log(Status.FAIL, result.getThrowable());
        // Add screenshot on failure
        String base64Screenshot = ScreenshotUtils.getBase64Screenshot();
        getTest().addScreenCaptureFromBase64String(base64Screenshot, "Test Failure");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        getTest().log(Status.SKIP, result.getThrowable());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentTestManager.flushReport();
    }
}
