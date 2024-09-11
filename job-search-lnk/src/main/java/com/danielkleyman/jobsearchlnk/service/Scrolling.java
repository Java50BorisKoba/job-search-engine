package com.danielkleyman.jobsearchlnk.service;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scrolling implements Runnable {

    private final WebDriver driver;
    private volatile boolean running = true; // Control flag for thread execution
    Map<String, String> jobDetails;
    private Thread scrollThread; // Reference to the scrolling thread

    public Scrolling(WebDriver driver) {
        this.driver = driver;
        this.jobDetails = new LinkedHashMap<>();
    }

    @Override
    public void run() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Define the step size for scrolling
        int stepSize = 800; // Example value in pixels
        while (running) {
            // Perform incremental scrolling and check for new content
            while (running) {
                // Check if the "See more jobs" button is present and click it
                List<WebElement> seeMoreButtons = driver.findElements(By.xpath("//button[contains(@class, 'infinite-scroller__show-more-button')]"));

                if (!seeMoreButtons.isEmpty()) {
                    WebElement seeMoreButton = seeMoreButtons.get(0);
                    if (seeMoreButton.isDisplayed() && seeMoreButton.isEnabled()) {
                        seeMoreButton.click();
                        System.out.println("Clicked 'See more jobs' button.");
                        try {
                            Thread.sleep(3000); // Wait for 3 seconds to allow content to load after clicking
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // Scroll down incrementally in steps
                js.executeScript("window.scrollBy(0, arguments[0]);", stepSize);
                js.executeScript("window.scrollBy(0, arguments[0]);", -stepSize / 3);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                js.executeScript("window.scrollBy(0, arguments[0]);", stepSize / 3);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Method to stop the thread
    public void stop() {
        running = false;
        if (scrollThread != null) {
            try {
                scrollThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
    }

    // Method to start the thread
    public void start() {

        scrollThread = new Thread(this);
        scrollThread.start();
    }
}






