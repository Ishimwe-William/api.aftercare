package com.bunsen.api.aftercare.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleTokenService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenService.class);

    @Value("#{'${google.oauth.client-ids}'.split(',')}")
    private List<String> clientIds;

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService() {
        JsonFactory jsonFactory = new GsonFactory();
        NetHttpTransport transport = new NetHttpTransport();

        verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(clientIds)
                .build();
    }

    public GoogleIdToken verifyToken(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            logger.error("Google token verification failed: Token is null");
            throw new IllegalArgumentException("Invalid Google token: Token verification failed");
        }
        return idToken;
    }

    public GoogleUserInfo getUserInfo(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");

        logger.debug(pictureUrl);

        return new GoogleUserInfo(email, emailVerified, name, pictureUrl, givenName, familyName);
    }

    @Getter
    @AllArgsConstructor
    public static class GoogleUserInfo {
        private final String email;
        private final boolean emailVerified;
        private final String name;
        private final String pictureUrl;
        private final String givenName;
        private final String familyName;
    }
}