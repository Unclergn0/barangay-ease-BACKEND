
package com.muntinlupa.barangay_ease.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Duration;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_FOLDERS =
            Set.of("complaints", "service-requests");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png");

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucket;
    private final HttpClient httpClient;

    public FileStorageService(
            @Value("${SUPABASE_URL:}") String supabaseUrl,
            @Value("${SUPABASE_SECRET_KEY:}") String secretKey,
            @Value("${SUPABASE_BUCKET:barangay-ease-files}") String bucket) {

        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_URL environment variable is missing.");
        }

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_SECRET_KEY environment variable is missing.");
        }

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_BUCKET environment variable is missing.");
        }

        this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
        this.secretKey = secretKey.trim();
        this.bucket = bucket.trim();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Uploads a validated file to the private Supabase bucket.
     * Returns the object path saved in the existing MySQL file_path column.
     */
    public String save(MultipartFile file, String folder)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file is empty.");
        }

        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException(
                    "Invalid upload destination.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Each uploaded file must be 10 MB or smaller.");
        }

        String originalName = file.getOriginalFilename() == null
                ? "upload"
                : file.getOriginalFilename();

        String extension = extensionOf(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Only PDF, JPG, JPEG and PNG files are allowed.");
        }

        validateContentSignature(file, extension);

        String safeName = UUID.randomUUID()
                + "_"
                + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String objectPath = folder + "/" + safeName;

        byte[] content = file.getBytes();

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = switch (extension) {
                case "pdf" -> "application/pdf";
                case "png" -> "image/png";
                default -> "image/jpeg";
            };
        }

        URI uri = storageUri(
                "/object/" + encodePath(bucket + "/" + objectPath));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("apikey", secretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .header("cache-control", "max-age=3600")
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response = sendString(request);

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw storageError(
                    "upload",
                    response.statusCode(),
                    response.body());
        }

        return objectPath;
    }

    /**
     * Downloads a private Supabase object into a temporary file.
     *
     * This method preserves compatibility with the current controllers,
     * which expect a Path and use FileSystemResource.
     */
    public Path resolve(String relative) throws IOException {

        String objectPath = normalizeObjectPath(relative);

        URI uri = storageUri(
                "/object/authenticated/"
                        + encodePath(bucket + "/" + objectPath));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("apikey", secretKey)
                .GET()
                .build();

        HttpResponse<byte[]> response = sendBytes(request);

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw storageError(
                    "download",
                    response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
        }

        String extension = extensionOf(objectPath);

        String suffix = extension.isBlank()
                ? ".tmp"
                : "." + extension;

        Path temporaryFile = Files.createTempFile(
                "barangay-ease-",
                suffix);

        try {
            Files.write(temporaryFile, response.body());

            // Transitional compatibility with existing controllers.
            temporaryFile.toFile().deleteOnExit();

            return temporaryFile;

        } catch (IOException ex) {
            Files.deleteIfExists(temporaryFile);
            throw ex;
        }
    }

    /**
     * Deletes a private Supabase object.
     * Does not fail silently, so callers can detect storage errors.
     */
    public void delete(String relative) throws IOException {

        if (relative == null || relative.isBlank()) {
            return;
        }

        String objectPath = normalizeObjectPath(relative);

        String json = "{\"prefixes\":[\""
                + objectPath
                + "\"]}";

        URI uri = storageUri(
                "/object/" + encodePath(bucket));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("apikey", secretKey)
                .header("Content-Type", "application/json")
                .method(
                        "DELETE",
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = sendString(request);

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw storageError(
                    "delete",
                    response.statusCode(),
                    response.body());
        }
    }

    private URI storageUri(String path) {
        return URI.create(
                supabaseUrl + "/storage/v1" + path);
    }

    /**
     * Encodes each URL path segment separately.
     * Preserves folder separators.
     */
    private String encodePath(String path) {

        String[] segments = path.split("/");
        StringBuilder encoded = new StringBuilder();

        for (String segment : segments) {

            if (encoded.length() > 0) {
                encoded.append("/");
            }

            encoded.append(
                    URLEncoder.encode(
                            segment,
                            StandardCharsets.UTF_8)
                            .replace("+", "%20"));
        }

        return encoded.toString();
    }

    /**
     * Prevents path traversal and invalid storage object paths.
     */
    private String normalizeObjectPath(String relative)
            throws IOException {

        if (relative == null || relative.isBlank()) {
            throw new IOException("File path is missing.");
        }

        String normalized = relative.replace('\\', '/');

        if (normalized.startsWith("/")
                || normalized.contains("..")
                || normalized.contains("?")
                || normalized.contains("#")) {

            throw new IOException("Invalid storage object path.");
        }

        String[] segments = normalized.split("/");

        if (segments.length < 2) {
            throw new IOException("Invalid storage object path.");
        }

        for (String segment : segments) {

            if (!segment.matches("[a-zA-Z0-9._-]+")
                    || segment.equals(".")
                    || segment.equals("..")) {

                throw new IOException(
                        "Invalid storage object path.");
            }
        }

        if (!ALLOWED_FOLDERS.contains(segments[0])) {
            throw new IOException(
                    "Invalid storage folder.");
        }

        return normalized;
    }

    private HttpResponse<String> sendString(HttpRequest request)
            throws IOException {

        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase request was interrupted.", ex);
        }
    }

    private HttpResponse<byte[]> sendBytes(HttpRequest request)
            throws IOException {

        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase request was interrupted.", ex);
        }
    }

    private IOException storageError(
            String operation,
            int status,
            String responseBody) {

        String details = responseBody == null
                ? ""
                : responseBody.substring(
                        0,
                        Math.min(responseBody.length(), 500));

        return new IOException(
                "Supabase " + operation
                        + " failed. HTTP "
                        + status
                        + ". "
                        + details);
    }

    private String extensionOf(String filename) {

        String clean = filename.replace('\\', '/');

        int slash = clean.lastIndexOf('/');
        clean = clean.substring(slash + 1);

        int dot = clean.lastIndexOf('.');

        return dot < 0
                ? ""
                : clean.substring(dot + 1)
                        .toLowerCase(Locale.ROOT);
    }

    private void validateContentSignature(
            MultipartFile file,
            String extension) throws IOException {

        byte[] header = file.getBytes();

        boolean valid = switch (extension) {

            case "pdf" ->
                    header.length >= 5
                            && header[0] == '%'
                            && header[1] == 'P'
                            && header[2] == 'D'
                            && header[3] == 'F'
                            && header[4] == '-';

            case "png" ->
                    header.length >= 8
                            && (header[0] & 0xff) == 0x89
                            && header[1] == 'P'
                            && header[2] == 'N'
                            && header[3] == 'G'
                            && header[4] == 0x0d
                            && header[5] == 0x0a
                            && header[6] == 0x1a
                            && header[7] == 0x0a;

            case "jpg", "jpeg" ->
                    header.length >= 3
                            && (header[0] & 0xff) == 0xff
                            && (header[1] & 0xff) == 0xd8
                            && (header[2] & 0xff) == 0xff;

            default -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "The uploaded file content does not match its file type.");
        }
    }
}
