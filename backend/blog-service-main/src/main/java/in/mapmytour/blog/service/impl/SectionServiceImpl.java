package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.section.CreateSectionRequest;
import in.mapmytour.blog.dto.request.section.UpdateSectionRequest;
import in.mapmytour.blog.dto.response.section.SectionResponse;
import in.mapmytour.blog.entity.Section;
import in.mapmytour.blog.exception.DuplicateResourceException;
import in.mapmytour.blog.exception.ResourceNotFoundException;
import in.mapmytour.blog.exception.BadRequestException;
import in.mapmytour.blog.repository.SectionRepository;
import in.mapmytour.blog.service.SectionService;
import in.mapmytour.blog.utils.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final BlogMapper blogMapper;

    @Override
    public SectionResponse createSection(CreateSectionRequest request) {
        log.info("Creating section with name: {}", request.getName());

        if (sectionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Section with name '" + request.getName() + "' already exists");
        }

        if (sectionRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Section with slug '" + request.getSlug() + "' already exists");
        }

        if (StringUtils.hasText(request.getParentSectionId())) {
            if (!sectionRepository.existsById(request.getParentSectionId())) {
                throw new ResourceNotFoundException("Parent section not found with ID: " + request.getParentSectionId());
            }
        }

        try {
            Section section = Section.builder()
                    .name(request.getName())
                    .slug(request.getSlug())
                    .description(request.getDescription())
                    .icon(request.getIcon())
                    .accentColor(request.getAccentColor())
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .parentSectionId(request.getParentSectionId())
                    .build();

            Section savedSection = sectionRepository.save(section);
            log.info("Section created successfully with ID: {}", savedSection.getId());

            return blogMapper.toSectionResponse(savedSection);
        } catch (Exception e) {
            log.error("Error creating section: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create section: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getSection(String sectionId) {
        log.info("Fetching section with ID: {}", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        SectionResponse response = blogMapper.toSectionResponse(section);

        try {
            Object[] result = sectionRepository.findSectionWithPostCount(sectionId);
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for section {}: {}", sectionId, e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getSectionBySlug(String slug) {
        log.info("Fetching section with slug: {}", slug);

        Section section = sectionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with slug: " + slug));

        SectionResponse response = blogMapper.toSectionResponse(section);

        try {
            Object[] result = sectionRepository.findSectionWithPostCount(section.getId());
            if (result != null && result.length > 1) {
                response.setPostCount(((Number) result[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("Error fetching post count for section {}: {}", section.getId(), e.getMessage());
            response.setPostCount(0);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getAllSections() {
        log.info("Fetching all sections");

        List<Section> sections = sectionRepository.findAll();

        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> sectionPostCounts = sectionRepository.findAllSectionsWithPostCount();
            for (Object[] result : sectionPostCounts) {
                String sectionId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(sectionId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for sections: {}", e.getMessage());
        }

        return sections.stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .map(section -> {
                    SectionResponse response = blogMapper.toSectionResponse(section);
                    response.setPostCount(postCounts.getOrDefault(section.getId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionHierarchy() {
        log.info("Fetching section hierarchy");

        List<Section> allSections = sectionRepository.findAll();

        Map<String, Integer> postCounts = new HashMap<>();
        try {
            List<Object[]> sectionPostCounts = sectionRepository.findAllSectionsWithPostCount();
            for (Object[] result : sectionPostCounts) {
                String sectionId = (String) result[0];
                Integer postCount = ((Number) result[2]).intValue();
                postCounts.put(sectionId, postCount);
            }
        } catch (Exception e) {
            log.warn("Error fetching post counts for sections: {}", e.getMessage());
        }

        Map<String, SectionResponse> sectionMap = new HashMap<>();

        for (Section section : allSections) {
            SectionResponse response = blogMapper.toSectionResponse(section);
            response.setPostCount(postCounts.getOrDefault(section.getId(), 0));
            sectionMap.put(section.getId(), response);
        }

        List<SectionResponse> rootSections = new ArrayList<>();
        for (SectionResponse section : sectionMap.values()) {
            if (section.getParentSectionId() == null) {
                rootSections.add(section);
            } else {
                SectionResponse parent = sectionMap.get(section.getParentSectionId());
                if (parent != null) {
                    if (parent.getSubSections() == null) {
                        parent.setSubSections(new ArrayList<>());
                    }
                    parent.getSubSections().add(section);
                }
            }
        }

        rootSections.sort(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0));
        for (SectionResponse root : rootSections) {
            sortSubSectionsRecursively(root);
        }

        return rootSections;
    }

    private void sortSubSectionsRecursively(SectionResponse section) {
        if (section.getSubSections() != null) {
            section.getSubSections().sort(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0));
            for (SectionResponse sub : section.getSubSections()) {
                sortSubSectionsRecursively(sub);
            }
        }
    }

    @Override
    public SectionResponse updateSection(String sectionId, UpdateSectionRequest request) {
        log.info("Updating section with ID: {}", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        try {
            if (StringUtils.hasText(request.getName()) && !request.getName().equals(section.getName())) {
                if (sectionRepository.existsByName(request.getName())) {
                    throw new DuplicateResourceException("Section with name '" + request.getName() + "' already exists");
                }
                section.setName(request.getName());
            }

            if (StringUtils.hasText(request.getSlug()) && !request.getSlug().equals(section.getSlug())) {
                if (sectionRepository.existsBySlug(request.getSlug())) {
                    throw new DuplicateResourceException("Section with slug '" + request.getSlug() + "' already exists");
                }
                section.setSlug(request.getSlug());
            }

            if (request.getDescription() != null) {
                section.setDescription(request.getDescription());
            }

            if (request.getIcon() != null) {
                section.setIcon(request.getIcon());
            }

            if (request.getAccentColor() != null) {
                section.setAccentColor(request.getAccentColor());
            }

            if (request.getSortOrder() != null) {
                section.setSortOrder(request.getSortOrder());
            }

            if (request.getParentSectionId() != null) {
                if (StringUtils.hasText(request.getParentSectionId()) &&
                        !sectionRepository.existsById(request.getParentSectionId())) {
                    throw new ResourceNotFoundException("Parent section not found with ID: " + request.getParentSectionId());
                }

                if (request.getParentSectionId().equals(sectionId)) {
                    throw new BadRequestException("Section cannot be its own parent");
                }

                section.setParentSectionId(request.getParentSectionId());
            }

            Section updatedSection = sectionRepository.save(section);
            log.info("Section updated successfully with ID: {}", updatedSection.getId());

            return blogMapper.toSectionResponse(updatedSection);
        } catch (Exception e) {
            log.error("Error updating section: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update section: " + e.getMessage());
        }
    }

    @Override
    public void deleteSection(String sectionId) {
        log.info("Deleting section with ID: {}", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        try {
            List<Section> subSections = sectionRepository.findByParentSectionId(sectionId);
            if (!subSections.isEmpty()) {
                throw new BadRequestException("Cannot delete section with sub-sections. Please delete or reassign sub-sections first.");
            }

            try {
                Object[] result = sectionRepository.findSectionWithPostCount(sectionId);
                if (result != null && result.length > 1) {
                    Integer postCount = ((Number) result[1]).intValue();
                    if (postCount > 0) {
                        throw new BadRequestException("Cannot delete section that is being used in blog posts. Please reassign posts to another section first.");
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking post count for section deletion: {}", e.getMessage());
            }

            sectionRepository.delete(section);
            log.info("Section deleted successfully with ID: {}", sectionId);
        } catch (Exception e) {
            log.error("Error deleting section: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete section: " + e.getMessage());
        }
    }
}
