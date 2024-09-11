package com.danielkleyman.jobsearchalljobs.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ViewportSize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AllService {

    private static final long SCHEDULED_TIME = 86400000;
    public static final Logger LOGGER = Logger.getLogger(AllService.class.getName());
    private static final String WEBSITE_NAME = "AllJobs";
    public static final String CHROME_DRIVER_PATH = System.getenv("CHROME_DRIVER_PATH");
    private final String URL = "https://www.alljobs.co.il/SearchResultsGuest.aspx?page=1&position=&type=&city=&region=";
    private final Map<String, List<String>> JOB_DETAILS = new LinkedHashMap<>();
    private final ExtractJobDetails extractJobDetails; // Injected dependency
    public static List<String> urlAlreadyAdded = new ArrayList<>();
    public static int jobCount;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;

    @Autowired
    public AllService(ExtractJobDetails extractJobDetails) {

        this.extractJobDetails = extractJobDetails; // Initialize injected dependency
        jobCount = 0;
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of(
                        "--incognito",
                        "--disable-blink-features=AutomationControlled",
                        "--disable-infobars",
                        "--disable-extensions",
                        "--disable-popup-blocking"
                )));// Add the --incognito argument
        this.context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(new ViewportSize(1920, 1080))); // Set viewport size
    }

    @Scheduled(fixedRate = SCHEDULED_TIME)
    public void scheduledGetResults() {
        try (Page page = browser.newPage()) {
            getResults(page);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during scheduled task", e);
        }
    }

    public void getResults(Page page) {
        jobCount = 0;
        long startTime = System.currentTimeMillis();
        page.navigate(URL);
        try {
            Thread.sleep(randomTimeoutCalculation(4000, 8000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            try {
                if (!extractJobDetails.extractProcess(page, JOB_DETAILS)) {
                    break;
                }
                LOGGER.info("Jobs found current: " + jobCount);
                clickNextPage(page);
                Thread.sleep(randomTimeoutCalculation(4000, 8000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //   WriteToExcel.writeToExcel(JOB_DETAILS, WEBSITE_NAME);
        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime) / 1000;
        LOGGER.info("Extraction completed in " + totalTime + " seconds");
        LOGGER.info("Jobs found finally: " + jobCount);
        LOGGER.info("Jobs parsed: " + JOB_DETAILS.size());
        JOB_DETAILS.clear();
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    public void clickNextPage(Page page) {
        try {
            // Wait until the "Next Page" button is clickable
            Locator nextPageButton = page.locator("//*[@id='divResults']/div[9]/div/div/div[1]/a");
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
}

