package com.movie.api.service.impl;

import com.movie.api.mapper.UploadMapper;
import com.movie.api.model.entity.Upload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadServiceImplTest {
    private UploadMapper mapper;
    private UploadServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(UploadMapper.class);
        service = new UploadServiceImpl();
        ReflectionTestUtils.setField(service, "uploadMapper", mapper);
    }

    @Test
    void storesAllBytesAndTheirDigest() {
        byte[] bytes = new byte[] {1, 2, 3, 4, 5};
        String id = service.checkAndSaveUpload(new MockMultipartFile("file", bytes));
        ArgumentCaptor<Upload> upload = ArgumentCaptor.forClass(Upload.class);
        verify(mapper).insert(upload.capture());
        assertEquals(id, upload.getValue().getId());
        assertArrayEquals(bytes, upload.getValue().getBytes());
        assertEquals("7cfdd07889b3295d6a550914ab35e068", upload.getValue().getMd5());
    }

    @Test
    void reusesExistingUploadWithoutInsertingAgain() {
        when(mapper.selectOne(any())).thenReturn(new Upload("existing", new byte[] {1}, "digest"));
        assertEquals("existing", service.checkAndSaveUpload(new MockMultipartFile("file", new byte[] {1})));
        verify(mapper, never()).insert(any());
    }

    @Test
    void readFailureIsReportedAndNeverStoredAsNullSuccess() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("simulated read failure"));
        assertThrows(IllegalStateException.class, () -> service.checkAndSaveUpload(file));
        verifyNoInteractions(mapper);
    }
}
