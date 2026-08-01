package in.mapmytour.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class FileUtil {

    // Allowed file types
    public static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    public static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv"
    );

    public static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm"
    );

    // File size limits (in bytes)
    public static final long MAX_IMAGE_SIZE = 50 * 1024 * 1024; // 50MB
    public static final long MAX_DOCUMENT_SIZE = 25 * 1024 * 1024; // 25MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    // File validation methods
    public static boolean isValidImageFile(MultipartFile file) {
        return file != null &&
                !file.isEmpty() &&
                ALLOWED_IMAGE_TYPES.contains(file.getContentType()) &&
                file.getSize() <= MAX_IMAGE_SIZE;
    }

    public static boolean isValidDocumentFile(MultipartFile file) {
        return file != null &&
                !file.isEmpty() &&
                ALLOWED_DOCUMENT_TYPES.contains(file.getContentType()) &&
                file.getSize() <= MAX_DOCUMENT_SIZE;
    }

    public static boolean isValidVideoFile(MultipartFile file) {
        return file != null &&
                !file.isEmpty() &&
                ALLOWED_VIDEO_TYPES.contains(file.getContentType()) &&
                file.getSize() <= MAX_VIDEO_SIZE;
    }

    public static boolean isValidFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return ALLOWED_IMAGE_TYPES.contains(contentType) ||
                ALLOWED_DOCUMENT_TYPES.contains(contentType) ||
                ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    // File name generation
    public static String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = removeFileExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return sanitizeFileName(baseName) + "_" + timestamp + "_" + uuid + extension;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public static String removeFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public static String getFileNameWithoutExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName);
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";

        // Remove or replace invalid characters
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Remove multiple consecutive underscores
        sanitized = sanitized.replaceAll("_{2,}", "_");

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // Ensure the filename is not empty
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        // Limit length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized;
    }

    // File size utilities
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static boolean isFileSizeValid(MultipartFile file, long maxSizeInBytes) {
        return file != null && file.getSize() <= maxSizeInBytes;
    }

    // File operations
    public static String saveFileToLocal(MultipartFile file, String uploadDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);

        // Copy file to destination
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File saved locally: {}", filePath.toString());
        return filePath.toString();
    }

    public static boolean deleteLocalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                log.info("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }

            return deleted;
        } catch (IOException e) {
            log.error("Error deleting file {}: {}", filePath, e.getMessage(), e);
            return false;
        }
    }

    public static boolean fileExists(String filePath) {
        try {
            return Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            log.error("Error checking file existence {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    public static byte[] readFileAsBytes(String filePath) throws IOException {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error reading file as bytes {}: {}", filePath, e.getMessage(), e);
            throw e;
        }
    }

    public static String readFileAsString(String filePath) throws IOException {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error reading file as string {}: {}", filePath, e.getMessage(), e);
            throw e;
        }
    }

    // File type detection
    public static String getFileTypeCategory(String contentType) {
        if (contentType == null) return "unknown";

        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return "image";
        } else if (ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            return "document";
        } else if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return "video";
        }

        return "other";
    }

    public static boolean isImageFile(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    public static boolean isDocumentFile(String contentType) {
        return ALLOWED_DOCUMENT_TYPES.contains(contentType);
    }

    public static boolean isVideoFile(String contentType) {
        return ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    // Directory operations
    public static boolean createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Directory created: {}", directoryPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Error creating directory {}: {}", directoryPath, e.getMessage(), e);
            return false;
        }
    }

    public static boolean deleteDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.error("Error deleting {}: {}", p, e.getMessage());
                            }
                        });
                log.info("Directory deleted: {}", directoryPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Error deleting directory {}: {}", directoryPath, e.getMessage(), e);
            return false;
        }
    }

    // File validation error messages
    public static String getValidationErrorMessage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "File cannot be null or empty";
        }

        String contentType = file.getContentType();
        long fileSize = file.getSize();

        if (!isValidFile(file)) {
            return "Invalid file type. Allowed types: images, documents, videos";
        }

        if (isImageFile(contentType) && fileSize > MAX_IMAGE_SIZE) {
            return "Image file size exceeds maximum limit of " + formatFileSize(MAX_IMAGE_SIZE);
        }

        if (isDocumentFile(contentType) && fileSize > MAX_DOCUMENT_SIZE) {
            return "Document file size exceeds maximum limit of " + formatFileSize(MAX_DOCUMENT_SIZE);
        }

        if (isVideoFile(contentType) && fileSize > MAX_VIDEO_SIZE) {
            return "Video file size exceeds maximum limit of " + formatFileSize(MAX_VIDEO_SIZE);
        }

        return null; // No validation errors
    }

    // Utility method to convert MultipartFile to File
    public static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + generateUniqueFileName(multipartFile.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }
}