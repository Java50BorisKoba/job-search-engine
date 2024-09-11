package com.danielkleyman.jobsearchalljobs.service;

import com.danielkleyman.jobsearchapi.service.AIService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ExtractJobDetails {
    public final AIService aiService;

    public ExtractJobDetails() {
        this.aiService = new AIService(new RestTemplate());
    }

    public boolean extractProcess(Page page, Map<String, List<String>>jobDetails) {
        Locator jobBoxesLocator = page.locator("//div[starts-with(@id, 'job-box-container')]");
        int numberOfJobBoxes = jobBoxesLocator.count();
        for (int i = 0; i < numberOfJobBoxes; i++) {
            Locator jobBox = jobBoxesLocator.nth(i);
            Locator jobTitleLocator = jobBox.locator("a.N");
            String jobTitle = jobTitleLocator.textContent(); // The text within <h2>
            String jobUrl = jobTitleLocator.getAttribute("href"); // The href attribute
            Locator companyNameLocator = jobBox.locator("a[ng-click^='ShowCompanyDetails']");
            String companyName = companyNameLocator.textContent();
            Locator cityLocator = jobBox.locator("a[href^='/SearchResultsGuest.aspx']");
            String city = cityLocator.textContent();
            Locator jobDescriptionLocator = jobBox.locator("div.job-content-top-desc");
            String jobDescription = jobDescriptionLocator.textContent();

            System.out.println("Job Title: " + jobTitle);
            System.out.println("Job URL: " + jobUrl);
            System.out.println("Company Name: " + companyName);
            System.out.println("City: " + city);
            System.out.println("Job Description: " + jobDescription);
            System.out.println("--------------------------------------------------");


        }
//        // Define the regex pattern to find the embedded JSON data
//        String regexPattern = "window.mosaic.providerData\\[\"mosaic-provider-jobcards\"\\]=(\\{.+?\\});";
//        Pattern pattern = Pattern.compile(regexPattern);
//        Matcher matcher = pattern.matcher(pageSource);
//        if (matcher.find()) {
//            String jsonString = matcher.group(1);
//            // Parse the JSON data
//            ObjectMapper objectMapper = new ObjectMapper();
//            try {
//                JsonNode rootNode = objectMapper.readTree(jsonString);
//                JsonNode resultsNode = rootNode.path("metaData").path("mosaicProviderJobCardsModel").path("results");
//                //  Print or process the extracted data
//                // System.out.println("Extracted Results Node:");
//                // System.out.println(resultsNode.toPrettyString());
//                // If you want to see individual job listings or specific fields, iterate through the resultsNode
//                if (resultsNode.isArray()) {
//                    detailsExtractor(resultsNode, jobDetails, jobDescriptionDriver, jobDescriptionWait);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            System.out.println("No embedded data found.");
//        }

        return false;
    }
private boolean isJobDateActual(Page page) {
    Locator element = page.locator("div.job-content-top-date");
    String textContent = element.textContent();
    System.out.println(textContent);
    return Objects.equals(textContent, "<div class=\"job-content-top-date\">1 ימים </div>");

}
//    private void detailsExtractor(JsonNode resultsNode, Map<String, List<String>> jobDetails, WebDriver jobDescriptionDriver, WebDriverWait jobDescriptionWait) throws InterruptedException, ExecutionException {
//        for (JsonNode jobNode : resultsNode) {
//            List<String> details = new ArrayList<>();
//            String jobKey = jobNode.path("jobkey").asText();
//            String url = "https://www.indeed.com/m/basecamp/viewjob?viewtype=embedded&jk=" + jobKey;
//            AllService.jobCount++;
//            // Extract and print job details
//            String jobTitle = jobNode.path("displayTitle").asText();
//            if (!checkCondition(url, jobTitle)) {
//                continue;
//            }
//            details.add(jobTitle);
//            Thread.sleep(AllService.randomTimeoutCalculation(2000, 3000));
//            // Execute the getJobDescription in a separate thread
//            Future<String> futureDescription = executorService.submit(new JobDescriptionTask(url, jobDescriptionDriver, jobDescriptionWait));
//            String jobDescription = futureDescription.get(); // This will block until the result is available
//            Thread.sleep(AllService.randomTimeoutCalculation(2000, 3000));
//            if (!descriptionCheck(jobDescription, jobTitle)) {
//                continue;
//            }
//            mapPopulationMethod(jobDescription, url, details, jobDetails, jobNode);
//        }
//    }
//
//    private void mapPopulationMethod(String jobDescription, String url, List<String> details, Map<String, List<String>> jobDetails, JsonNode jobNode) {
//        details.add(jobDescription);
//        String companyName = jobNode.path("company").asText();
//        details.add(companyName);
//        String city = jobNode.path("jobLocationCity").asText();
//        details.add(city);
//        jobDetails.putIfAbsent(url, details);
//        System.out.println("-------------------------------");
//    }
//
//    private boolean descriptionCheck(String jobDescription, String jobTitle) {
//        if (!filterDescription(jobDescription)) {
//            System.out.println("Job description excluded for job title: " + jobTitle);
//            return false;
//        }
//        String prompt = jobTitle + ". " + jobDescription;
//        int aiResponse = aiService.getResponse(prompt);
//        System.out.println(jobTitle + " gpt score = " + aiResponse);
//        return aiResponse >= 21; // Skip this job card if the extended text does not match filter criteria
//    }
//
//    private boolean checkCondition(String url, String jobTitle) {
//        if (AllService.urlAlreadyAdded.contains(url)) {
//            return false;
//        }
//        if (filterTitle(jobTitle)) {
//            System.out.println("Job title excluded: " + jobTitle);
//            return false;
//        }
//        System.out.println("Job Title: " + jobTitle);
//        return true;
//    }
//
//    private class JobDescriptionTask implements Callable<String> {
//        private final String url;
//        WebDriver driver;
//        WebDriverWait wait;
//
//        public JobDescriptionTask(String url, WebDriver jobDescriptionDriver, WebDriverWait jobDescriptionWait) {
//            this.url = url;
//            this.driver = jobDescriptionDriver;
//            this.wait = jobDescriptionWait;
//        }
//
//        @Override
//        public String call() throws Exception {
//            String jobDescriptionText = "";
//            driver.get(url);
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
//            String pageSource = driver.getPageSource();
//            String regexPattern = "_initialData=(\\{.+?\\});";
//            Pattern pattern = Pattern.compile(regexPattern);
//            Matcher matcher = pattern.matcher(pageSource);
//            if (matcher.find()) {
//                String jsonString = matcher.group(1);
//                ObjectMapper objectMapper = new ObjectMapper();
//                try {
//                    JsonNode rootNode = objectMapper.readTree(jsonString);
//                    JsonNode jobDataNode = rootNode.path("hostQueryExecutionResult").path("data").path("jobData").path("results").get(0).path("job").path("description");
//                    jobDescriptionText = jobDataNode.path("text").asText();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//                System.out.println("No embedded data found.");
//            }
//            return jobDescriptionText;
//        }
//    }

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
                "penetration", " investigations", "intelligence", "hrbp", "officer", "curriculum", " business", "team", "staff", "automation", "machine learning"
                , "mechanic", "ראש", "sr", "server", "writer", "בכיר", "בודק", "מנתח");
        // Check if any exclude keyword is present in the job title
        return excludeKeywords.stream()
                .anyMatch(jobTitle.toLowerCase()::contains);
    }

    private static boolean filterDescription(String aboutJob) {
        String aboutJob1 = aboutJob.toLowerCase();
        Set<String> includeKeywords = Set.of("java", "spring", "microservice", "react", "javascript", "oop",
                "typescript", "backend", "back-end", "back end", "fullstack", "full-stack", "full stack"
        );
        return includeKeywords.stream()
                .anyMatch(aboutJob1::contains);
    }
}
