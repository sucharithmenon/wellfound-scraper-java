package com.gigacrawl.wellfound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigacrawl.wellfound.model.Company;
import com.gigacrawl.wellfound.model.WellfoundJobPosting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Database service for storing Wellfound companies and job postings
 * Compatible with Gigacrawl database schema (job_source_urls and ats_job_postings)
 */
public class DatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    
    // Database queries
    private static final String INSERT_COMPANY_SQL = 
            "INSERT INTO job_source_urls (" +
            "url, source_type, company_name, company_identifier, status, " +
            "last_scraped, total_jobs_found, metadata, created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, NOW(), NOW()) " +
            "ON CONFLICT (url) DO UPDATE SET " +
            "company_name = EXCLUDED.company_name, " +
            "total_jobs_found = EXCLUDED.total_jobs_found, " +
            "metadata = EXCLUDED.metadata, " +
            "updated_at = NOW()";
    
    private static final String INSERT_JOB_SQL = 
            "INSERT INTO ats_job_postings (" +
            "ats_job_id, ats_type, title, description, location, job_type, " +
            "company_name, company_identifier, apply_url, salary_min, salary_max, " +
            "salary_currency, skills, experience_level, remote_ok, " +
            "posted_date, extraction_timestamp, extraction_success_score, " +
            "quality_grade, source_url, native_data, created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, NOW(), NOW()) " +
            "ON CONFLICT (ats_job_id, ats_type) DO UPDATE SET " +
            "title = EXCLUDED.title, " +
            "description = EXCLUDED.description, " +
            "location = EXCLUDED.location, " +
            "salary_min = EXCLUDED.salary_min, " +
            "salary_max = EXCLUDED.salary_max, " +
            "extraction_success_score = EXCLUDED.extraction_success_score, " +
            "quality_grade = EXCLUDED.quality_grade, " +
            "native_data = EXCLUDED.native_data, " +
            "updated_at = NOW()";
    
    private static final String GET_COMPANIES_SQL = 
            "SELECT url, company_name, company_identifier, total_jobs_found, metadata " +
            "FROM job_source_urls " +
            "WHERE source_type = 'wellfound' " +
            "ORDER BY total_jobs_found DESC NULLS LAST";
    
    private static final String GET_JOBS_SQL = 
            "SELECT ats_job_id, title, company_name, location, extraction_success_score, quality_grade " +
            "FROM ats_job_postings " +
            "WHERE ats_type = 'wellfound' " +
            "ORDER BY extraction_timestamp DESC";
    
    public DatabaseService() {
        this.dataSource = createDataSource();
        this.objectMapper = new ObjectMapper();
        logger.info("DatabaseService initialized with connection pool");
    }
    
    /**
     * Save company to job_source_urls table
     */
    public boolean saveCompany(Company company) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_COMPANY_SQL)) {
            
            stmt.setString(1, company.getJobsUrl()); // URL
            stmt.setString(2, "wellfound"); // source_type
            stmt.setString(3, company.getName()); // company_name
            stmt.setString(4, company.getSlug()); // company_identifier
            stmt.setString(5, "active"); // status
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now())); // last_scraped
            stmt.setObject(7, company.getTotalJobs()); // total_jobs_found
            
            // Convert company to metadata JSON
            String metadataJson = createCompanyMetadata(company);
            stmt.setString(8, metadataJson); // metadata
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logger.debug("Saved company: {} ({})", company.getName(), company.getSlug());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error saving company {}: {}", company.getName(), e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Save job posting to ats_job_postings table
     */
    public boolean saveJob(WellfoundJobPosting job) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_JOB_SQL)) {
            
            stmt.setString(1, job.getAtsJobId()); // ats_job_id
            stmt.setString(2, job.getAtsType()); // ats_type
            stmt.setString(3, job.getTitle()); // title
            stmt.setString(4, job.getDescription()); // description
            stmt.setString(5, job.getLocation()); // location
            stmt.setString(6, job.getJobType()); // job_type
            stmt.setString(7, job.getCompanyName()); // company_name
            stmt.setString(8, job.getCompanySlug()); // company_identifier
            stmt.setString(9, job.getApplyUrl()); // apply_url
            stmt.setObject(10, job.getSalaryMin()); // salary_min
            stmt.setObject(11, job.getSalaryMax()); // salary_max
            stmt.setString(12, job.getSalaryCurrency()); // salary_currency
            
            // Convert skills list to JSON
            String skillsJson = job.getSkills() != null ? objectMapper.writeValueAsString(job.getSkills()) : null;
            stmt.setString(13, skillsJson); // skills
            
            stmt.setString(14, job.getExperienceLevel()); // experience_level
            stmt.setBoolean(15, job.getRemoteOk() != null ? job.getRemoteOk() : false); // remote_ok
            stmt.setTimestamp(16, job.getPostedDate() != null ? Timestamp.valueOf(job.getPostedDate()) : null); // posted_date
            stmt.setTimestamp(17, Timestamp.valueOf(job.getExtractionTimestamp())); // extraction_timestamp
            stmt.setObject(18, job.getExtractionSuccessScore()); // extraction_success_score
            stmt.setString(19, job.getQualityGrade()); // quality_grade
            stmt.setString(20, job.getSourceUrl()); // source_url
            
            // Convert native data to JSON
            String nativeDataJson = job.getNativeData() != null ? objectMapper.writeValueAsString(job.getNativeData()) : null;
            stmt.setString(21, nativeDataJson); // native_data
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logger.debug("Saved job: {} at {} (Score: {}%)", 
                        job.getTitle(), job.getCompanyName(), job.getExtractionSuccessScore());
                return true;
            }
            
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Error saving job {}: {}", job.getTitle(), e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Save multiple companies in batch
     */
    public int saveCompaniesBatch(List<Company> companies) {
        int savedCount = 0;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_COMPANY_SQL)) {
                for (Company company : companies) {
                    stmt.setString(1, company.getJobsUrl());
                    stmt.setString(2, "wellfound");
                    stmt.setString(3, company.getName());
                    stmt.setString(4, company.getSlug());
                    stmt.setString(5, "active");
                    stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setObject(7, company.getTotalJobs());
                    stmt.setString(8, createCompanyMetadata(company));
                    
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                conn.commit();
                
                for (int result : results) {
                    if (result > 0) savedCount++;
                }
                
                logger.info("Batch saved {} companies out of {}", savedCount, companies.size());
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Error in batch save companies: {}", e.getMessage());
        }
        
        return savedCount;
    }
    
    /**
     * Save multiple jobs in batch
     */
    public int saveJobsBatch(List<WellfoundJobPosting> jobs) {
        int savedCount = 0;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_JOB_SQL)) {
                for (WellfoundJobPosting job : jobs) {
                    try {
                        setJobParameters(stmt, job);
                        stmt.addBatch();
                    } catch (JsonProcessingException e) {
                        logger.error("Error setting parameters for job {}: {}", job.getTitle(), e.getMessage());
                    }
                }
                
                int[] results = stmt.executeBatch();
                conn.commit();
                
                for (int result : results) {
                    if (result > 0) savedCount++;
                }
                
                logger.info("Batch saved {} jobs out of {}", savedCount, jobs.size());
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Error in batch save jobs: {}", e.getMessage());
        }
        
        return savedCount;
    }
    
    /**
     * Get all companies from database
     */
    public List<Company> getCompanies() {
        List<Company> companies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_COMPANIES_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Company company = new Company();
                company.setJobsUrl(rs.getString("url"));
                company.setName(rs.getString("company_name"));
                company.setSlug(rs.getString("company_identifier"));
                company.setTotalJobs(rs.getObject("total_jobs_found", Integer.class));
                
                // Parse metadata JSON
                String metadataJson = rs.getString("metadata");
                if (metadataJson != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                        company.setNativeData(metadata);
                        
                        // Extract fields from metadata
                        if (metadata.containsKey("location")) {
                            company.setLocation((String) metadata.get("location"));
                        }
                        if (metadata.containsKey("companySize")) {
                            company.setCompanySize((String) metadata.get("companySize"));
                        }
                        
                    } catch (JsonProcessingException e) {
                        logger.debug("Error parsing metadata for company {}: {}", company.getName(), e.getMessage());
                    }
                }
                
                companies.add(company);
            }
            
            logger.info("Retrieved {} companies from database", companies.size());
            
        } catch (SQLException e) {
            logger.error("Error retrieving companies: {}", e.getMessage());
        }
        
        return companies;
    }
    
    /**
     * Get database statistics
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // Count companies
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM job_source_urls WHERE source_type = 'wellfound'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalCompanies", rs.getInt(1));
                }
            }
            
            // Count jobs
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM ats_job_postings WHERE ats_type = 'wellfound'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalJobs", rs.getInt(1));
                }
            }
            
            // Average quality score
            try (PreparedStatement stmt = conn.prepareStatement("SELECT AVG(extraction_success_score) FROM ats_job_postings WHERE ats_type = 'wellfound' AND extraction_success_score IS NOT NULL");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("averageQualityScore", rs.getDouble(1));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving database stats: {}", e.getMessage());
        }
        
        return stats;
    }
    
    // Helper methods
    
    private DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Use environment variables or defaults
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null) {
            config.setJdbcUrl(dbUrl);
        } else {
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/cursor_jobs");
            config.setUsername(System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "cursor");
            config.setPassword(System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "cursor_password");
        }
        
        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
    
    private String createCompanyMetadata(Company company) {
        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("id", company.getId());
            metadata.put("slug", company.getSlug());
            metadata.put("logo", company.getLogo());
            metadata.put("headline", company.getHeadline());
            metadata.put("description", company.getDescription());
            metadata.put("location", company.getLocation());
            metadata.put("companySize", company.getCompanySize());
            metadata.put("companyType", company.getCompanyType());
            metadata.put("funding", company.getFunding());
            metadata.put("website", company.getWebsite());
            metadata.put("twitter", company.getTwitter());
            metadata.put("linkedin", company.getLinkedin());
            metadata.put("badges", company.getBadges());
            metadata.put("foundedYear", company.getFoundedYear());
            metadata.put("industries", company.getIndustries());
            metadata.put("extractionTimestamp", company.getExtractionTimestamp());
            
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.debug("Error creating metadata for company {}: {}", company.getName(), e.getMessage());
            return "{}";
        }
    }
    
    private void setJobParameters(PreparedStatement stmt, WellfoundJobPosting job) throws SQLException, JsonProcessingException {
        stmt.setString(1, job.getAtsJobId());
        stmt.setString(2, job.getAtsType());
        stmt.setString(3, job.getTitle());
        stmt.setString(4, job.getDescription());
        stmt.setString(5, job.getLocation());
        stmt.setString(6, job.getJobType());
        stmt.setString(7, job.getCompanyName());
        stmt.setString(8, job.getCompanySlug());
        stmt.setString(9, job.getApplyUrl());
        stmt.setObject(10, job.getSalaryMin());
        stmt.setObject(11, job.getSalaryMax());
        stmt.setString(12, job.getSalaryCurrency());
        stmt.setString(13, job.getSkills() != null ? objectMapper.writeValueAsString(job.getSkills()) : null);
        stmt.setString(14, job.getExperienceLevel());
        stmt.setBoolean(15, job.getRemoteOk() != null ? job.getRemoteOk() : false);
        stmt.setTimestamp(16, job.getPostedDate() != null ? Timestamp.valueOf(job.getPostedDate()) : null);
        stmt.setTimestamp(17, Timestamp.valueOf(job.getExtractionTimestamp()));
        stmt.setObject(18, job.getExtractionSuccessScore());
        stmt.setString(19, job.getQualityGrade());
        stmt.setString(20, job.getSourceUrl());
        stmt.setString(21, job.getNativeData() != null ? objectMapper.writeValueAsString(job.getNativeData()) : null);
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Close database connection pool
     */
    public void close() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            logger.info("Database connection pool closed");
        }
    }
}