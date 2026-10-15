package com.irms.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Cấu hình trang giới thiệu công ty (Company Profile / About Us)
 */
public class CompanyProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DRAFT = "DRAFT";

    private String id;
    private String companyName;
    private String brandName;
    private String slogan;
    private String tagline;
    private String logoUrl;
    private String bannerUrl;
    private String overview;
    private String historyStory;
    private String mission;
    private String vision;
    private String coreValues; // JSON string
    private String cultureDesc;
    private String galleryUrls; // JSON string
    private String perks; // JSON string
    private String headquartersAddress;
    private String contactEmail;
    private String contactPhone;
    private String websiteUrl;
    private String facebookUrl;
    private String linkedinUrl;
    private String youtubeUrl;
    private String status = STATUS_PUBLISHED;
    private String updatedBy;
    private String updatedByName;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Helper items
    public static class CoreValueItem implements Serializable {
        private String icon;
        private String title;
        private String desc;

        public CoreValueItem() {}
        public CoreValueItem(String icon, String title, String desc) {
            this.icon = icon;
            this.title = title;
            this.desc = desc;
        }

        public String getIcon() { return icon != null ? icon : "bi-star"; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
    }

    public static class PerkItem implements Serializable {
        private String icon;
        private String title;
        private String desc;

        public PerkItem() {}
        public PerkItem(String icon, String title, String desc) {
            this.icon = icon;
            this.title = title;
            this.desc = desc;
        }

        public String getIcon() { return icon != null ? icon : "bi-gift"; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
    }

    public CompanyProfile() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getHistoryStory() { return historyStory; }
    public void setHistoryStory(String historyStory) { this.historyStory = historyStory; }

    public String getMission() { return mission; }
    public void setMission(String mission) { this.mission = mission; }

    public String getVision() { return vision; }
    public void setVision(String vision) { this.vision = vision; }

    public String getCoreValues() { return coreValues; }
    public void setCoreValues(String coreValues) { this.coreValues = coreValues; }
    public String getCoreValuesJson() { return coreValues; }
    public void setCoreValuesJson(String coreValues) { this.coreValues = coreValues; }

    public String getCultureDesc() { return cultureDesc; }
    public void setCultureDesc(String cultureDesc) { this.cultureDesc = cultureDesc; }

    public String getGalleryUrls() { return galleryUrls; }
    public void setGalleryUrls(String galleryUrls) { this.galleryUrls = galleryUrls; }
    public String getGalleryUrlsJson() { return galleryUrls; }
    public void setGalleryUrlsJson(String galleryUrls) { this.galleryUrls = galleryUrls; }

    public String getPerks() { return perks; }
    public void setPerks(String perks) { this.perks = perks; }
    public String getPerksJson() { return perks; }
    public void setPerksJson(String perks) { this.perks = perks; }

    public String getHeroBannerUrl() { return bannerUrl; }
    public void setHeroBannerUrl(String heroBannerUrl) { this.bannerUrl = heroBannerUrl; }

    public String getHeadquartersAddress() { return headquartersAddress; }
    public void setHeadquartersAddress(String headquartersAddress) { this.headquartersAddress = headquartersAddress; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Helpers
    public boolean isPublished() {
        return STATUS_PUBLISHED.equalsIgnoreCase(status);
    }

    public String getStatusLabel() {
        return isPublished() ? "Đã xuất bản" : "Bản nháp";
    }

    public String getStatusBadgeClass() {
        return isPublished() ? "bg-success-subtle text-success border border-success-subtle"
                             : "bg-warning-subtle text-warning border border-warning-subtle";
    }

    public List<CoreValueItem> getCoreValueList() {
        if (coreValues == null || coreValues.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<CoreValueItem>>() {}.getType();
            return new Gson().fromJson(coreValues, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setCoreValueList(List<CoreValueItem> list) {
        if (list == null || list.isEmpty()) {
            this.coreValues = "[]";
        } else {
            this.coreValues = new Gson().toJson(list);
        }
    }

    public List<PerkItem> getPerkList() {
        if (perks == null || perks.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<PerkItem>>() {}.getType();
            return new Gson().fromJson(perks, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setPerkList(List<PerkItem> list) {
        if (list == null || list.isEmpty()) {
            this.perks = "[]";
        } else {
            this.perks = new Gson().toJson(list);
        }
    }

    public List<String> getGalleryList() {
        if (galleryUrls == null || galleryUrls.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return new Gson().fromJson(galleryUrls, listType);
        } catch (Exception e) {
            // Fallback: nếu lưu dạng dấu phẩy hoặc newline
            List<String> list = new ArrayList<>();
            for (String s : galleryUrls.split("[,\n\r]+")) {
                if (!s.trim().isEmpty()) list.add(s.trim());
            }
            return list;
        }
    }

    public void setGalleryList(List<String> list) {
        if (list == null || list.isEmpty()) {
            this.galleryUrls = "[]";
        } else {
            this.galleryUrls = new Gson().toJson(list);
        }
    }
}
