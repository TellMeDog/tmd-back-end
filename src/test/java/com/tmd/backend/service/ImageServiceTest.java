package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Presigner r2Presigner;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(
            r2Presigner,
            "test-bucket",
            "https://images.example.com/",
            600,
            10 * 1024 * 1024
        );
    }

    @Test
    void signsContentTypeAndExactFileSize() throws MalformedURLException {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://example.com/upload").toURL());
        given(r2Presigner.presignPutObject(org.mockito.ArgumentMatchers.any(PutObjectPresignRequest.class)))
            .willReturn(presigned);

        PresignedUrlResponse response = imageService.generatePresignedUrl(
            new PresignedUrlRequest("DOG.JPG", "IMAGE/JPEG", 1234)
        );

        ArgumentCaptor<PutObjectPresignRequest> captor =
            ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        org.mockito.Mockito.verify(r2Presigner).presignPutObject(captor.capture());
        PutObjectPresignRequest captured = captor.getValue();
        PutObjectRequest putRequest = captured.putObjectRequest();

        assertThat(putRequest.bucket()).isEqualTo("test-bucket");
        assertThat(putRequest.key()).matches("images/[0-9a-f-]+\\.jpg");
        assertThat(putRequest.contentType()).isEqualTo("image/jpeg");
        assertThat(putRequest.contentLength()).isEqualTo(1234L);
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(response.getImageUrl()).startsWith("https://images.example.com/images/");
        assertThat(response.getRequiredHeaders()).containsEntry("Content-Type", "image/jpeg");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
    }

    @Test
    void rejectsContentTypeThatDoesNotMatchExtension() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl(
            new PresignedUrlRequest("dog.png", "image/jpeg", 1234)
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_TYPE));

        verifyNoInteractions(r2Presigner);
    }

    @Test
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl(
            new PresignedUrlRequest("dog.webp", "image/webp", 10 * 1024 * 1024L + 1)
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_SIZE));

        verifyNoInteractions(r2Presigner);
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
