package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.section.CreateSectionRequest;
import in.mapmytour.blog.dto.request.section.UpdateSectionRequest;
import in.mapmytour.blog.dto.response.section.SectionResponse;

import java.util.List;

public interface SectionService {
    SectionResponse createSection(CreateSectionRequest request);
    SectionResponse getSection(String sectionId);
    SectionResponse getSectionBySlug(String slug);
    List<SectionResponse> getAllSections();
    List<SectionResponse> getSectionHierarchy();
    SectionResponse updateSection(String sectionId, UpdateSectionRequest request);
    void deleteSection(String sectionId);
}
