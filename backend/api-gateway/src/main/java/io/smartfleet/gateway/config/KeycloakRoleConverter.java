package io.smartfleet.gateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");

        Stream<String> realmRoles = Stream.empty();
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            realmRoles = ((List<String>) realmAccess.get("roles")).stream();
        }

        Stream<String> resourceRoles = Stream.empty();
        if (resourceAccess != null && resourceAccess.containsKey("backend-services")) {
            Map<String, Object> backendServices = (Map<String, Object>) resourceAccess.get("backend-services");
            if (backendServices.containsKey("roles")) {
                resourceRoles = ((List<String>) backendServices.get("roles")).stream();
            }
        }

        return Stream.concat(realmRoles, resourceRoles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
