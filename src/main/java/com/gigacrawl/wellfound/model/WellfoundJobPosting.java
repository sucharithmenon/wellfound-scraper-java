package com.gigacrawl.wellfound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Job posting model for Wellfound jobs compatible with ats_job_postings table structure
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WellfoundJobPosting {
    
    // Core job fields
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("job_type")
    private String jobType;
    
    @JsonProperty("apply_url")
    private String applyUrl;
    
    @JsonProperty("company_name")
    private String companyName;
    
    @JsonProperty("company_slug")
    private String companySlug;
    
    @JsonProperty("company_logo")
    private String companyLogo;
    
    // Salary information
    @JsonProperty("salary_min")
    private Integer salaryMin;
    
    @JsonProperty("salary_max")
    private Integer salaryMax;
    
    @JsonProperty("salary_currency")
    private String salaryCurrency;
    
    @JsonProperty("salary_text")
    private String salaryText;
    
    // Skills and requirements
    @JsonProperty("skills")
    private List<String> skills;
    
    @JsonProperty("experience_level")
    private String experienceLevel;
    
    @JsonProperty("requirements")
    private List<String> requirements;
    
    // Dates
    @JsonProperty("posted_date")
    private LocalDateTime postedDate;
    
    @JsonProperty("valid_until")
    private LocalDateTime validUntil;
    
    // ATS compatibility fields
    @JsonProperty("ats_type")
    private String atsType = "wellfound";
    
    @JsonProperty("ats_job_id")
    private String atsJobId;
    
    @JsonProperty("source_url")
    private String sourceUrl;
    
    @JsonProperty("extraction_timestamp")
    private LocalDateTime extractionTimestamp;
    
    @JsonProperty("extraction_success_score")
    private Double extractionSuccessScore;
    
    @JsonProperty("quality_grade")
    private String qualityGrade;
    
    @JsonProperty("native_data")
    private Map<String, Object> nativeData;
    
    // Company information
    @JsonProperty("company_id")
    private String companyId;
    
    @JsonProperty("company_size")
    private String companySize;
    
    @JsonProperty("company_funding")
    private String companyFunding;
    
    @JsonProperty("company_industries")
    private List<String> companyIndustries;
    
    // Remote work
    @JsonProperty("remote_ok")
    private Boolean remoteOk;
    
    @JsonProperty("visa_sponsorship")
    private Boolean visaSponsorship;
    
    // Benefits and perks
    @JsonProperty("benefits")
    private List<String> benefits;
    
    @JsonProperty("equity_offered")
    private Boolean equityOffered;
    
    // Default constructor
    public WellfoundJobPosting() {
        this.extractionTimestamp = LocalDateTime.now();
        this.atsType = "wellfound";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { 
        this.id = id;
        this.atsJobId = id;
    }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { 
        this.location = location;
        // Set remote flag based on location
        if (location != null) {
            this.remoteOk = location.toLowerCase().contains("remote");
        }
    }
    
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getCompanySlug() { return companySlug; }
    public void setCompanySlug(String companySlug) { this.companySlug = companySlug; }
    
    public String getCompanyLogo() { return companyLogo; }
    public void setCompanyLogo(String companyLogo) { this.companyLogo = companyLogo; }
    
    public Integer getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Integer salaryMin) { this.salaryMin = salaryMin; }
    
    public Integer getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Integer salaryMax) { this.salaryMax = salaryMax; }
    
    public String getSalaryCurrency() { return salaryCurrency; }
    public void setSalaryCurrency(String salaryCurrency) { this.salaryCurrency = salaryCurrency; }
    
    public String getSalaryText() { return salaryText; }
    public void setSalaryText(String salaryText) { this.salaryText = salaryText; }
    
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    
    public List<String> getRequirements() { return requirements; }
    public void setRequirements(List<String> requirements) { this.requirements = requirements; }
    
    public LocalDateTime getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDateTime postedDate) { this.postedDate = postedDate; }
    
    public void setPostedDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            this.postedDate = zonedDateTime.toLocalDateTime();
        }
    }
    
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    
    public String getAtsType() { return atsType; }
    public void setAtsType(String atsType) { this.atsType = atsType; }
    
    public String getAtsJobId() { return atsJobId; }
    public void setAtsJobId(String atsJobId) { this.atsJobId = atsJobId; }
    
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    
    public LocalDateTime getExtractionTimestamp() { return extractionTimestamp; }
    public void setExtractionTimestamp(LocalDateTime extractionTimestamp) { this.extractionTimestamp = extractionTimestamp; }
    
    public Double getExtractionSuccessScore() { return extractionSuccessScore; }
    public void setExtractionSuccessScore(Double extractionSuccessScore) { this.extractionSuccessScore = extractionSuccessScore; }
    
    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }
    
    public Map<String, Object> getNativeData() { return nativeData; }
    public void setNativeData(Map<String, Object> nativeData) { this.nativeData = nativeData; }
    
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
    
    public String getCompanyFunding() { return companyFunding; }
    public void setCompanyFunding(String companyFunding) { this.companyFunding = companyFunding; }
    
    public List<String> getCompanyIndustries() { return companyIndustries; }
    public void setCompanyIndustries(List<String> companyIndustries) { this.companyIndustries = companyIndustries; }
    
    public Boolean getRemoteOk() { return remoteOk; }
    public void setRemoteOk(Boolean remoteOk) { this.remoteOk = remoteOk; }
    
    public Boolean getVisaSponsorship() { return visaSponsorship; }
    public void setVisaSponsorship(Boolean visaSponsorship) { this.visaSponsorship = visaSponsorship; }
    
    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
    
    public Boolean getEquityOffered() { return equityOffered; }
    public void setEquityOffered(Boolean equityOffered) { this.equityOffered = equityOffered; }
    
    /**
     * Calculate quality score based on field completeness
     */
    public void calculateQualityScore() {
        int totalFields = 15;
        int filledFields = 0;
        
        if (title != null && !title.trim().isEmpty()) filledFields++;
        if (description != null && !description.trim().isEmpty()) filledFields++;
        if (location != null && !location.trim().isEmpty()) filledFields++;
        if (jobType != null && !jobType.trim().isEmpty()) filledFields++;
        if (applyUrl != null && !applyUrl.trim().isEmpty()) filledFields++;
        if (companyName != null && !companyName.trim().isEmpty()) filledFields++;
        if (salaryMin != null || salaryMax != null || salaryText != null) filledFields++;
        if (skills != null && !skills.isEmpty()) filledFields++;
        if (experienceLevel != null && !experienceLevel.trim().isEmpty()) filledFields++;
        if (postedDate != null) filledFields++;
        if (companySize != null && !companySize.trim().isEmpty()) filledFields++;
        if (remoteOk != null) filledFields++;
        if (benefits != null && !benefits.isEmpty()) filledFields++;
        if (companyIndustries != null && !companyIndustries.isEmpty()) filledFields++;
        if (companyFunding != null && !companyFunding.trim().isEmpty()) filledFields++;
        
        double score = (double) filledFields / totalFields * 100.0;
        this.extractionSuccessScore = Math.round(score * 100.0) / 100.0;
        
        // Assign quality grade
        if (score >= 90) this.qualityGrade = "A";
        else if (score >= 80) this.qualityGrade = "B";
        else if (score >= 70) this.qualityGrade = "C";
        else if (score >= 60) this.qualityGrade = "D";
        else this.qualityGrade = "F";
    }
    
    @Override
    public String toString() {
        return String.format("WellfoundJob{id='%s', title='%s', company='%s', location='%s', score=%.1f%%}", 
                id, title, companyName, location, extractionSuccessScore != null ? extractionSuccessScore : 0.0);
    }
}