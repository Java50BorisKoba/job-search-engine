package com.danielkleyman.jobsearchind.service;

import com.danielkleyman.jobsearchapi.service.WriteToExcel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class IndService {

    private static final long SCHEDULED_TIME = 86400000;
    public static final Logger LOGGER = Logger.getLogger(IndService.class.getName());
    public static final String CHROME_DRIVER_PATH = System.getenv("CHROME_DRIVER_PATH");
    private static final String WEBSITE_NAME = "Indeed";
    private final String URL = "https://il.indeed.com/jobs?q=&l=israel&fromage=1&vjk=087662bdbb912e05";
    private final Map<String, List<String>> JOB_DETAILS = new LinkedHashMap<>();
    private final ExtractJobDetails extractJobDetails; // Injected dependency
    public static List<String> urlAlreadyAdded = new ArrayList<>();
    public static int jobCount;

    @Autowired
    public IndService(ExtractJobDetails extractJobDetails) {
        this.extractJobDetails = extractJobDetails; // Initialize injected dependency
        jobCount = 0;
    }

    @Scheduled(fixedRate = SCHEDULED_TIME)
    public void scheduledGetResults() {
        WebDriver localDriver = null;
        WebDriverWait localWait;
        try {
            localDriver = initializeWebDriver();
            localWait = new WebDriverWait(localDriver, Duration.ofSeconds(10));
            getResults(localDriver, localWait);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during scheduled task", e);
        } finally {
            if (localDriver != null) {
                localDriver.quit();
            }
        }
    }

    public void getResults(WebDriver driver, WebDriverWait wait) {
        jobCount = 0;
        long startTime = System.currentTimeMillis();
        WebDriver jobDescriptionDriver = initializeWebDriver();
        WebDriverWait jobDescriptionWait = new WebDriverWait(jobDescriptionDriver, Duration.ofSeconds(10));
        driver.get(URL);
        try {
            while (isNextPageButtonVisible(wait)) {
                Thread.sleep(randomTimeoutCalculation(4000, 8000));
                extractJobDetails.extractProcess(driver, JOB_DETAILS, jobDescriptionDriver, jobDescriptionWait);
                Thread.sleep(randomTimeoutCalculation(4000, 8000));
                LOGGER.info("Jobs found: " + jobCount);
                clickNextPage(wait);
            }
            WriteToExcel.writeToExcel(JOB_DETAILS, WEBSITE_NAME);
            long endTime = System.currentTimeMillis();
            long totalTime = (endTime - startTime) / 1000;
            LOGGER.info("Extraction completed in " + totalTime + " seconds");
            LOGGER.info("Jobs found: " + jobCount);
            LOGGER.info("Jobs parsed: " + JOB_DETAILS.size());
            JOB_DETAILS.clear();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (driver != null) {
                driver.quit();
                jobDescriptionDriver.quit();
            }
        }
    }

    private boolean isNextPageButtonVisible(WebDriverWait wait) {
        try {
            WebElement nextPageButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[data-testid='pagination-page-next']")));
            return nextPageButton.isDisplayed();
        } catch (Exception e) {
            return false;  // If the element is not found or not visible
        }
    }

    public void clickNextPage(WebDriverWait wait) {
        try {
            // Wait until the "Next Page" button is clickable
            WebElement nextPageButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[data-testid='pagination-page-next']")));
            // Click on the "Next Page" button
            nextPageButton.click();
            System.out.println("Navigated to the next page.");
        } catch (Exception e) {
            System.err.println("Failed to click on the Next Page button: " + e.getMessage());
        }
    }

    public static long randomTimeoutCalculation(long min, long max) {
        Random random = new Random();
        // Generate a random long value between 3000 and 6000 milliseconds
        return min + random.nextInt((int) (max - min + 1));
    }

    private WebDriver initializeWebDriver() {
        if (CHROME_DRIVER_PATH == null) {
            throw new IllegalStateException("CHROME_DRIVER_PATH environment variable not set");
        }
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        return driver;
    }
}

