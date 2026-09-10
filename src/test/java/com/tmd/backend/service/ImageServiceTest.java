package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.image.ImageUpload;
import com.tmd.backend.domain.image.ImageUploadStatus;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.S3Client;
import com.tmd.backend.repository.ImageUploadRepository;
import com.tmd.backend.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Presigner r2Presigner;
    @Mock private S3Client r2Client;
    @Mock private UserRepository userRepository;
    @Mock private ImageUploadRepository imageUploadRepository;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(
            r2Presigner,
            r2Client,
            userRepository,
            imageUploadRepository,
            "test-bucket",
            "https://images.example.com/",
            600,
            24,
            10 * 1024 * 1024,
            100
        );
    }

    @Test
    void signsContentTypeAndExactFileSize() throws MalformedURLException {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://example.com/upload").toURL());
        given(r2Presigner.presignPutObject(org.mockito.ArgumentMatchers.any(PutObjectPresignRequest.class)))
            .willReturn(presigned);
        User user = mock(User.class);
        given(user.getId()).willReturn(7L);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        PresignedUrlResponse response = imageService.initiateUpload(
            "user@example.com",
            new PresignedUrlRequest(ImageUsage.REVIEW, "DOG.JPG", "IMAGE/JPEG", 1234)
        );

        ArgumentCaptor<PutObjectPresignRequest> captor =
            ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        org.mockito.Mockito.verify(r2Presigner).presignPutObject(captor.capture());
        PutObjectPresignRequest captured = captor.getValue();
        PutObjectRequest putRequest = captured.putObjectRequest();

        assertThat(putRequest.bucket()).isEqualTo("test-bucket");
        assertThat(putRequest.key()).matches("tmp/7/[0-9a-f-]+\\.jpg");
        assertThat(putRequest.contentType()).isEqualTo("image/jpeg");
        assertThat(putRequest.contentLength()).isEqualTo(1234L);
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(response.getUploadId()).isNotNull();
        assertThat(response.getRequiredHeaders()).containsEntry("Content-Type", "image/jpeg");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
    }

    @Test
    void rejectsContentTypeThatDoesNotMatchExtension() {
        assertThatThrownBy(() -> imageService.initiateUpload("user@example.com",
            new PresignedUrlRequest(ImageUsage.REVIEW, "dog.png", "image/jpeg", 1234)
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE));

        verifyNoInteractions(r2Presigner);
    }

    @Test
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> imageService.initiateUpload("user@example.com",
            new PresignedUrlRequest(ImageUsage.REVIEW, "dog.webp", "image/webp", 10 * 1024 * 1024L + 1)
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_SIZE));

        verifyNoInteractions(r2Presigner);
    }

    @Test
    void completesUploadedObjectAndPromotesItToReady() {
        UUID uploadId = UUID.randomUUID();
        ImageUpload upload = mock(ImageUpload.class);
        given(upload.getStatus()).willReturn(ImageUploadStatus.PENDING);
        given(upload.getTemporaryKey()).willReturn("tmp/7/" + uploadId + ".jpg");
        given(upload.getFinalKey()).willReturn("images/review/" + uploadId + ".jpg");
        given(upload.getContentType()).willReturn("image/jpeg");
        given(upload.getExpectedSize()).willReturn(1234L);
        given(upload.getId()).willReturn(uploadId);
        given(imageUploadRepository.findOwnedForUpdate(uploadId, "user@example.com"))
            .willReturn(Optional.of(upload));
        given(r2Client.headObject(any(HeadObjectRequest.class))).willReturn(
            HeadObjectResponse.builder().contentType("image/jpeg").contentLength(1234L).build());

        imageService.completeUpload("user@example.com", uploadId);

        verify(upload).markPromoting();
        verify(upload).markReady(any());
        verify(r2Client).copyObject(any(CopyObjectRequest.class));
        verify(r2Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void rejectsAlreadyAttachedUpload() {
        UUID uploadId = UUID.randomUUID();
        ImageUpload upload = mock(ImageUpload.class);
        given(upload.getStatus()).willReturn(ImageUploadStatus.ATTACHED);
        given(imageUploadRepository.findOwnedForUpdate(uploadId, "user@example.com"))
            .willReturn(Optional.of(upload));

        assertThatThrownBy(() -> imageService.attachReadyUpload(
            "user@example.com", uploadId, ImageUsage.REVIEW))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_UPLOAD_ALREADY_USED));
    }

    @Test
    void deletesExpiredTemporaryAndFinalObjectsThenRemovesUploadRecord() {
        ImageUpload upload = mock(ImageUpload.class);
        given(upload.getTemporaryKey()).willReturn("tmp/7/expired.jpg");
        given(upload.getFinalKey()).willReturn("images/review/expired.jpg");
        given(imageUploadRepository.findExpiredForUpdate(any(), any(), any()))
            .willReturn(java.util.List.of(upload));

        imageService.cleanupExpiredUploads();

        verify(r2Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
        verify(imageUploadRepository).delete(upload);
    }

    @Test
    void sdkSignsUploadRestrictionsAsHeaders() {
        try (S3Presigner presigner = S3Presigner.builder()
            .endpointOverride(URI.create("https://example.r2.cloudflarestorage.com"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("access-key", "secret-key")))
            .region(Region.of("auto"))
            .build()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket("test-bucket")
                .key("images/test.png")
                .contentType("image/png")
                .contentLength(1234L)
                .build();

            PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(putRequest)
                    .build()
            );

            assertThat(presigned.signedHeaders()).containsKeys("content-type", "content-length");
        }
    }
}
