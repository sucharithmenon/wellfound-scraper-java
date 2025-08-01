package com.gigacrawl.wellfound.config;

/**
 * Configuration constants for Wellfound scraper
 */
public class WellfoundConfig {
    
    // Base URLs
    public static final String BASE_URL = "https://wellfound.com";
    public static final String GRAPHQL_URL = "https://wellfound.com/graphql";
    public static final String STARTUPS_URL = "https://wellfound.com/startups";
    
    // Rate limiting
    public static final double DEFAULT_RATE_LIMIT = 1.5; // requests per second
    public static final double AGGRESSIVE_RATE_LIMIT = 1.0; // requests per second for sensitive operations
    
    // Request timeouts
    public static final int CONNECT_TIMEOUT = 30; // seconds
    public static final int READ_TIMEOUT = 60; // seconds
    public static final int WRITE_TIMEOUT = 30; // seconds
    
    // Retry configuration
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_DELAY_BASE = 2; // seconds
    public static final int MAX_RETRY_DELAY = 30; // seconds
    
    // Browser automation
    public static final int BROWSER_TIMEOUT = 30; // seconds
    public static final int BROWSER_PAGE_LOAD_TIMEOUT = 60; // seconds
    public static final boolean HEADLESS_MODE = true;
    
    // Session management
    public static final int SESSION_REFRESH_INTERVAL = 100; // requests
    public static final int SESSION_MAX_AGE = 3600; // seconds (1 hour)
    
    // Anti-bot evasion
    public static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };
    
    // Default headers for HTTP requests
    public static final String[][] DEFAULT_HEADERS = {
        {"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"},
        {"Accept-Language", "en-US,en;q=0.9"},
        {"Accept-Encoding", "gzip, deflate, br"},
        {"DNT", "1"},
        {"Connection", "keep-alive"},
        {"Upgrade-Insecure-Requests", "1"},
        {"Sec-Fetch-Dest", "document"},
        {"Sec-Fetch-Mode", "navigate"},
        {"Sec-Fetch-Site", "none"},
        {"Sec-Fetch-User", "?1"},
        {"sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\""},
        {"sec-ch-ua-mobile", "?0"},
        {"sec-ch-ua-platform", "\"macOS\""}
    };
    
    // GraphQL headers
    public static final String[][] GRAPHQL_HEADERS = {
        {"Content-Type", "application/json"},
        {"Accept", "application/json"},
        {"Apollo-Require-Preflight", "true"},
        {"X-Requested-With", "XMLHttpRequest"}
    };
    
    // Extraction patterns
    public static final String NEXT_DATA_PATTERN = "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>";
    public static final String COMPANY_DATA_PATTERN = "\"company\":\\s*(\\{.*?\\})";
    public static final String JOBS_DATA_PATTERN = "\"jobs\":\\s*(\\[.*?\\])";
    
    // Database table names
    public static final String COMPANIES_TABLE = "job_source_urls";
    public static final String JOBS_TABLE = "ats_job_postings";
    
    // Quality thresholds
    public static final double MIN_QUALITY_SCORE = 60.0;
    public static final double TARGET_QUALITY_SCORE = 75.0;
    public static final double EXCELLENT_QUALITY_SCORE = 90.0;
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGES_PER_SEARCH = 50;
    public static final int MAX_CONCURRENT_PAGES = 3;
    
    // Error codes
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_SERVER_ERROR = 500;
    
    private WellfoundConfig() {
        // Utility class - prevent instantiation
    }
}