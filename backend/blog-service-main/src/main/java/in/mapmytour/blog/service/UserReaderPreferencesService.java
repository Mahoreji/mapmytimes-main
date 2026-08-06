package in.mapmytour.blog.service;

import in.mapmytour.blog.dto.request.readingprogress.UpsertReaderPrefsRequest;
import in.mapmytour.blog.dto.response.readingprogress.UserReaderPreferencesResponse;

public interface UserReaderPreferencesService {
    UserReaderPreferencesResponse upsert(String userId, UpsertReaderPrefsRequest request);
    UserReaderPreferencesResponse get(String userId);
}
