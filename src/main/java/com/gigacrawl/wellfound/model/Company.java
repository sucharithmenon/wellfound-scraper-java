package com.gigacrawl.wellfound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Company model for Wellfound companies with comprehensive metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("slug")
    private String slug;
    
    @JsonProperty("logo")
    private String logo;
    
    @JsonProperty("headline")
    private String headline;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("company_size")
    private String companySize;
    
    @JsonProperty("company_type")
    private String companyType;
    
    @JsonProperty("funding")
    private String funding;
    
    @JsonProperty("website")
    private String website;
    
    @JsonProperty("twitter")
    private String twitter;
    
    @JsonProperty("linkedin")
    private String linkedin;
    
    @JsonProperty("total_jobs")
    private Integer totalJobs;
    
    @JsonProperty("badges")
    private List<String> badges;
    
    @JsonProperty("founded_year")
    private Integer foundedYear;
    
    @JsonProperty("industries")
    private List<String> industries;
    
    @JsonProperty("company_url")
    private String companyUrl;
    
    @JsonProperty("jobs_url")
    private String jobsUrl;
    
    // Extraction metadata
    @JsonProperty("extraction_timestamp")
    private LocalDateTime extractionTimestamp;
    
    @JsonProperty("source_url")
    private String sourceUrl;
    
    @JsonProperty("native_data")
    private Map<String, Object> nativeData;
    
    // Default constructor
    public Company() {
        this.extractionTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { 
        this.slug = slug;
        if (slug != null) {
            this.companyUrl = "https://wellfound.com/company/" + slug;
            this.jobsUrl = "https://wellfound.com/company/" + slug + "/jobs";
        }
    }
    
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
    
    public String getCompanyType() { return companyType; }
    public void setCompanyType(String companyType) { this.companyType = companyType; }
    
    public String getFunding() { return funding; }
    public void setFunding(String funding) { this.funding = funding; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }
    
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    
    public Integer getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Integer totalJobs) { this.totalJobs = totalJobs; }
    
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    
    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }
    
    public List<String> getIndustries() { return industries; }
    public void setIndustries(List<String> industries) { this.industries = industries; }
    
    public String getCompanyUrl() { return companyUrl; }
    public void setCompanyUrl(String companyUrl) { this.companyUrl = companyUrl; }
    
    public String getJobsUrl() { return jobsUrl; }
    public void setJobsUrl(String jobsUrl) { this.jobsUrl = jobsUrl; }
    
    public LocalDateTime getExtractionTimestamp() { return extractionTimestamp; }
    public void setExtractionTimestamp(LocalDateTime extractionTimestamp) { this.extractionTimestamp = extractionTimestamp; }
    
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    
    public Map<String, Object> getNativeData() { return nativeData; }
    public void setNativeData(Map<String, Object> nativeData) { this.nativeData = nativeData; }
    
    @Override
    public String toString() {
        return String.format("Company{id='%s', name='%s', slug='%s', location='%s', totalJobs=%d}", 
                id, name, slug, location, totalJobs);
    }
}