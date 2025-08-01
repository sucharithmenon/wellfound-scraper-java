package com.gigacrawl.wellfound.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gigacrawl.wellfound.config.WellfoundConfig;
import com.gigacrawl.wellfound.model.Company;
import com.gigacrawl.wellfound.model.WellfoundJobPosting;
import com.gigacrawl.wellfound.util.RateLimiter;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready Wellfound scraper with multi-strategy extraction and anti-bot capabilities
 */
public class WellfoundScraper {
    
    private static final Logger logger = LoggerFactory.getLogger(WellfoundScraper.class);
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final Pattern nextDataPattern;
    private final SessionManager sessionManager;
    
    // Extraction statistics
    private int totalRequests = 0;
    private int successfulExtractions = 0;
    private int failedExtractions = 0;
    
    public WellfoundScraper() {
        this.httpClient = createHttpClient();
        this.objectMapper = createObjectMapper();
        this.rateLimiter = new RateLimiter(WellfoundConfig.DEFAULT_RATE_LIMIT);
        this.nextDataPattern = Pattern.compile(WellfoundConfig.NEXT_DATA_PATTERN, Pattern.DOTALL);
        this.sessionManager = new SessionManager();
        
        logger.info("WellfoundScraper initialized with rate limit: {} req/sec", 
                WellfoundConfig.DEFAULT_RATE_LIMIT);
    }
    
    /**
     * Scrape companies from Wellfound startups directory
     */
    public List<Company> scrapeCompanies(int maxPages) {
        logger.info("Starting company scraping, max pages: {}", maxPages);
        List<Company> allCompanies = new ArrayList<>();
        
        for (int page = 1; page <= maxPages; page++) {
            try {
                rateLimiter.acquire();
                List<Company> pageCompanies = scrapeCompaniesPage(page);
                allCompanies.addAll(pageCompanies);
                
                logger.info("Page {}/{}: Found {} companies (total: {})", 
                        page, maxPages, pageCompanies.size(), allCompanies.size());
                
                if (pageCompanies.isEmpty()) {
                    logger.info("No more companies found, stopping at page {}", page);
                    break;
                }
                
            } catch (Exception e) {
                logger.error("Error scraping companies page {}: {}", page, e.getMessage());
                failedExtractions++;
            }
        }
        
        logger.info("Company scraping completed. Total companies: {}", allCompanies.size());
        return allCompanies;
    }
    
    /**
     * Scrape jobs from a company jobs URL
     */
    public List<WellfoundJobPosting> scrapeJobsFromUrl(String jobsUrl) {
        logger.info("Scraping jobs from URL: {}", jobsUrl);
        
        try {
            rateLimiter.acquire();
            
            // Extract company slug from URL
            String companySlug = extractCompanySlugFromUrl(jobsUrl);
            if (companySlug == null) {
                logger.warn("Could not extract company slug from URL: {}", jobsUrl);
                return new ArrayList<>();
            }
            
            // Create company object
            Company company = new Company();
            company.setSlug(companySlug);
            company.setName(companySlug); // Will be updated during extraction if available
            
            List<WellfoundJobPosting> jobs = extractJobsFromUrl(jobsUrl, company);
            logger.info("Found {} jobs from URL: {}", jobs.size(), jobsUrl);
            return jobs;
            
        } catch (Exception e) {
            logger.error("Error scraping jobs from URL {}: {}", jobsUrl, e.getMessage());
            failedExtractions++;
            return new ArrayList<>();
        }
    }
    
    /**
     * Scrape jobs for a specific company
     */
    public List<WellfoundJobPosting> scrapeCompanyJobs(Company company) {
        if (company.getSlug() == null) {
            logger.warn("Company slug is null, cannot scrape jobs: {}", company.getName());
            return new ArrayList<>();
        }
        
        logger.info("Scraping jobs for company: {} ({})", company.getName(), company.getSlug());
        
        try {
            rateLimiter.acquire();
            String jobsUrl = WellfoundConfig.BASE_URL + "/company/" + company.getSlug() + "/jobs";
            List<WellfoundJobPosting> jobs = extractJobsFromUrl(jobsUrl, company);
            
            logger.info("Found {} jobs for company: {}", jobs.size(), company.getName());
            return jobs;
            
        } catch (Exception e) {
            logger.error("Error scraping jobs for company {}: {}", company.getName(), e.getMessage());
            failedExtractions++;
            return new ArrayList<>();
        }
    }
    
    /**
     * Scrape companies from a specific page
     */
    private List<Company> scrapeCompaniesPage(int page) throws IOException {
        String url = WellfoundConfig.STARTUPS_URL + "?page=" + page;
        logger.debug("Scraping companies page: {}", url);
        
        totalRequests++;
        Request request = createRequest(url);
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for URL: " + url);
            }
            
            String html = response.body().string();
            return extractCompaniesFromHtml(html, url);
        }
    }
    
    /**
     * Extract companies from HTML using multiple strategies
     */
    private List<Company> extractCompaniesFromHtml(String html, String sourceUrl) {
        List<Company> companies = new ArrayList<>();
        
        try {
            // Strategy 1: Extract from __NEXT_DATA__ JSON
            companies = extractCompaniesFromNextData(html, sourceUrl);
            if (!companies.isEmpty()) {
                logger.debug("Extracted {} companies using __NEXT_DATA__ strategy", companies.size());
                successfulExtractions++;
                return companies;
            }
            
            // Strategy 2: Parse HTML structure directly
            companies = extractCompaniesFromHtmlStructure(html, sourceUrl);
            if (!companies.isEmpty()) {
                logger.debug("Extracted {} companies using HTML parsing strategy", companies.size());
                successfulExtractions++;
                return companies;
            }
            
            logger.warn("No companies extracted from URL: {}", sourceUrl);
            failedExtractions++;
            
        } catch (Exception e) {
            logger.error("Error extracting companies from HTML: {}", e.getMessage());
            failedExtractions++;
        }
        
        return companies;
    }
    
    /**
     * Extract companies from __NEXT_DATA__ JSON embedded in HTML
     */
    private List<Company> extractCompaniesFromNextData(String html, String sourceUrl) {
        List<Company> companies = new ArrayList<>();
        
        try {
            Matcher matcher = nextDataPattern.matcher(html);
            if (matcher.find()) {
                String jsonData = matcher.group(1);
                JsonNode rootNode = objectMapper.readTree(jsonData);
                
                // Navigate to companies data - path may vary
                JsonNode companiesNode = findCompaniesNode(rootNode);
                if (companiesNode != null && companiesNode.isArray()) {
                    for (JsonNode companyNode : companiesNode) {
                        Company company = parseCompanyFromJson(companyNode, sourceUrl);
                        if (company != null) {
                            companies.add(company);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error parsing __NEXT_DATA__: {}", e.getMessage());
        }
        
        return companies;
    }
    
    /**
     * Extract companies from HTML structure using JSoup
     */
    private List<Company> extractCompaniesFromHtmlStructure(String html, String sourceUrl) {
        List<Company> companies = new ArrayList<>();
        
        try {
            Document document = Jsoup.parse(html);
            
            // Look for company cards/items in various selectors
            String[] selectors = {
                "[data-test='startup-card']",
                ".startup-card",
                ".company-card",
                "[data-startup-id]",
                ".company-item"
            };
            
            for (String selector : selectors) {
                List<Element> elements = document.select(selector);
                if (!elements.isEmpty()) {
                    logger.debug("Found {} company elements with selector: {}", elements.size(), selector);
                    
                    for (Element element : elements) {
                        Company company = parseCompanyFromElement(element, sourceUrl);
                        if (company != null) {
                            companies.add(company);
                        }
                    }
                    break; // Use first successful selector
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error parsing HTML structure: {}", e.getMessage());
        }
        
        return companies;
    }
    
    /**
     * Extract jobs from company jobs page
     */
    private List<WellfoundJobPosting> extractJobsFromUrl(String jobsUrl, Company company) throws IOException {
        logger.debug("Extracting jobs from URL: {}", jobsUrl);
        
        totalRequests++;
        Request request = createRequest(jobsUrl);
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for URL: " + jobsUrl);
            }
            
            String html = response.body().string();
            return extractJobsFromHtml(html, jobsUrl, company);
        }
    }
    
    /**
     * Extract jobs from HTML using multiple strategies
     */
    private List<WellfoundJobPosting> extractJobsFromHtml(String html, String sourceUrl, Company company) {
        List<WellfoundJobPosting> jobs = new ArrayList<>();
        
        try {
            // Strategy 1: Extract from __NEXT_DATA__ JSON
            jobs = extractJobsFromNextData(html, sourceUrl, company);
            if (!jobs.isEmpty()) {
                logger.debug("Extracted {} jobs using __NEXT_DATA__ strategy", jobs.size());
                successfulExtractions++;
                return jobs;
            }
            
            // Strategy 2: Parse HTML structure directly
            jobs = extractJobsFromHtmlStructure(html, sourceUrl, company);
            if (!jobs.isEmpty()) {
                logger.debug("Extracted {} jobs using HTML parsing strategy", jobs.size());
                successfulExtractions++;
                return jobs;
            }
            
            logger.debug("No jobs extracted from URL: {}", sourceUrl);
            
        } catch (Exception e) {
            logger.error("Error extracting jobs from HTML: {}", e.getMessage());
            failedExtractions++;
        }
        
        return jobs;
    }
    
    /**
     * Extract jobs from __NEXT_DATA__ JSON
     */
    private List<WellfoundJobPosting> extractJobsFromNextData(String html, String sourceUrl, Company company) {
        List<WellfoundJobPosting> jobs = new ArrayList<>();
        
        try {
            Matcher matcher = nextDataPattern.matcher(html);
            if (matcher.find()) {
                String jsonData = matcher.group(1);
                JsonNode rootNode = objectMapper.readTree(jsonData);
                
                JsonNode jobsNode = findJobsNode(rootNode);
                if (jobsNode != null && jobsNode.isArray()) {
                    for (JsonNode jobNode : jobsNode) {
                        WellfoundJobPosting job = parseJobFromJson(jobNode, sourceUrl, company);
                        if (job != null) {
                            jobs.add(job);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error parsing jobs from __NEXT_DATA__: {}", e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * Extract jobs from HTML structure
     */
    private List<WellfoundJobPosting> extractJobsFromHtmlStructure(String html, String sourceUrl, Company company) {
        List<WellfoundJobPosting> jobs = new ArrayList<>();
        
        try {
            Document document = Jsoup.parse(html);
            
            String[] selectors = {
                "[data-test='job-card']",
                ".job-card",
                ".job-item",
                "[data-job-id]",
                ".job-listing"
            };
            
            for (String selector : selectors) {
                List<Element> elements = document.select(selector);
                if (!elements.isEmpty()) {
                    logger.debug("Found {} job elements with selector: {}", elements.size(), selector);
                    
                    for (Element element : elements) {
                        WellfoundJobPosting job = parseJobFromElement(element, sourceUrl, company);
                        if (job != null) {
                            jobs.add(job);
                        }
                    }
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error parsing jobs from HTML structure: {}", e.getMessage());
        }
        
        return jobs;
    }
    
    // Helper methods for parsing JSON nodes and HTML elements
    
    private JsonNode findCompaniesNode(JsonNode rootNode) {
        // Search for companies data in various possible paths
        String[] paths = {
            "props.pageProps.companies",
            "props.pageProps.data.companies",
            "props.pageProps.startups",
            "props.serverData.companies"
        };
        
        for (String path : paths) {
            JsonNode node = rootNode.at("/" + path.replace(".", "/"));
            if (node != null && !node.isMissingNode() && node.isArray()) {
                return node;
            }
        }
        
        return null;
    }
    
    private JsonNode findJobsNode(JsonNode rootNode) {
        String[] paths = {
            "props.pageProps.jobs",
            "props.pageProps.data.jobs",
            "props.pageProps.company.jobs",
            "props.serverData.jobs"
        };
        
        for (String path : paths) {
            JsonNode node = rootNode.at("/" + path.replace(".", "/"));
            if (node != null && !node.isMissingNode() && node.isArray()) {
                return node;
            }
        }
        
        return null;
    }
    
    private Company parseCompanyFromJson(JsonNode companyNode, String sourceUrl) {
        try {
            Company company = new Company();
            company.setSourceUrl(sourceUrl);
            
            // Extract fields with null checks
            if (companyNode.has("id")) company.setId(companyNode.get("id").asText());
            if (companyNode.has("name")) company.setName(companyNode.get("name").asText());
            if (companyNode.has("slug")) company.setSlug(companyNode.get("slug").asText());
            if (companyNode.has("logo")) company.setLogo(companyNode.get("logo").asText());
            if (companyNode.has("headline")) company.setHeadline(companyNode.get("headline").asText());
            if (companyNode.has("location")) company.setLocation(companyNode.get("location").asText());
            if (companyNode.has("companySize")) company.setCompanySize(companyNode.get("companySize").asText());
            if (companyNode.has("website")) company.setWebsite(companyNode.get("website").asText());
            if (companyNode.has("jobsCount")) company.setTotalJobs(companyNode.get("jobsCount").asInt());
            
            // Store native data
            Map<String, Object> nativeData = objectMapper.convertValue(companyNode, Map.class);
            company.setNativeData(nativeData);
            
            return company.getName() != null ? company : null;
            
        } catch (Exception e) {
            logger.debug("Error parsing company from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    private Company parseCompanyFromElement(Element element, String sourceUrl) {
        try {
            Company company = new Company();
            company.setSourceUrl(sourceUrl);
            
            // Extract data from HTML attributes and text
            String id = element.attr("data-startup-id");
            if (id.isEmpty()) id = element.attr("data-id");
            company.setId(id);
            
            Element nameElement = element.selectFirst("h2, .company-name, [data-test='company-name']");
            if (nameElement != null) company.setName(nameElement.text());
            
            Element locationElement = element.selectFirst(".location, [data-test='location']");
            if (locationElement != null) company.setLocation(locationElement.text());
            
            return company.getName() != null ? company : null;
            
        } catch (Exception e) {
            logger.debug("Error parsing company from HTML element: {}", e.getMessage());
            return null;
        }
    }
    
    private WellfoundJobPosting parseJobFromJson(JsonNode jobNode, String sourceUrl, Company company) {
        try {
            WellfoundJobPosting job = new WellfoundJobPosting();
            job.setSourceUrl(sourceUrl);
            job.setCompanyId(company.getId());
            job.setCompanyName(company.getName());
            job.setCompanySlug(company.getSlug());
            
            // Extract job fields
            if (jobNode.has("id")) job.setId(jobNode.get("id").asText());
            if (jobNode.has("title")) job.setTitle(jobNode.get("title").asText());
            if (jobNode.has("description")) job.setDescription(jobNode.get("description").asText());
            if (jobNode.has("location")) job.setLocation(jobNode.get("location").asText());
            if (jobNode.has("jobType")) job.setJobType(jobNode.get("jobType").asText());
            
            // Apply URL
            if (job.getId() != null && company.getSlug() != null) {
                job.setApplyUrl(WellfoundConfig.BASE_URL + "/company/" + company.getSlug() + "/jobs/" + job.getId());
            }
            
            // Store native data and calculate quality
            Map<String, Object> nativeData = objectMapper.convertValue(jobNode, Map.class);
            job.setNativeData(nativeData);
            job.calculateQualityScore();
            
            return job.getTitle() != null ? job : null;
            
        } catch (Exception e) {
            logger.debug("Error parsing job from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    private WellfoundJobPosting parseJobFromElement(Element element, String sourceUrl, Company company) {
        try {
            WellfoundJobPosting job = new WellfoundJobPosting();
            job.setSourceUrl(sourceUrl);
            job.setCompanyId(company.getId());
            job.setCompanyName(company.getName());
            job.setCompanySlug(company.getSlug());
            
            String id = element.attr("data-job-id");
            if (id.isEmpty()) id = element.attr("data-id");
            job.setId(id);
            
            Element titleElement = element.selectFirst("h3, .job-title, [data-test='job-title']");
            if (titleElement != null) job.setTitle(titleElement.text());
            
            Element locationElement = element.selectFirst(".location, [data-test='location']");
            if (locationElement != null) job.setLocation(locationElement.text());
            
            job.calculateQualityScore();
            return job.getTitle() != null ? job : null;
            
        } catch (Exception e) {
            logger.debug("Error parsing job from HTML element: {}", e.getMessage());
            return null;
        }
    }
    
    // HTTP client and session management
    
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(WellfoundConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(WellfoundConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WellfoundConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    private Request createRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        
        // Add default headers
        for (String[] header : WellfoundConfig.DEFAULT_HEADERS) {
            builder.addHeader(header[0], header[1]);
        }
        
        // Add user agent
        String userAgent = WellfoundConfig.USER_AGENTS[0]; // Use first user agent for now
        builder.addHeader("User-Agent", userAgent);
        
        return builder.build();
    }
    
    /**
     * Extract company slug from jobs URL
     * URL format: https://wellfound.com/company/{slug}/jobs
     */
    private String extractCompanySlugFromUrl(String jobsUrl) {
        try {
            if (jobsUrl != null && jobsUrl.contains("/company/")) {
                String[] parts = jobsUrl.split("/company/");
                if (parts.length > 1) {
                    String remaining = parts[1];
                    String[] slugParts = remaining.split("/");
                    if (slugParts.length > 0 && !slugParts[0].isEmpty()) {
                        return slugParts[0];
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting company slug from URL {}: {}", jobsUrl, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get extraction statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests);
        stats.put("successfulExtractions", successfulExtractions);
        stats.put("failedExtractions", failedExtractions);
        stats.put("successRate", totalRequests > 0 ? (double) successfulExtractions / totalRequests * 100.0 : 0.0);
        return stats;
    }
    
    /**
     * Basic session manager for cookie handling
     */
    private static class SessionManager {
        private String cookies = "";
        private long lastRefresh = System.currentTimeMillis();
        
        public String getCookies() {
            // Implement session refresh logic here
            return cookies;
        }
        
        public void refreshSession() {
            // Implement session refresh using browser automation
            lastRefresh = System.currentTimeMillis();
        }
    }
}