package com.pharmacy.iposca.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class EmailEndpoint {
    private static final String URL = "http://127.0.0.1:9000/api/email/send";
    /** Constructor for backward compatibility */
    public EmailEndpoint(String json) throws IOException, InterruptedException {
        send(json);
    }

    private static void send(String json) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("Email API Success: HTTP " + response.statusCode());
            return;
        }
        String serverError = "Email API returned HTTP " + response.statusCode() + " | Body: " + response.body();
        System.err.println(serverError);
        throw new IOException(serverError);
    }

    public static boolean sendSafe(String json) {
        try {
            new EmailEndpoint(json);
            return true;
        } catch (ConnectException e) {
            System.err.println("ConnectException: Cannot reach localhost:8888. Is the EmailAPI server running?");
            return false;
        } catch (IOException e) {
            System.err.println("Email API Error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getClass().getSimpleName() + " - " + (e.getMessage() != null ? e.getMessage() : "null"));
            return false;
        }
    }
}