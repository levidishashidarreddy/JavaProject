package com.netsentinel.web.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NetSentinel Firebase Configuration.
 * 
 * Manages configuration and connection parameters for Google Firebase:
 * - Project ID: netsentinel-1ded9
 * - Database URL: https://netsentinel-1ded9-default-rtdb.firebaseio.com
 * - Client web app parameters
 * - Graceful fallback to local SQLite when cloud credentials are not supplied
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${netsentinel.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${netsentinel.firebase.config-path:}")
    private String configPath;

    @Value("${netsentinel.firebase.database-url:https://netsentinel-1ded9-default-rtdb.firebaseio.com}")
    private String databaseUrl;

    @Value("${netsentinel.firebase.project-id:netsentinel-1ded9}")
    private String projectId;

    @Value("${netsentinel.firebase.api-key:${FIREBASE_API_KEY:}}")
    private String apiKey;

    @Value("${netsentinel.firebase.auth-domain:${FIREBASE_AUTH_DOMAIN:netsentinel-1ded9.firebaseapp.com}}")
    private String authDomain;

    @Value("${netsentinel.firebase.storage-bucket:${FIREBASE_STORAGE_BUCKET:netsentinel-1ded9.firebasestorage.app}}")
    private String storageBucket;

    @Value("${netsentinel.firebase.messaging-sender-id:${FIREBASE_MESSAGING_SENDER_ID:}}")
    private String messagingSenderId;

    @Value("${netsentinel.firebase.app-id:${FIREBASE_APP_ID:}}")
    private String appId;

    @Value("${netsentinel.firebase.measurement-id:${FIREBASE_MEASUREMENT_ID:}}")
    private String measurementId;

    private boolean initialized = false;

    @Bean
    public FirebaseDatabase firebaseDatabase() {
        if (!firebaseEnabled) {
            log.info("[FIREBASE-INTEGRATION] Firebase is disabled via configuration.");
            return null;
        }

        try {
            InputStream serviceAccount = resolveCredentialsStream();
            if (serviceAccount == null) {
                log.info("[FIREBASE-INTEGRATION] Service account file not detected. Using REST sync client for: {}", databaseUrl);
                return null;
            }

            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (databaseUrl != null && !databaseUrl.isBlank()) {
                builder.setDatabaseUrl(databaseUrl);
            }

            List<FirebaseApp> apps = FirebaseApp.getApps();
            FirebaseApp app;
            if (apps == null || apps.isEmpty()) {
                app = FirebaseApp.initializeApp(builder.build());
            } else {
                app = apps.get(0);
            }

            initialized = true;
            log.info("[FIREBASE-INTEGRATION] Successfully initialized Firebase Admin SDK for application: {}", app.getName());
            return FirebaseDatabase.getInstance(app);
        } catch (Exception e) {
            log.warn("[FIREBASE-INTEGRATION] Notice during Firebase Admin SDK initialization: {}. Falling back to REST mode.", e.getMessage());
            return null;
        }
    }

    private InputStream resolveCredentialsStream() {
        try {
            if (configPath != null && !configPath.isBlank()) {
                if (Files.exists(Paths.get(configPath))) {
                    return new FileInputStream(configPath);
                }
            }
            String envPath = System.getenv("FIREBASE_CONFIG_PATH");
            if (envPath != null && !envPath.isBlank() && Files.exists(Paths.get(envPath))) {
                return new FileInputStream(envPath);
            }
            File localFile = new File("firebase-service-account.json");
            if (localFile.exists()) {
                return new FileInputStream(localFile);
            }
        } catch (Exception e) {
            log.debug("Credentials stream resolution exception: {}", e.getMessage());
        }
        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isFirebaseEnabled() {
        return firebaseEnabled;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAuthDomain() {
        return authDomain;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public String getMessagingSenderId() {
        return messagingSenderId;
    }

    public String getAppId() {
        return appId;
    }

    public String getMeasurementId() {
        return measurementId;
    }

    public Map<String, Object> getClientConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", apiKey);
        config.put("authDomain", authDomain);
        config.put("databaseURL", databaseUrl);
        config.put("projectId", projectId);
        config.put("storageBucket", storageBucket);
        config.put("messagingSenderId", messagingSenderId);
        config.put("appId", appId);
        config.put("measurementId", measurementId);
        return config;
    }
}
