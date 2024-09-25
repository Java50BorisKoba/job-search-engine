package com.danielkleyman.jobsearchlnk.service;

import com.danielkleyman.jobsearchapi.service.WriteToExcel;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
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
public class LnkService {

    private static final long SCHEDULED_TIME = 900000;
    private static final String SCHEDULED_TIME_STRING = "900";
    public static final Logger LOGGER = Logger.getLogger(LnkService.class.getName());
    public static final String EDGE_DRIVER_PATH = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedgedriver.exe";
    private static final String WEBSITE_NAME = "Linkedin";
    private final String MAIN_URL = String.format("https://www.linkedin.com/jobs/search?keywords=&location=Israel&geoId=101620260&f_TPR=r%s&position=1&pageNum=0", SCHEDULED_TIME_STRING);
    private final Map<String, List<String>> JOB_DETAILS = new LinkedHashMap<>();
    public static Set<String> alreadyAdded = new HashSet<>();

    public static int jobCount;
    private final ExtractJobDetails extractJobDetails; // Injected dependency

    @Autowired
    public LnkService(ExtractJobDetails extractJobDetails) {
        this.extractJobDetails = extractJobDetails; // Initialize injected dependency
        jobCount = 0;
    }

    @Scheduled(fixedRate = SCHEDULED_TIME)
    public void scheduledGetResults() {
        WebDriver localDriver = null;
        WebDriverWait localWait;
        try {
            localDriver = initializeWebDriver();
            localWait = new WebDriverWait(localDriver, Duration.ofSeconds(15)); // Увеличено время ожидания
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
        Scrolling scroller = new Scrolling(driver);
        long startTime = System.currentTimeMillis();
        try {
            openPage(driver, wait);
            printJobCount(driver);
            scroller.start();
            Thread.sleep((long) (jobCount * 0.6 * 1000));
            scroller.stop();
            LOGGER.info("Scrolling stopped");
            extractJobDetails.extractJobDetails(driver, wait, JOB_DETAILS);
            WriteToExcel.writeToExcel(JOB_DETAILS, WEBSITE_NAME);
            long endTime = System.currentTimeMillis();
            long totalTime = (endTime - startTime) / 1000;
            LOGGER.info("Extraction completed in " + totalTime + " seconds");
            LOGGER.info("Jobs parsed: " + JOB_DETAILS.size());
            JOB_DETAILS.clear();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } 
//        finally {
//            if (driver != null) {
//                driver.quit();
//            }
//        }
    }

    private void openPage(WebDriver driver, WebDriverWait wait) {
        boolean reload = true;
        while (reload) {
            driver.get(MAIN_URL);
            closePushWindow(driver);
            try {
                WebElement jobCounter = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".results-context-header__job-count")));

                if (jobCounter != null) {
                    LOGGER.info("Proceeding");
                    reload = false;
                }
            } catch (TimeoutException e) {
                try {
                    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"main-content\"]/section[1]/h1/strong")));
                    LOGGER.info("Waiting time before reloading page");
                    Thread.sleep(900000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOGGER.severe("Interrupted while waiting: " + ex.getMessage());
                } catch (TimeoutException ignored) {
                }
                LOGGER.info("Reloading page");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Interrupted while waiting: " + ex.getMessage());
            }
        }
    }
    private void closePushWindow(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        try {
            WebElement closeButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#base-contextual-sign-in-modal > div > section > button")));
            closeButton.click();
        } catch (TimeoutException e) {
        }
        ;
    }
    private WebDriver initializeWebDriver() {
        System.setProperty("webdriver.edge.driver", EDGE_DRIVER_PATH);

        EdgeOptions options = new EdgeOptions();
        options.setBinary("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        WebDriver driver = new EdgeDriver(options);
        driver.manage().window().maximize();

        return driver;
    }

    private void printJobCount(WebDriver driver) {
        WebElement jobCountElement = driver.findElement(By.cssSelector(".results-context-header__job-count"));
        String jobCountText = jobCountElement.getText().replace("+", "").replace(",", "");

        // Извлекаем только числовую часть из строки
        String numberOnly = jobCountText.replaceAll("\\D+", ""); // Удаляем все символы, кроме цифр

        try {
            jobCount = Integer.parseInt(numberOnly); // Преобразуем строку в число
        } catch (NumberFormatException e) {
            LOGGER.severe("Failed to parse job count: " + e.getMessage());
        }
        LOGGER.info("Job count: " + jobCount);
    }
}