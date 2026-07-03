package com.example.groupshop.upload;

import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.upload.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadControllerTest extends MockMvcTestBase {

    private static final String MOCK_LOGIN_URL = "/api/v1/auth/mock-login";
    private static final String UPLOAD_URL = "/api/v1/my/uploads/images";
    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00
    };

    @Test
    void uploadImage_shouldFailWhenNotAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart(UPLOAD_URL).file(file))
                .andExpect(status().isUnauthorized())
                .andExpectAll(errorResult("UNAUTHORIZED"));
    }

    @Test
    void uploadImage_shouldSucceedAndExposePublicUrl() throws Exception {
        String token = loginAndGetToken("13800009001");
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", PNG_BYTES);

        String body = mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpectAll(successResult())
                .andExpect(jsonPath("$.data.url", startsWith("http://localhost:8080/uploads/images/")))
                .andExpect(jsonPath("$.data.objectKey", startsWith("images/")))
                .andExpect(jsonPath("$.data.originalFilename").value("cover.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(PNG_BYTES.length))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String objectKey = body.split("\"objectKey\":\"")[1].split("\"")[0];
        mockMvc.perform(get("/uploads/" + objectKey))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void publicUploadUrl_shouldReturnNotFoundForMissingFile() throws Exception {
        mockMvc.perform(get("/uploads/images/missing.png"))
                .andExpect(status().isNotFound())
                .andExpectAll(errorResult("RESOURCE_NOT_FOUND"));
    }

    @Test
    void uploadImage_shouldRejectEmptyFile() throws Exception {
        String token = loginAndGetToken("13800009002");
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void uploadImage_shouldRejectUnsupportedType() throws Exception {
        String token = loginAndGetToken("13800009003");
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    @Test
    void uploadImage_shouldRejectOversizedFile() throws Exception {
        String token = loginAndGetToken("13800009004");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[(int) ImageUploadService.MAX_IMAGE_SIZE_BYTES + 1]
        );

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpectAll(errorResult("VALIDATION_ERROR"));
    }

    private String loginAndGetToken(String phone) throws Exception {
        String body = mockMvc.perform(post(MOCK_LOGIN_URL)
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }
}
