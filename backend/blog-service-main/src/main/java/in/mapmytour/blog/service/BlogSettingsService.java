package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.blogsettings.CreateSettingRequest;
import in.mapmytour.blog.dto.request.blogsettings.UpdateSettingRequest;
import in.mapmytour.blog.dto.response.BlogStatsResponse;
import in.mapmytour.blog.dto.response.blogsettings.BlogSettingsResponse;

import java.util.List;
import java.util.Map;

public interface BlogSettingsService {
    BlogSettingsResponse createSetting(CreateSettingRequest request);
    BlogSettingsResponse getSetting(String settingKey);
    List<BlogSettingsResponse> getAllSettings();
    Map<String, String> getSettingsMap();
    BlogSettingsResponse updateSetting(String settingKey, UpdateSettingRequest request);
    void deleteSetting(String settingKey);
    BlogStatsResponse getBlogStats();
}