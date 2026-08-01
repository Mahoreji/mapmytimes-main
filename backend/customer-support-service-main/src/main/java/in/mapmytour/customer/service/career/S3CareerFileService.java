package in.mapmytour.customer.service.career;

import in.mapmytour.customer.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3CareerFileService {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${career.resume.s3-folder:careers/resumes}")
    private String resumeFolder;

    private S3Client s3Client;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public record ResumeUploadResult(String s3Key, String url, String originalFileName) {}

    public ResumeUploadResult uploadResume(MultipartFile file, String applicantId) {
        log.debug("Starting resume upload for applicant: {}", applicantId);
        
        if (file == null || file.isEmpty()) {
            throw new ServiceException("Resume file cannot be empty");
        }

        // Validate size <= 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ServiceException("Resume file size exceeds 5MB limit");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
        }

        // Validate extension
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new ServiceException("Invalid file name");
        }
        
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServiceException("Invalid file extension. Only .pdf, .doc, and .docx are allowed.");
        }

        String s3Key = String.format("%s/%s/%s%s", resumeFolder, applicantId, UUID.randomUUID(), extension);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
            
            log.info("Resume uploaded to S3: key={}", s3Key);
            return new ResumeUploadResult(s3Key, url, originalFileName);

        } catch (IOException e) {
            log.error("Failed to upload resume to S3: {}", e.getMessage());
            throw new ServiceException("Failed to upload resume: " + e.getMessage());
        }
    }

    public void deleteResume(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Resume deleted from S3: key={}", s3Key);
        } catch (Exception e) {
            log.warn("Failed to delete resume from S3 (non-blocking): {}", e.getMessage());
        }
    }
}
