package com.movie.api.controller;

import com.movie.api.service.UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadControllerTest {
    private UploadController controller;
    private UploadService service;
    private final MockMultipartFile file = new MockMultipartFile("file", new byte[] {1});

    @BeforeEach
    void setUp() {
        controller = new UploadController();
        service = mock(UploadService.class);
        when(service.checkAndSaveUpload(any())).thenReturn("image-id");
        ReflectionTestUtils.setField(controller, "uploadService", service);
        ReflectionTestUtils.setField(controller, "serverPort", "8888");
    }

    @Test
    void preservesLocalRawUrlContract() throws Exception {
        assertEquals("http://localhost:8888/api/upload?id=image-id", controller.upload(file));
    }

    @Test
    void supportsConfiguredPublicOriginWithoutDoubleSlashes() throws Exception {
        ReflectionTestUtils.setField(controller, "publicUrl", "https://movies.example.com/");
        assertEquals("https://movies.example.com/api/upload?id=image-id", controller.upload(file));
    }

    @Test
    void rejectsInvalidPublicOriginBeforeStoringFile() {
        ReflectionTestUtils.setField(controller, "publicUrl", "javascript:invalid");
        assertThrows(IllegalStateException.class, () -> controller.upload(file));
        verify(service, never()).checkAndSaveUpload(any());
    }

    @Test
    void rejectsEmptyUpload() {
        assertThrows(Exception.class, () -> controller.upload(new MockMultipartFile("file", new byte[0])));
        verify(service, never()).checkAndSaveUpload(any());
    }
}
