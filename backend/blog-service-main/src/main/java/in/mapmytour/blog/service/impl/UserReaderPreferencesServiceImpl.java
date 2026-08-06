package in.mapmytour.blog.service.impl;

import in.mapmytour.blog.dto.request.readingprogress.UpsertReaderPrefsRequest;
import in.mapmytour.blog.dto.response.readingprogress.UserReaderPreferencesResponse;
import in.mapmytour.blog.entity.UserReaderPreferences;
import in.mapmytour.blog.repository.UserReaderPreferencesRepository;
import in.mapmytour.blog.service.UserReaderPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserReaderPreferencesServiceImpl implements UserReaderPreferencesService {

    private static final Set<String> VALID_FONT_STACKS = Set.of("sans", "serif");
    private static final Set<String> VALID_THEMES = Set.of("light", "dark", "sepia");
    private static final Set<String> VALID_LINE_SPACINGS = Set.of("compact", "normal", "relaxed");
    private static final int MIN_FONT_SIZE_IDX = 0;
    private static final int MAX_FONT_SIZE_IDX = 6;

    private final UserReaderPreferencesRepository userReaderPreferencesRepository;

    @Override
    public UserReaderPreferencesResponse upsert(String userId, UpsertReaderPrefsRequest request) {
        log.info("Upserting reader preferences for user {}", userId);

        Optional<UserReaderPreferences> existing = userReaderPreferencesRepository.findByUserId(userId);

        UserReaderPreferences prefs;
        if (existing.isPresent()) {
            prefs = existing.get();
        } else {
            prefs = UserReaderPreferences.builder()
                    .userId(userId)
                    .build();
        }

        if (request.getFontSizeIdx() != null) {
            int idx = request.getFontSizeIdx();
            if (idx < MIN_FONT_SIZE_IDX || idx > MAX_FONT_SIZE_IDX) {
                idx = Math.max(MIN_FONT_SIZE_IDX, Math.min(MAX_FONT_SIZE_IDX, idx));
            }
            prefs.setFontSizeIdx(idx);
        }

        if (request.getFontStack() != null && VALID_FONT_STACKS.contains(request.getFontStack())) {
            prefs.setFontStack(request.getFontStack());
        }

        if (request.getTheme() != null && VALID_THEMES.contains(request.getTheme())) {
            prefs.setTheme(request.getTheme());
        }

        if (request.getLineSpacing() != null && VALID_LINE_SPACINGS.contains(request.getLineSpacing())) {
            prefs.setLineSpacing(request.getLineSpacing());
        }

        UserReaderPreferences saved = userReaderPreferencesRepository.save(prefs);
        log.info("Reader preferences saved for user {}", userId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserReaderPreferencesResponse get(String userId) {
        log.info("Fetching reader preferences for user {}", userId);

        return userReaderPreferencesRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> UserReaderPreferencesResponse.builder()
                        .fontSizeIdx(2)
                        .fontStack("sans")
                        .theme("light")
                        .lineSpacing("normal")
                        .build());
    }

    private UserReaderPreferencesResponse toResponse(UserReaderPreferences prefs) {
        return UserReaderPreferencesResponse.builder()
                .fontSizeIdx(prefs.getFontSizeIdx())
                .fontStack(prefs.getFontStack())
                .theme(prefs.getTheme())
                .lineSpacing(prefs.getLineSpacing())
                .build();
    }
}
