package io.smartfleet.auth.controller;

import io.smartfleet.auth.dto.UserInfoDto;
import io.smartfleet.common.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller for user authentication and information endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ApiResponse<UserInfoDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserInfoDto userInfo = UserInfoDto.builder()
                .userId(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .roles(extractRoles(jwt))
                .build();

        return ApiResponse.of(userInfo);
    }

    @GetMapping("/validate")
    public ApiResponse<Boolean> validateToken(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(jwt != null && jwt.getExpiresAt() != null);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }
}
