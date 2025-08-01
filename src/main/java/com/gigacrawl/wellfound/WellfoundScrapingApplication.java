package com.gigacrawl.wellfound;

import com.gigacrawl.wellfound.model.Company;
import com.gigacrawl.wellfound.model.WellfoundJobPosting;
import com.gigacrawl.wellfound.service.DatabaseService;
import com.gigacrawl.wellfound.service.WellfoundScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Wellfound Job Scraper Application - Production-ready command-line interface
 * Scrapes companies and jobs from Wellfound.com with database integration
 */
public class WellfoundScrapingApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(WellfoundScrapingApplication.class);
    
    private static final String VERSION = "1.0.0";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "--version":
            case "-v":
                printVersion();
                break;
                
            case "--help":
            case "-h":
                printHelp();
                break;
                
            case "--companies":
                scrapeCompanies(args);
                break;
                
            case "--jobs":
                scrapeJobs(args);
                break;
                
            case "--jobs-from-db":
                scrapeJobsFromDatabase(args);
                break;
                
            case "--full":
                runFullScrape(args);
                break;
                
            case "--stats":
                showStatistics();
                break;
                
            case "--test-db":
                testDatabase();
                break;
                
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }
    
    private static void printVersion() {
        System.out.println("Wellfound Scraper v" + VERSION);
        System.out.println("Production-ready scraper for Wellfound.com startup jobs");
        System.out.println("Multi-strategy extraction with anti-bot capabilities");
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar wellfound-scraper.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  --companies [pages]       Scrape companies to job_source_urls (default: 10 pages)");
        System.out.println("  --jobs [company-slug]     Scrape jobs for specific company");
        System.out.println("  --jobs-from-db [limit]    Scrape jobs from job_source_urls table");
        System.out.println("  --full [pages]            Run full scrape (companies + jobs)");
        System.out.println("  --stats                   Show database statistics");
        System.out.println("  --test-db                 Test database connection");
        System.out.println("  --version                 Show version information");
        System.out.println("  --help                    Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar wellfound-scraper.jar --companies 5");
        System.out.println("  java -jar wellfound-scraper.jar --jobs openai");
        System.out.println("  java -jar wellfound-scraper.jar --jobs-from-db 50");
        System.out.println("  java -jar wellfound-scraper.jar --full 20");
    }
    
    private static void printHelp() {
        printUsage();
        System.out.println();
        System.out.println("Environment Variables:");
        System.out.println("  DATABASE_URL          PostgreSQL connection URL");
        System.out.println("  DB_USERNAME           Database username (default: cursor)");
        System.out.println("  DB_PASSWORD           Database password (default: cursor_password)");
        System.out.println();
        System.out.println("Workflow:");
        System.out.println("  1. --companies: Scrape companies → job_source_urls table");
        System.out.println("  2. --jobs-from-db: Read URLs from job_source_urls → scrape jobs → ats_job_postings");
        System.out.println("  3. --full: Combines both phases automatically");
        System.out.println();
        System.out.println("Features:");
        System.out.println("  - Multi-strategy extraction (JSON + HTML parsing)");
        System.out.println("  - Anti-bot evasion with rate limiting");
        System.out.println("  - Quality scoring and grading system");
        System.out.println("  - Concurrent processing with thread pools");
        System.out.println("  - Database integration with batch operations");
        System.out.println("  - Comprehensive error handling and retry logic");
    }
    
    private static void scrapeCompanies(String[] args) {
        int maxPages = 10; // default
        
        if (args.length > 1) {
            try {
                maxPages = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid page count: " + args[1]);
                return;
            }
        }
        
        System.out.println("=== Wellfound Company Scraper ===");
        System.out.println("Max pages: " + maxPages);
        System.out.println("Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try {
            WellfoundScraper scraper = new WellfoundScraper();
            DatabaseService dbService = new DatabaseService();
            
            // Test database connection
            if (!dbService.testConnection()) {
                System.err.println("❌ Database connection failed");
                return;
            }
            
            System.out.println("✅ Database connection successful");
            
            // Scrape companies
            List<Company> companies = scraper.scrapeCompanies(maxPages);
            
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found");
                return;
            }
            
            System.out.println("✅ Found " + companies.size() + " companies");
            
            // Save to database
            int savedCount = dbService.saveCompaniesBatch(companies);
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println();
            System.out.println("=== RESULTS ===");
            System.out.println("Companies found: " + companies.size());
            System.out.println("Companies saved: " + savedCount);
            System.out.println("Success rate: " + String.format("%.1f%%", (double) savedCount / companies.size() * 100));
            System.out.println("Duration: " + duration + "ms");
            
            // Show scraper statistics
            Map<String, Object> stats = scraper.getStatistics();
            System.out.println("Extraction success rate: " + String.format("%.1f%%", (Double) stats.get("successRate")));
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error during company scraping: " + e.getMessage());
            logger.error("Company scraping failed", e);
        }
    }
    
    private static void scrapeJobs(String[] args) {
        if (args.length < 2) {
            System.err.println("Company slug required. Usage: --jobs <company-slug>");
            return;
        }
        
        String companySlug = args[1];
        
        System.out.println("=== Wellfound Job Scraper ===");
        System.out.println("Company: " + companySlug);
        System.out.println("Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try {
            WellfoundScraper scraper = new WellfoundScraper();
            DatabaseService dbService = new DatabaseService();
            
            // Create company object
            Company company = new Company();
            company.setSlug(companySlug);
            company.setName(companySlug); // Will be updated during scraping
            
            // Scrape jobs
            List<WellfoundJobPosting> jobs = scraper.scrapeCompanyJobs(company);
            
            if (jobs.isEmpty()) {
                System.out.println("❌ No jobs found for company: " + companySlug);
                return;
            }
            
            System.out.println("✅ Found " + jobs.size() + " jobs");
            
            // Save to database
            int savedCount = dbService.saveJobsBatch(jobs);
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println();
            System.out.println("=== RESULTS ===");
            System.out.println("Jobs found: " + jobs.size());
            System.out.println("Jobs saved: " + savedCount);
            System.out.println("Success rate: " + String.format("%.1f%%", (double) savedCount / jobs.size() * 100));
            System.out.println("Duration: " + duration + "ms");
            
            // Show quality statistics
            double avgQuality = jobs.stream()
                    .mapToDouble(job -> job.getExtractionSuccessScore() != null ? job.getExtractionSuccessScore() : 0.0)
                    .average()
                    .orElse(0.0);
            
            System.out.println("Average quality score: " + String.format("%.1f%%", avgQuality));
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error during job scraping: " + e.getMessage());
            logger.error("Job scraping failed", e);
        }
    }
    
    private static void scrapeJobsFromDatabase(String[] args) {
        int maxUrls = 50; // default
        
        if (args.length > 1) {
            try {
                maxUrls = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid URL limit: " + args[1]);
                return;
            }
        }
        
        System.out.println("=== Wellfound Jobs from Database ===");
        System.out.println("Max URLs to process: " + maxUrls);
        System.out.println("Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try {
            WellfoundScraper scraper = new WellfoundScraper();
            DatabaseService dbService = new DatabaseService();
            
            // Test database connection
            if (!dbService.testConnection()) {
                System.err.println("❌ Database connection failed");
                return;
            }
            
            System.out.println("✅ Database connection successful");
            
            // Get company URLs from database
            List<String> companyUrls = dbService.getCompanyUrlsForScraping();
            
            if (companyUrls.isEmpty()) {
                System.out.println("❌ No company URLs found in job_source_urls table");
                System.out.println("💡 Run --companies first to populate the database");
                return;
            }
            
            // Limit URLs if specified
            if (companyUrls.size() > maxUrls) {
                companyUrls = companyUrls.subList(0, maxUrls);
            }
            
            System.out.println("✅ Found " + companyUrls.size() + " company URLs to process");
            
            // Process URLs with concurrent execution
            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<WellfoundJobPosting> allJobs = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            for (String url : companyUrls) {
                executor.submit(() -> {
                    try {
                        List<WellfoundJobPosting> jobs = scraper.scrapeJobsFromUrl(url);
                        allJobs.addAll(jobs);
                        System.out.println("✅ " + url + ": " + jobs.size() + " jobs");
                    } catch (Exception e) {
                        System.err.println("❌ Error processing " + url + ": " + e.getMessage());
                    }
                });
            }
            
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Save jobs to database
            int savedJobs = 0;
            if (!allJobs.isEmpty()) {
                savedJobs = dbService.saveJobsBatch(allJobs);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println();
            System.out.println("=== RESULTS ===");
            System.out.println("URLs processed: " + companyUrls.size());
            System.out.println("Jobs found: " + allJobs.size());
            System.out.println("Jobs saved: " + savedJobs);
            System.out.println("Success rate: " + String.format("%.1f%%", 
                    companyUrls.size() > 0 ? (double) savedJobs / allJobs.size() * 100 : 0));
            System.out.println("Duration: " + String.format("%.1f", duration / 1000.0) + "s");
            
            // Quality statistics
            if (!allJobs.isEmpty()) {
                double avgQuality = allJobs.stream()
                        .mapToDouble(job -> job.getExtractionSuccessScore() != null ? job.getExtractionSuccessScore() : 0.0)
                        .average()
                        .orElse(0.0);
                
                System.out.println("Average quality score: " + String.format("%.1f%%", avgQuality));
            }
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error during database job scraping: " + e.getMessage());
            logger.error("Database job scraping failed", e);
        }
    }
    
    private static void runFullScrape(String[] args) {
        int maxPages = 20; // default for full scrape
        
        if (args.length > 1) {
            try {
                maxPages = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid page count: " + args[1]);
                return;
            }
        }
        
        System.out.println("=== Wellfound Full Scraper ===");
        System.out.println("Max pages: " + maxPages);
        System.out.println("Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try {
            WellfoundScraper scraper = new WellfoundScraper();
            DatabaseService dbService = new DatabaseService();
            
            // Test database connection
            if (!dbService.testConnection()) {
                System.err.println("❌ Database connection failed");
                return;
            }
            
            System.out.println("✅ Database connection successful");
            
            // Phase 1: Scrape companies
            System.out.println("\n--- Phase 1: Scraping Companies ---");
            List<Company> companies = scraper.scrapeCompanies(maxPages);
            
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found");
                return;
            }
            
            System.out.println("✅ Found " + companies.size() + " companies");
            int savedCompanies = dbService.saveCompaniesBatch(companies);
            System.out.println("✅ Saved " + savedCompanies + " companies");
            
            // Phase 2: Scrape jobs from saved company URLs (concurrent processing)
            System.out.println("\n--- Phase 2: Scraping Jobs from Database ---");
            
            // Get the URLs we just saved
            List<String> companyUrls = dbService.getCompanyUrlsForScraping();
            System.out.println("Found " + companyUrls.size() + " company URLs to process for jobs");
            
            ExecutorService executor = Executors.newFixedThreadPool(3); // Limit concurrent threads
            List<WellfoundJobPosting> allJobs = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            for (String url : companyUrls) {
                executor.submit(() -> {
                    try {
                        List<WellfoundJobPosting> jobs = scraper.scrapeJobsFromUrl(url);
                        allJobs.addAll(jobs);
                        String companyName = extractCompanyNameFromUrl(url);
                        System.out.println("✅ " + companyName + ": " + jobs.size() + " jobs");
                    } catch (Exception e) {
                        System.err.println("❌ Error scraping jobs from " + url + ": " + e.getMessage());
                    }
                });
            }
            
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Save jobs
            if (!allJobs.isEmpty()) {
                int savedJobs = dbService.saveJobsBatch(allJobs);
                System.out.println("✅ Saved " + savedJobs + " jobs");
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Final statistics
            System.out.println();
            System.out.println("=== FINAL RESULTS ===");
            System.out.println("Companies found: " + companies.size());
            System.out.println("Companies saved: " + savedCompanies);
            System.out.println("Jobs found: " + allJobs.size());
            System.out.println("Jobs saved: " + dbService.saveJobsBatch(allJobs));
            System.out.println("Total duration: " + String.format("%.1f", duration / 1000.0) + "s");
            
            // Quality statistics
            if (!allJobs.isEmpty()) {
                double avgQuality = allJobs.stream()
                        .mapToDouble(job -> job.getExtractionSuccessScore() != null ? job.getExtractionSuccessScore() : 0.0)
                        .average()
                        .orElse(0.0);
                
                System.out.println("Average quality score: " + String.format("%.1f%%", avgQuality));
            }
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error during full scraping: " + e.getMessage());
            logger.error("Full scraping failed", e);
        }
    }
    
    private static void showStatistics() {
        System.out.println("=== Database Statistics ===");
        
        try {
            DatabaseService dbService = new DatabaseService();
            
            if (!dbService.testConnection()) {
                System.err.println("❌ Database connection failed");
                return;
            }
            
            Map<String, Object> stats = dbService.getDatabaseStats();
            
            System.out.println("Total companies: " + stats.getOrDefault("totalCompanies", 0));
            System.out.println("Total jobs: " + stats.getOrDefault("totalJobs", 0));
            
            Double avgQuality = (Double) stats.get("averageQualityScore");
            if (avgQuality != null) {
                System.out.println("Average quality score: " + String.format("%.1f%%", avgQuality));
            }
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error retrieving statistics: " + e.getMessage());
            logger.error("Statistics retrieval failed", e);
        }
    }
    
    private static void testDatabase() {
        System.out.println("=== Database Connection Test ===");
        
        try {
            DatabaseService dbService = new DatabaseService();
            
            if (dbService.testConnection()) {
                System.out.println("✅ Database connection successful");
                
                // Show database info
                Map<String, Object> stats = dbService.getDatabaseStats();
                System.out.println("Companies in database: " + stats.getOrDefault("totalCompanies", 0));
                System.out.println("Jobs in database: " + stats.getOrDefault("totalJobs", 0));
            } else {
                System.out.println("❌ Database connection failed");
            }
            
            dbService.close();
            
        } catch (Exception e) {
            System.err.println("❌ Database test failed: " + e.getMessage());
            logger.error("Database test failed", e);
        }
    }
    
    /**
     * Extract company name from jobs URL for display purposes
     */
    private static String extractCompanyNameFromUrl(String url) {
        try {
            if (url != null && url.contains("/company/")) {
                String[] parts = url.split("/company/");
                if (parts.length > 1) {
                    String remaining = parts[1];
                    String[] slugParts = remaining.split("/");
                    if (slugParts.length > 0 && !slugParts[0].isEmpty()) {
                        return slugParts[0];
                    }
                }
            }
        } catch (Exception e) {
            // Fall back to URL
        }
        return url;
    }
}