package io.smartfleet.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for current user information extracted from JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}
