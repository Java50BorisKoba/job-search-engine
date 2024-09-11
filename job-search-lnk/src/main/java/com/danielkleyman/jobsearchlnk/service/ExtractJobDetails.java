package com.danielkleyman.jobsearchlnk.service;

import com.danielkleyman.jobsearchapi.service.AIService;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.*;
import java.util.concurrent.*;

import static com.danielkleyman.jobsearchlnk.service.LnkService.LOGGER;


@Component
public class ExtractJobDetails {
    public final AIService aiService;
    int jobsVisibleOnPage;

    public ExtractJobDetails() {
        this.aiService = new AIService(new RestTemplate());
        this.jobsVisibleOnPage = 0;
    }

    public void extractJobDetails(WebDriver driver, WebDriverWait wait, Map<String, List<String>> jobDetails) {

        extractProcess(driver, wait, jobDetails);

    }

    private void extractProcess(WebDriver driver, WebDriverWait wait, Map<String, List<String>> jobDetails) {

        System.out.println("Waiting for page to load...");

        try {
            System.out.println("Searching for job container");
            // Wait until the job container is visible
            WebElement jobContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[contains(@class, 'jobs-search__results-list')]")));
            System.out.println("Job container is found");
            // Find all job cards on the page
            List<WebElement> jobCards = driver.findElements(By.xpath("//div[contains(@class, 'job-search-card')]"));
            if (jobCards.isEmpty()) {
                System.err.println("No job cards found.");
                return;
            }
            System.out.println("Job cards are found");
            jobCardsParsing(driver, wait, jobDetails, jobCards);

        } catch (TimeoutException e) {
            System.err.println("Timeout while waiting for job container: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
        System.out.println("Jobs visible: " + jobsVisibleOnPage);
        jobsVisibleOnPage = 0;
    }

    private void jobCardsParsing(WebDriver driver, WebDriverWait wait, Map<String, List<String>> jobDetails, List<WebElement> jobCards) throws java.util.concurrent.TimeoutException {
        for (int i = 0; i < jobCards.size(); i++) {
            int timeout = LnkService.jobCount * 10000;
            final int index = i;
            boolean stopParsing = executeWithTimeout(() -> singleCardParsing(jobCards, index, driver, wait, jobDetails), timeout);
            if (!stopParsing) {
                continue;
            }
        }
    }

    private boolean singleCardParsing(List<WebElement> jobCards, int i, WebDriver driver, WebDriverWait wait, Map<String, List<String>> jobDetails) {
        jobsVisibleOnPage++;
        WebElement jobCard = jobCards.get(i);
        System.out.println("get card number: " + i);
        List<String> url = new ArrayList<>();
        List<String> details = new ArrayList<>();
        try {
            // Extract job title and URL
            boolean extractionTitleAndUrl = executeWithTimeout(() -> extractTitleAndUrl(jobCard, details, url), 20000);
            if (!extractionTitleAndUrl) {
                return false; // Skip to the next job card if extraction failed
            }
            String jobUrl = url.get(0);
            // Skip processing if URL has already been processed
            int positionIndex = jobUrl.indexOf("position");
            String extractedPart = jobUrl.substring(0, positionIndex + "position".length());
            if (LnkService.alreadyAdded.contains(extractedPart)) {
                System.out.println("URL already been added: " + jobUrl);
                return false; // Continue with the next job card
            }
            showExpandedContent(wait, i, jobCard, jobCards);
            boolean extractionExpandedContent = extractExpandedContent(driver, details);
            if (!extractionExpandedContent) {
                return false; // Skip to the next job card if extraction failed
            }
            // Extract company name
            extractCompanyName(details, wait);
            // Extract city
            extractCity(details, wait);

            // Add job details to the map
            jobDetails.putIfAbsent(jobUrl, details);
            LnkService.alreadyAdded.add(extractedPart);
        } catch (Exception e) {
            System.err.println("Unexpected error extracting details from job card: " + e.getMessage());
        }
        return true;
    }

    private boolean executeWithTimeout(Callable<Boolean> callable, long timeoutMillis) throws TimeoutException, java.util.concurrent.TimeoutException {
        int maxRetries = 3;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Future<Boolean> future = executor.submit(callable);

            try {
                return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | java.util.concurrent.TimeoutException e) {
                if (attempt == maxRetries) {
                    // If the last attempt fails, propagate the TimeoutException
                    throw e;
                }
                System.out.println("Retry attempt " + attempt + " failed due to timeout, retrying...");
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.severe("Exception while executing callable: " + e.getMessage());
                return false;
            } finally {
                future.cancel(true); // Optionally cancel the future if it's still running
            }
        }

        executor.shutdownNow(); // Ensure that the executor is shut down after all retries
        return false;
    }

    private void extractCity(List<String> details, WebDriverWait wait) {
        try {
            WebElement cityElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.topcard__flavor.topcard__flavor--bullet")));
            String city = cityElement.getText().trim();
            details.add(city);
        } catch (NoSuchElementException | TimeoutException e) {
            System.err.println("City element not found: " + e.getMessage());
            details.add("City not available");
        }
    }

    private void extractCompanyName(List<String> details, WebDriverWait wait) {
        try {
            WebElement companyElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a.topcard__org-name-link")));
            String companyName = companyElement.getText();
            details.add(companyName);
        } catch (NoSuchElementException | TimeoutException e) {
            System.err.println("Company name element not found: " + e.getMessage());
            details.add("Company name not available");
        }
    }

    private boolean extractExpandedContent(WebDriver driver, List<String> details) {
        // Use a shorter wait for the expanded content
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(1));
        WebElement expandedContent = null;
        try {
            expandedContent = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.show-more-less-html__markup")));
            String extendedText = expandedContent.getText();
            if (!filterDescription(extendedText)) {
                System.out.println("Extended text excluded for job: " + details.get(0));
                return false; // Skip this job card if the extended text does not match filter criteria
            }
            String prompt = details.get(0) + ". " + extendedText;
            int aiResponse = aiService.getResponse(prompt);
            System.out.println(" " + details.get(0) + " gpt score = " + aiResponse);
            System.out.println("----------------------------------------");
            if (aiResponse < 21) {
                //   System.out.println("text for job title:  " + details.get(0) + " excluded by ai = " + aiResponse);
                return false; // Skip this job card if the extended text does not match filter criteria
            }
            details.add(extendedText);
            //                    System.out.println("Extended text added: " + extendedText);
        } catch (TimeoutException e) {
            System.err.println("Extended content not found within 1 second.");
            details.add("Extended text not available");
        }
        return true;
    }

    private void showExpandedContent(WebDriverWait wait, int i, WebElement jobCard, List<WebElement> jobCards) {
        boolean showMoreVisible = false;
        while (!showMoreVisible) {
            // Click on the job card to expand it
            jobCard.click();
            try {
                // Try to find and click the "Show more" button
                WebElement showMoreButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button.show-more-less-html__button")));
                showMoreButton.click();
                showMoreVisible = true; // Exit loop if the button was clicked
                System.out.println("Clicked 'Show more' button.");
            } catch (NoSuchElementException | TimeoutException e) {
                // If the "Show more" button is not found, collapse the previous job card if possible
                if (i > 0) {
                    WebElement previousJobCard = jobCards.get(i - 1);
                    previousJobCard.click(); // Collapse the previous job card
                    System.out.println("Collapsed previous job card.");
                }
                // Retry the current job card by clicking it again
                jobCard.click();
            }
        }
    }

    private boolean extractTitleAndUrl(WebElement jobCard, List<String> details, List<String> url) {
        String title = "";
        try {
            WebElement titleElement = jobCard.findElement(By.xpath(".//h3[contains(@class, 'base-search-card__title')]"));
            title = titleElement.getText();
            if (!filterTitle(title.toLowerCase())) {
                System.out.println("Job title excluded: " + title);
                return false; // Skip this job card if title matches filter criteria
            }
            details.add(title);

            WebElement urlElement = jobCard.findElement(By.cssSelector("a.base-card__full-link"));
            String urlAddress = urlElement.getAttribute("href");
            url.add(urlAddress);
        } catch (NoSuchElementException e) {
            System.err.println("Job title or URL element not found in a job card.");
            return false; // Skip this job card if title or URL is missing
        }
        return true;
    }

    private static boolean filterTitle(String jobTitle) {
        Set<String> excludeKeywords = Set.of(
                "lead", "leader", "devops", "manager", "qa", "mechanical", "infrastructure", "integration", "civil",
                "principal", "customer", "embedded", "system", " verification", "electrical", "support", "complaint", "solution", "solutions", "simulation", "technical",
                "manufacturing", "validation", "finops", "hardware", "devsecops", "motion", "machine Learning", "design", "sr.", "quality", "architect", "head",
                "director", "president", "executive", "detection", "industrial", "chief", "specialist", "algorithm", "architecture", "admin", " researcher",
                " data science", "webmaster", "medical", "associate", "mrb", "accountant", "waiter", "dft", "test", "musicologist", "sales", "media", "product",
                "reliability", "account", "representative", "Architect", "Analyst", "Account", "Executive", "Specialist", "Associate", "devtest", "big data", "digital",
                "coordinator", "intern", "researcher", "network", "security", "malware", " intelligence", " algo-dev", "electro-optics", "secops", "implementer",
                "ml", "picker", "revenue", "controller", "פלנר", "טכנאי", "emulation", "tester", "counsel", "administrative", "assistant", "production", " scientist",
                "penetration", " investigations", "מנהל", "intelligence", "hrbp", "officer", "curriculum", " business", "team", "staff", "automation", "machine learning"
                , "mechanic", "ראש", "writer");
        Set<String> includeKeywords = Set.of(
                "developer", "engineer", "programmer", "backend", "back-end", "back end", "fullstack", "full-stack", "full stack",
                "software", "fs", "java", "מתחנת", "מפתח"
        );

        // Check if any exclude keyword is present in the job title
        boolean shouldExclude = excludeKeywords.stream()
                .anyMatch(jobTitle.toLowerCase()::contains);

        // Check if any include keyword is present in the job title
//        boolean shouldInclude = includeKeywords.stream()
//                .anyMatch(keyword -> jobTitle.contains(keyword));
// Check if any include keyword is present in the job title
        boolean shouldInclude = includeKeywords.stream()
                .anyMatch(keyword -> jobTitle.toLowerCase().contains(keyword.toLowerCase()));
        //   return !shouldExclude && shouldInclude;
        return !shouldExclude;
    }

    private static boolean filterDescription(String aboutJob) {
        String aboutJob1 = aboutJob.toLowerCase();
        Set<String> includeKeywords = Set.of("java", "spring", "microservice", "react", "javascript", "oop",
                "typescript", "backend", "back-end", "back end", "מפתח", "מתכנת", "fullstack", "full-stack", "full stack"
        );

        return includeKeywords.stream()
                .anyMatch(aboutJob1::contains);

    }
}
