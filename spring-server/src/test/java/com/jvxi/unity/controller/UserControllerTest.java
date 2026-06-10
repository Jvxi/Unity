package com.jvxi.unity.controller;

import com.jvxi.unity.model.User;
import com.jvxi.unity.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @TempDir
    Path uploadRoot;

    @Test
    void uploadAvatarStoresImageAndReturnsUrl() {
        UserService userService = mock(UserService.class);
        UserController controller = controllerWith(userService);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] { 1, 2, 3, 4 }
        );

        ResponseEntity<Map<String, Object>> response = controller.uploadAvatar(authFor(42L), file);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        String avatarUrl = String.valueOf(body.get("avatarUrl"));
        assertTrue(avatarUrl.startsWith("/uploads/avatars/avatar_42_"));
        assertTrue(avatarUrl.endsWith(".png"));
        verify(userService).updateAvatar(42L, avatarUrl);

        Path uploadedFile = uploadRoot.resolve("avatars").resolve(Path.of(avatarUrl).getFileName().toString());
        assertTrue(Files.exists(uploadedFile));
    }

    @Test
    void uploadAvatarRejectsNonImageFile() {
        UserController controller = controllerWith(mock(UserService.class));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                new byte[] { 1, 2, 3 }
        );

        ResponseEntity<Map<String, Object>> response = controller.uploadAvatar(authFor(42L), file);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("仅支持上传图片头像", response.getBody().get("error"));
    }

    @Test
    void uploadAvatarRejectsLargeImage() {
        UserController controller = controllerWith(mock(UserService.class));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[(10 * 1024 * 1024) + 1]
        );

        ResponseEntity<Map<String, Object>> response = controller.uploadAvatar(authFor(42L), file);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("头像不能超过 10MB", response.getBody().get("error"));
    }

    private UserController controllerWith(UserService userService) {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "uploadRoot", uploadRoot.toString());
        return controller;
    }

    private Authentication authFor(Long userId) {
        User user = new User();
        user.setId(userId);
        return new UsernamePasswordAuthenticationToken(user, null);
    }
}
