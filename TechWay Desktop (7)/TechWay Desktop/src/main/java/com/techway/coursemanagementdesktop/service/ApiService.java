package com.techway.coursemanagementdesktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import com.techway.coursemanagementdesktop.*;
import com.techway.coursemanagementdesktop.dto.*;
import com.techway.coursemanagementdesktop.model.*;
import com.techway.coursemanagementdesktop.util.HttpRequestUtil;
import com.techway.coursemanagementdesktop.util.SessionManager;
import okhttp3.*;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.json.JSONObject;

import static com.techway.coursemanagementdesktop.util.HttpRequestUtil.sendPostRequest;


/**
 * API Service for communicating with Spring Boot backend
 */
public class ApiService {

    private static ApiService instance;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private String baseUrl;

    private final HttpClient httpClient;


    // JSON MediaType
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // ===== Base paths =====
    private static final String FAVORITES_BASE = "/api/favorites";
    private static final String ADMIN_BASE     = "/api/admin";
    private static final String ENROLLMENTS_BASE = "/api/enrollments";


    // Cookie storage (session-based auth support)
    private final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();
    private volatile String xsrfToken;
    private final Gson gson = new Gson();

    public ApiService() {
        httpClient = HttpClient.newHttpClient();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        if (cookies == null || cookies.isEmpty()) return;
                        cookieStore.put(url.host(), cookies);
                        for (Cookie c : cookies) {
                            if ("XSRF-TOKEN".equalsIgnoreCase(c.name())) {
                                xsrfToken = c.value();
                            }
                        }
                    }
                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return cookieStore.getOrDefault(url.host(), Collections.emptyList());
                    }
                })
                .addInterceptor(chain -> {
                    Request req = chain.request();
                    Request.Builder b = req.newBuilder();
                    if (xsrfToken != null && !"GET".equalsIgnoreCase(req.method())) {
                        b.header("X-XSRF-TOKEN", xsrfToken);
                    }
                    return chain.proceed(b.build());
                })
                .build();
    }



    public static List<LessonProgress> parseLessonProgressList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<LessonProgress>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    public static void initialize(String baseUrl) {
        if (instance == null) {
            instance = new ApiService();
        }
        instance.baseUrl = baseUrl;
    }

    public static ApiService getInstance() {
        if (instance == null) throw new IllegalStateException("ApiService not initialized. Call initialize() first.");
        return instance;
    }

    // ========================= AUTH =========================

    public CompletableFuture<LoginResponse> login(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LoginRequest loginRequest = new LoginRequest(email, password);
                String json = objectMapper.writeValueAsString(loginRequest);

                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/auth/login")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Login failed: HTTP " + response.code() + " - " + responseBody);
                    }

                    LoginResponse out = null;
                    try {
                        ApiResponse<LoginResponse> apiResponse = objectMapper.readValue(
                                responseBody,
                                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoginResponse.class)
                        );
                        out = apiResponse.getData();
                    } catch (Exception ignore) { }

                    if (out == null) {
                        JsonNode root = objectMapper.readTree(responseBody);
                        JsonNode dataNode = root.path("data");
                        out = new LoginResponse();
                        if (dataNode.isObject()) {
                            if (dataNode.has("user")) {
                                out.setUser(objectMapper.treeToValue(dataNode.get("user"), User.class));
                            } else if (dataNode.has("id") || dataNode.has("email")) {
                                out.setUser(objectMapper.treeToValue(dataNode, User.class));
                            }
                            if (dataNode.has("token") && !dataNode.get("token").isNull())
                                out.setToken(dataNode.get("token").asText());
                            else if (dataNode.has("accessToken"))
                                out.setToken(dataNode.get("accessToken").asText());
                            else if (dataNode.has("jwt"))
                                out.setToken(dataNode.get("jwt").asText());
                        }
                    }

                    if (out != null && (out.getToken() == null || out.getToken().isBlank())) {
                        String authHeader = response.header("Authorization");
                        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
                            out.setToken(authHeader.substring(7).trim());
                        }
                    }

                    if (out == null || out.getUser() == null) {
                        throw new RuntimeException("Login error: user missing in response");
                    }

                    return out;
                }
            } catch (IOException e) {
                throw new RuntimeException("Login error: network/IO - " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Login error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<User> register(String name, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RegisterRequest registerRequest = new RegisterRequest(name, email, password);
                String json = objectMapper.writeValueAsString(registerRequest);

                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/auth/register")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Registration failed: HTTP " + response.code() + " - " + responseBody);
                    }

                    ApiResponse<User> apiResponse = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, User.class)
                    );
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                throw new RuntimeException("Registration error: " + e.getMessage(), e);
            }
        });
    }

    // ========================= COURSES =========================

    public CompletableFuture<List<Course>> getAllCourses() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/courses")
                        .get()
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch courses: HTTP " + response.code() + " - " + responseBody);
                    }
                    ApiResponse<List<Course>> apiResponse = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructParametricType(
                                    ApiResponse.class,
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Course.class)
                            )
                    );
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching courses: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Course> getCourseById(Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/courses/" + courseId)
                        .get()
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch course: HTTP " + response.code() + " - " + responseBody);
                    }
                    ApiResponse<Course> apiResponse = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, Course.class)
                    );
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching course: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Course>> searchCourses(String keyword, String location, Boolean isFree) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/api/courses/search").newBuilder();
                if (keyword != null && !keyword.trim().isEmpty()) urlBuilder.addQueryParameter("keyword", keyword.trim());
                if (location != null && !location.trim().isEmpty()) urlBuilder.addQueryParameter("location", location.trim());
                if (isFree != null) urlBuilder.addQueryParameter("isFree", isFree.toString());

                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .get()
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Search failed: HTTP " + response.code() + " - " + responseBody);
                    }
                    ApiResponse<List<Course>> apiResponse = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructParametricType(
                                    ApiResponse.class,
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Course.class)
                            )
                    );
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                throw new RuntimeException("Search error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Course>> getFreeCourses() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/courses/free")
                        .get()
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch free courses: HTTP " + response.code() + " - " + responseBody);
                    }
                    ApiResponse<List<Course>> apiResponse = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructParametricType(
                                    ApiResponse.class,
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Course.class)
                            )
                    );
                    return apiResponse.getData();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching free courses: " + e.getMessage(), e);
            }
        });
    }

    // ========================= FAVORITES (Wishlist) =========================

    private <T> T extractDataOrWhole(String body, TypeReference<T> typeRef) throws IOException {
        JsonNode root;
        try { root = objectMapper.readTree(body); }
        catch (Exception e) { return objectMapper.readValue(body, typeRef); }
        JsonNode data = root.get("data");
        if (data != null && !data.isNull()) return objectMapper.convertValue(data, typeRef);
        return objectMapper.readValue(body, typeRef);
    }

    private Request.Builder withAuth(Request.Builder builder) {
        String token = null;
        try { token = SessionManager.getInstance().getAuthToken(); } catch (Exception ignore) {
            System.err.println("ERRORRRRRRRRRRRR");
            ignore.printStackTrace();}
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.header("Accept", "application/json");
        if (xsrfToken != null) builder.header("X-XSRF-TOKEN", xsrfToken);
        return builder;
    }

    public CompletableFuture<List<Long>> getFavoriteIds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = SessionManager.getInstance().getCurrentUserId();
                if (userId == null) return new ArrayList<>();

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + FAVORITES_BASE + "/user/" + userId)
                        .get()).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch favorite ids: HTTP " + response.code() + " - " + body);
                    }
                    List<Course> courses = extractDataOrWhole(body, new TypeReference<List<Course>>() {});
                    List<Long> ids = new ArrayList<>();
                    for (Course c : courses) {
                        if (c != null && c.getId() != null) ids.add(c.getId());
                    }
                    return ids;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching favorite ids: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Course>> getFavorites() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = SessionManager.getInstance().getCurrentUserId();
                if (userId == null) return new ArrayList<>();

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + FAVORITES_BASE + "/user/" + userId)
                        .get()).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Failed to fetch favorites: HTTP " + response.code() + " - " + body);
                    }
                    return extractDataOrWhole(body, new TypeReference<List<Course>>() {});
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching favorites: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> isFavorite(Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = SessionManager.getInstance().getCurrentUserId();
                if (userId == null || courseId == null) return false;

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + FAVORITES_BASE + "/user/" + userId + "/course/" + courseId + "/check")
                        .get()).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        if (response.code() == 404) return false;
                        throw new RuntimeException("Failed to check favorite: HTTP " + response.code() + " - " + body);
                    }
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        if (root.has("data")) {
                            JsonNode d = root.get("data");
                            if (d.isBoolean()) return d.asBoolean();
                        }
                        if (root.has("success") && root.get("success").asBoolean()) {
                            return true;
                        }
                        return objectMapper.readValue(body, Boolean.class);
                    } catch (Exception ignore) {
                        return false;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error checking favorite: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Course>> getFavoritesByUserId(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/favorites/user/" + userId) // تأكد من مسار الـ API عندك
                        .get()
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        ApiResponse<List<Course>> apiResponse = objectMapper.readValue(
                                responseBody,
                                objectMapper.getTypeFactory().constructParametricType(
                                        ApiResponse.class,
                                        objectMapper.getTypeFactory().constructCollectionType(List.class, Course.class)
                                )
                        );
                        return apiResponse.getData();
                    } else {
                        throw new RuntimeException("Failed to fetch favorites: HTTP "
                                + response.code() + " - " + responseBody);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching favorites: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> addFavorite(Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = SessionManager.getInstance().getCurrentUserId();
                if (userId == null || courseId == null) return false;

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("userId", userId);
                requestBody.put("courseId", courseId);

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + FAVORITES_BASE))
                        .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) return true;
                    throw new RuntimeException("Add favorite failed: HTTP " + response.code() + " - " + body);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error adding favorite: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> removeFavorite(Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = SessionManager.getInstance().getCurrentUserId();
                if (userId == null || courseId == null) return false;

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + FAVORITES_BASE + "/user/" + userId + "/course/" + courseId))
                        .delete()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) return true;
                    if (response.code() == 404) return true;
                    throw new RuntimeException("Remove favorite failed: HTTP " + response.code() + " - " + body);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error removing favorite: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> toggleFavorite(Long courseId) {
        return isFavorite(courseId).thenCompose(current -> {
            if (Boolean.TRUE.equals(current)) {
                return removeFavorite(courseId).thenApply(ok -> ok ? false : true);
            } else {
                return addFavorite(courseId).thenApply(ok -> ok ? true : false);
            }
        });
    }

    // ========================= ADMIN =========================

    public CompletableFuture<List<Course>> adminListCourses(String q) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + ADMIN_BASE + "/courses";
                if (q != null && !q.isBlank()) {
                    url += "?q=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
                }
                Request request = withAuth(new Request.Builder().url(url).get()).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Admin list courses failed: HTTP " + response.code() + ", error: " + body);
                    }
                    return extractDataOrWhole(body, new TypeReference<List<Course>>() {});
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin list courses error: " + e.getMessage(), e);
            }
        });
    }

    public static class CourseAdminRequest {
        public String title, description, location, instructor, status, imageUrl;
        public Double price;
        public Integer duration;
        public Boolean isFree;
    }

    public CompletableFuture<Boolean> adminCreateCourse(Object form) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CourseAdminRequest reqObj = toCourseAdminRequest(form);
                // إضافة isFree بناء على السعر
                if (reqObj.isFree == null) {
                    reqObj.isFree = reqObj.price == null || reqObj.price <= 0.0;
                }

                Request req = withAuth(new Request.Builder()
                        .url(baseUrl + ADMIN_BASE + "/courses"))
                        .post(RequestBody.create(objectMapper.writeValueAsBytes(reqObj), JSON))
                        .build();
                try (Response res = client.newCall(req).execute()) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) throw new RuntimeException("Create course failed: HTTP " + res.code() + " - " + body);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin create course error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> adminUpdateCourse(long id, Object form) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CourseAdminRequest reqObj = toCourseAdminRequest(form);
                // إضافة isFree بناء على السعر
                if (reqObj.isFree == null) {
                    reqObj.isFree = reqObj.price == null || reqObj.price <= 0.0;
                }

                Request req = withAuth(new Request.Builder()
                        .url(baseUrl + ADMIN_BASE + "/courses/" + id))
                        .put(RequestBody.create(objectMapper.writeValueAsBytes(reqObj), JSON))
                        .build();
                try (Response res = client.newCall(req).execute()) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) throw new RuntimeException("Update course failed: HTTP " + res.code() + " - " + body);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin update course error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> adminDeleteCourse(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request req = withAuth(new Request.Builder()
                        .url(baseUrl + ADMIN_BASE + "/courses/" + id))
                        .delete()
                        .build();
                try (Response res = client.newCall(req).execute()) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) throw new RuntimeException("Delete course failed: HTTP " + res.code() + " - " + body);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin delete course error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<User>> adminListUsers(String q) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + ADMIN_BASE + "/users";
                if (q != null && !q.isBlank()) url += "?q=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
                Request request = withAuth(new Request.Builder().url(url).get()).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Admin list users failed: HTTP " + response.code() + ", error: " + body);
                    }
                    return extractDataOrWhole(body, new TypeReference<List<User>>() {});
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin list users error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> put(String path, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                // .header("Authorization", "Bearer " + token) // إذا تستخدم توكن
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() >= 200 && response.statusCode() < 300);
    }


    // داخل ApiService.java
    public CompletableFuture<DashboardStats> getDashboardStats() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl+ADMIN_BASE + "/dashboard"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> gson.fromJson(json, DashboardStats.class));
    }



    public CompletableFuture<Boolean> adminSetRole(long userId, String role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("role", role);

                Request req = withAuth(new Request.Builder()
                        .url(baseUrl + ADMIN_BASE + "/users/" + userId + "/role"))
                        .put(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON))
                        .build();
                try (Response res = client.newCall(req).execute()) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) throw new RuntimeException("Set role failed: HTTP " + res.code() + " - " + body);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin set role error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> adminDisableUser(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // نظراً لعدم وجود endpoint للتعطيل في الباك إند، نعطي false مباشرة
                return false;
            } catch (Exception e) {
                throw new RuntimeException("Admin disable user error: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> adminDeleteUser(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request req = withAuth(new Request.Builder()
                        .url(baseUrl + ADMIN_BASE + "/users/" + userId))
                        .delete()
                        .build();
                try (Response res = client.newCall(req).execute()) {
                    String body = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) throw new RuntimeException("Delete user failed: HTTP " + res.code() + " - " + body);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Admin delete user error: " + e.getMessage(), e);
            }
        });
    }



    public AdminOverviewDTO fetchDashboardStats() {
        try {
            // إعداد الطلب
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl+ADMIN_BASE + "/dashboard"))
                    .header("Accept", "application/json")
                    .build();

            // إرسال الطلب واستلام الاستجابة
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // التحقق من نجاح الطلب
            if (response.statusCode() == 200) {
                // تحويل JSON إلى كائن Java
                String responseBody = response.body();
                Type type = new TypeToken<ApiResponse<AdminOverviewDTO>>() {}.getType();
                ApiResponse<AdminOverviewDTO> apiResponse = gson.fromJson(responseBody, type);
                return apiResponse.getData();
            } else {
                System.err.println("فشل في جلب البيانات: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<AdminUserDTO> fetchAllUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ADMIN_BASE + "/users"))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                Type type = new TypeToken<ApiResponse<List<AdminUserDTO>>>() {}.getType();
                ApiResponse<List<AdminUserDTO>> apiResponse = gson.fromJson(responseBody, type);
                return apiResponse.getData();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }





    // ========================= UTIL =========================

    private CourseAdminRequest toCourseAdminRequest(Object form) {
        return objectMapper.convertValue(form, CourseAdminRequest.class);
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) { super(message); }
        public ApiException(String message, Throwable cause) { super(message, cause); }
    }

    // تسجيل المستخدم في كورس
    public CompletableFuture<Enrollment> enrollUser(Long userId, Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Long> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("courseId", courseId);

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + ENROLLMENTS_BASE)
                        .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                ).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Enrollment failed: HTTP " + response.code() + " - " + body);
                    }
                    if (body.isEmpty()) {
                        // التعامل مع الاستجابة الفارغة بشكل مناسب
                        return null;
                    }
                    return objectMapper.readValue(body, Enrollment.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error enrolling user: " + e.getMessage(), e);
            }
        });
    }


    // تسجيل المستخدم في كورس
    public CompletableFuture<EnrollmentDTO> enrollUser2(Long userId, Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Long> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("courseId", courseId);

                Request request = withAuth(new Request.Builder()
                        .url(baseUrl + ENROLLMENTS_BASE)
                        .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                ).build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Enrollment failed: HTTP " + response.code() + " - " + body);
                    }

                    // استقبل DTO من الـ backend
                    return objectMapper.readValue(body, EnrollmentDTO.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error enrolling user: " + e.getMessage(), e);
            }
        });
    }


    // ========================= ENROLLMENTS =========================


    public CompletableFuture<List<EnrollmentDTO>> getUserEnrollments(Long userId) {
        String url = baseUrl + "/api/enrollments/user/" + userId;

        return HttpRequestUtil.sendGetRequest(url)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        JsonArray dataArray = json.getAsJsonArray("data");

                        Type listType = new TypeToken<List<EnrollmentDTO>>() {}.getType();
                        return new Gson().fromJson(dataArray, listType);

                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("فشل في معالجة استجابة التسجيلات", e);
                    }
                });
    }








    public CompletableFuture<Boolean> deleteEnrollment(Long enrollmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/enrollments/" + enrollmentId)
                        .delete()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error deleting enrollment: " + e.getMessage(), e);
            }
        });
    }


    // ✅ أضف هذه الدالة 👇
    public CompletableFuture<List<Map<String, Object>>> getLessonsForCourse(Long courseId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/courses/" + courseId + "/lessons"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return List.of();
                    }
                });
    }




    public void fetchLessonProgressByEnrollment(Long enrollmentId, Consumer<List<LessonProgress>> onSuccess, Consumer<Throwable> onError) {
        String url = baseUrl + "/api/lesson-progress/by-enrollment/" + enrollmentId;

        HttpRequestUtil.sendGetRequest("http://localhost:8080/api/lesson-progress/by-enrollment/14")
                .thenAccept(json -> {
                    List<LessonProgress> progresses = ApiService.parseLessonProgressList(json);
                    progresses.forEach(p -> System.out.println("Lesson ID: " + p.getId() + ", Completed: " + p.isCompleted()));

                    // هنا تقدر تحدث الواجهة، مثلاً:
                    // tableView.setItems(FXCollections.observableArrayList(progresses));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }



    public CompletableFuture<LessonAccessResult> checkLessonAccess(Long lessonId, Long userId) {
        HttpClient client = HttpClient.newHttpClient();
        String url = String.format("http://localhost:8080/api/lessons/%d/user/%d", lessonId, userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    String body = response.body();

                    if (status == 200) {
                        return new LessonAccessResult(true, "تم الوصول بنجاح");
                    } else {
                        // حاول قراءة رسالة من الـ JSON إن وُجد
                        try {
                            JSONObject json = new JSONObject(body);
                            String message = json.optString("message", "الوصول مرفوض لهذا الدرس.");
                            return new LessonAccessResult(false, message);
                        } catch (Exception ex) {
                            return new LessonAccessResult(false, "الوصول مرفوض لهذا الدرس.");
                        }
                    }
                })
                .exceptionally(e -> {
                    return new LessonAccessResult(false, "حدث خطأ أثناء التحقق من الوصول.");
                });
    }






    public CompletableFuture<Boolean> hasUserPaid(Long userId, Long courseId) {
        String url = baseUrl + "/api/payments/has-paid?userId=" + userId + "&courseId=" + courseId;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    // افترض ان الرد هو نص "true" او "false"
                    return Boolean.parseBoolean(body.trim());
                });
    }


    public CompletableFuture<PaymentResult> makePayment(Long userId, Long courseId) {
        return CompletableFuture.supplyAsync(() -> {
            // محاكاة نجاح دائم للدفع
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            return result;
        });
    }


    public CompletableFuture<Boolean> isUserEnrolled(Long userId, Long courseId) {
        String url = baseUrl + "/api/enrollments/is-enrolled?userId=" + userId + "&courseId=" + courseId;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> Boolean.parseBoolean(body.trim()));
    }

    public CompletableFuture<CourseStatus> getUserCourseStatus(Long userId, Long courseId) {
        String url = String.format("http://localhost:8080/api/courses/%d/user/%d/status", courseId, userId);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        boolean enrolled = json.getBoolean("enrolled");
                        boolean hasPaid = json.getBoolean("hasPaid");
                        return new CourseStatus(enrolled, hasPaid);
                    } else {
                        throw new RuntimeException("Failed to get course status");
                    }
                });
    }



    public CompletableFuture<Boolean> enrollUser3(Long userId, Long courseId) {
        String url = "http://localhost:8080/api/enrollments";

        JSONObject json = new JSONObject();
        json.put("userId", userId);
        json.put("courseId", courseId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .header("Content-Type", "application/json")
                .build();

        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 201);
    }


    public CompletableFuture<Long> getEnrollmentId(Long userId, Long courseId) {
        String url = baseUrl + "/api/enrollments/user/" + userId + "/course/" + courseId + "/id";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(response.body());
                            return root.get("enrollmentId").asLong();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    } else {
                        return null;
                    }
                });
    }


    public CompletableFuture<Enrollment> markPaid(Long userId, Course course, String paymentReference) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(userId);
        enrollment.setCourse(course);
        enrollment.setPaid(true);
        return CompletableFuture.completedFuture(enrollment);
    }

    public CompletableFuture<EnrollmentDTO> markPaid(Long enrollmentId) {
        String url = String.format("http://localhost:8080/api/payments/markPaid/%d", enrollmentId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        JSONObject data = json.getJSONObject("data");

                        EnrollmentDTO dto = new EnrollmentDTO();
                        dto.setId(data.getLong("id"));
                        dto.setUserId(data.getLong("userId"));
                        dto.setCourseId(data.getLong("courseId"));
                        dto.setPaid(data.getBoolean("paid"));
                        return dto;
                    } else {
                        throw new RuntimeException("Request failed. Status: " + response.statusCode());
                    }
                });
    }






    public CompletableFuture<EnrollmentDTO> getEnrollmentDetails(Long userId, Long courseId) {
        String url = baseUrl + "/api/enrollments/user/" + userId + "/course/" + courseId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(response.body(), EnrollmentDTO.class);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل تفاصيل التسجيل: " + e.getMessage(), e);
                        }
                    } else if (response.statusCode() == 404) {
                        return null; // غير مسجل
                    } else {
                        throw new RuntimeException("خطأ في جلب تفاصيل التسجيل: " + response.statusCode());
                    }
                });
    }







    public CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public <T> CompletableFuture<T> sendAsync(HttpRequest request, TypeReference<T> typeReference) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    int statusCode = response.statusCode();

                    System.out.println("Response body: [" + body + "]");

                    if (statusCode >= 200 && statusCode < 300) {
                        try {
                            if (body == null || body.trim().isEmpty()) {
                                return null;
                            }
                            return objectMapper.readValue(body, typeReference);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse JSON", e);
                        }
                    } else {
                        // نحاول نقرأ الرسالة من الاستجابة حتى لو كانت خطأ
                        String errorMessage = "Request failed with status " + statusCode;
                        try {
                            Map<String, Object> errorMap = objectMapper.readValue(body, new TypeReference<>() {});
                            if (errorMap.containsKey("message")) {
                                errorMessage = (String) errorMap.get("message");
                            }
                        } catch (Exception ignored) {
                            // في حال لم تكن JSON نترك الرسالة العامة
                        }

                        throw new RuntimeException(errorMessage);
                    }
                });
    }




    // دالة POST ترجع CompletableFuture<T>
    public <T> CompletableFuture<T> postJson(String path, Object body, TypeReference<T> typeReference) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return sendAsync(request, typeReference);

        } catch (Exception e) {
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }


    /**
     * جلب جميع شهادات المستخدم - API موجود في Backend
     */
    /**
     * إضافات كاملة لـ ApiService للكويزات والشهادات
     * أضف هذه المethods للكلاس ApiService الموجود لديك
     */

// ========================= QUIZ APIs - تطابق QuizController =========================

    /**
     * جلب كويز الكورس
     * GET /api/quiz/course/{courseId}
     */
    public CompletableFuture<Quiz> getQuizByCourseId(Long courseId) {
        String url = baseUrl + "/api/quiz/course/" + courseId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(response.body(), Quiz.class);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل بيانات الكويز: " + e.getMessage());
                        }
                    } else if (response.statusCode() == 404) {
                        return null; // لا يوجد كويز
                    } else {
                        throw new RuntimeException("خطأ في جلب الكويز: " + response.statusCode());
                    }
                });
    }

    /**
     * إنشاء كويز للكورس (Admin only)
     * POST /api/quiz/course/{courseId}
     */
    public CompletableFuture<Quiz> createCourseQuiz(Long courseId, Quiz quiz) {
        String url = baseUrl + "/api/quiz/course/" + courseId;

        try {
            String jsonBody = objectMapper.writeValueAsString(quiz);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                return objectMapper.readValue(response.body(), Quiz.class);
                            } catch (Exception e) {
                                throw new RuntimeException("فشل في تحليل الكويز المُنشأ: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("فشل في إنشاء الكويز: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<Quiz> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في إنشاء طلب الكويز: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * تحديث كويز (Admin only)
     * PUT /api/quiz/{quizId}
     */
    public CompletableFuture<Quiz> updateQuiz(Long quizId, Quiz quiz) {
        String url = baseUrl + "/api/quiz/" + quizId;

        try {
            String jsonBody = objectMapper.writeValueAsString(quiz);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                return objectMapper.readValue(response.body(), Quiz.class);
                            } catch (Exception e) {
                                throw new RuntimeException("فشل في تحليل الكويز المُحدث: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("فشل في تحديث الكويز: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<Quiz> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في تحديث الكويز: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * بدء محاولة جديدة
     * POST /api/quiz/{quizId}/start?userId={userId}
     */
    public CompletableFuture<QuizAttempt> startQuizAttempt(Long quizId, Long userId) {
        String url = baseUrl + "/api/quiz/" + quizId + "/start?userId=" + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            return objectMapper.readValue(response.body(), QuizAttempt.class);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل بيانات المحاولة: " + e.getMessage());
                        }
                    } else if (response.statusCode() == 403) {
                        throw new RuntimeException("غير مسموح لك بالوصول لهذا الكويز");
                    } else {
                        throw new RuntimeException("فشل في بدء المحاولة: " + response.statusCode());
                    }
                });
    }

    /**
     * تسجيل إجابة
     * POST /api/quiz/attempt/{attemptId}/answer
     */
    public CompletableFuture<QuizAttempt> submitAnswer(Long attemptId, Long questionId, Long selectedOptionId) {
        String url = baseUrl + "/api/quiz/attempt/" + attemptId + "/answer";

        try {
            Map<String, Object> answerData = new HashMap<>();
            answerData.put("questionId", questionId);
            answerData.put("selectedOptionId", selectedOptionId);

            String jsonBody = objectMapper.writeValueAsString(answerData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                // استخراج attempt من الاستجابة
                                JsonNode root = objectMapper.readTree(response.body());
                                JsonNode attemptNode = root.get("attempt");
                                return objectMapper.treeToValue(attemptNode, QuizAttempt.class);
                            } catch (Exception e) {
                                throw new RuntimeException("فشل في تحليل استجابة الإجابة: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("فشل في حفظ الإجابة: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<QuizAttempt> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في إرسال الإجابة: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * إنهاء المحاولة
     * POST /api/quiz/attempt/{attemptId}/complete
     */
    public CompletableFuture<Map<String, Object>> completeQuizAttempt(Long attemptId) {
        String url = baseUrl + "/api/quiz/attempt/" + attemptId + "/complete";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                            return gson.fromJson(response.body(), mapType);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل نتائج الكويز: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("فشل في إنهاء المحاولة: " + response.statusCode());
                    }
                });
    }

    /**
     * جلب محاولات المستخدم
     * GET /api/quiz/{quizId}/attempts?userId={userId}
     */
    public CompletableFuture<List<QuizAttempt>> getUserQuizAttempts(Long quizId, Long userId) {
        String url = baseUrl + "/api/quiz/" + quizId + "/attempts?userId=" + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            Type listType = new TypeToken<List<QuizAttempt>>() {}.getType();
                            return gson.fromJson(response.body(), listType);
                        } catch (Exception e) {
                            return new ArrayList<>();
                        }
                    } else {
                        return new ArrayList<>();
                    }
                });
    }

    /**
     * جلب أفضل محاولة
     * GET /api/quiz/{quizId}/best-attempt?userId={userId}
     */
    public CompletableFuture<QuizAttempt> getBestAttempt(Long quizId, Long userId) {
        String url = baseUrl + "/api/quiz/" + quizId + "/best-attempt?userId=" + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(response.body(), QuizAttempt.class);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل أفضل محاولة: " + e.getMessage());
                        }
                    } else if (response.statusCode() == 404) {
                        throw new RuntimeException("لا توجد محاولات للمستخدم");
                    } else {
                        throw new RuntimeException("فشل في جلب أفضل محاولة: " + response.statusCode());
                    }
                });
    }

    /**
     * إضافة سؤال للكويز (Admin only)
     * POST /api/quiz/{quizId}/questions
     */
    public CompletableFuture<Question> addQuestionToQuiz(Long quizId, Question question) {
        String url = baseUrl + "/api/quiz/" + quizId + "/questions";

        try {
            String jsonBody = objectMapper.writeValueAsString(question);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                return objectMapper.readValue(response.body(), Question.class);
                            } catch (Exception e) {
                                throw new RuntimeException("فشل في تحليل السؤال المُنشأ: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("فشل في إضافة السؤال: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<Question> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في إضافة السؤال: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * حذف كويز (Admin only)
     * DELETE /api/quiz/{quizId}
     */
    public CompletableFuture<Boolean> deleteQuiz(Long quizId) {
        String url = baseUrl + "/api/quiz/" + quizId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    return response.statusCode() >= 200 && response.statusCode() < 300;
                });
    }

    /**
     * إحصائيات الكويز (Admin only)
     * GET /api/quiz/{quizId}/statistics
     */
    public CompletableFuture<Map<String, Object>> getQuizStatistics(Long quizId) {
        String url = baseUrl + "/api/quiz/" + quizId + "/statistics";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                            return gson.fromJson(response.body(), mapType);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل إحصائيات الكويز: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("فشل في جلب إحصائيات الكويز: " + response.statusCode());
                    }
                });
    }

// ========================= CERTIFICATE APIs - تطابق CertificateController =========================

    /**
     * جلب شهادة المستخدم للكورس
     * GET /api/certificates/user/{userId}/course/{courseId}
     */
    public CompletableFuture<Optional<CertificateDTO>> getUserCourseCertificate(Long userId, Long courseId) {
        String url = baseUrl + "/api/certificates/user/" + userId + "/course/" + courseId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            CertificateDTO cert = objectMapper.readValue(response.body(), CertificateDTO.class);
                            return Optional.of(cert);
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    } else if (response.statusCode() == 404) {
                        return Optional.empty();
                    } else {
                        throw new RuntimeException("فشل في جلب شهادة المستخدم: " + response.statusCode());
                    }
                });
    }

    /**
     * جلب جميع شهادات المستخدم
     * GET /api/certificates/user/{userId}
     */
    public CompletableFuture<List<CertificateDTO>> getUserCertificates(Long userId) {
        String url = baseUrl + "/api/certificates/user/" + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            Type listType = new TypeToken<List<CertificateDTO>>() {}.getType();
                            List<Object> rawList = gson.fromJson(response.body(), listType);

                            List<CertificateDTO> certificates = new ArrayList<>();
                            for (Object item : rawList) {
                                if (item instanceof Map) {
                                    // تحويل Map إلى CertificateDTO
                                    String itemJson = gson.toJson(item);
                                    CertificateDTO cert = gson.fromJson(itemJson, CertificateDTO.class);
                                    certificates.add(cert);
                                }
                            }
                            return certificates;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return new ArrayList<>();
                        }
                    } else {
                        return new ArrayList<>();
                    }
                });
    }

    /**
     * توليد شهادة يدوياً (Admin only)
     * POST /api/certificates/generate
     */
    public CompletableFuture<CertificateDTO> generateCertificate(Long userId, Long courseId, Double finalScore, Double quizScore) {
        String url = baseUrl + "/api/certificates/generate";

        try {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("userId", userId);
            requestData.put("courseId", courseId);
            requestData.put("finalScore", finalScore);
            requestData.put("quizScore", quizScore);

            String jsonBody = objectMapper.writeValueAsString(requestData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                return objectMapper.readValue(response.body(), CertificateDTO.class);
                            } catch (Exception e) {
                                throw new RuntimeException("فشل في تحليل الشهادة المُنشأة: " + e.getMessage());
                            }
                        } else {
                            throw new RuntimeException("فشل في إنشاء الشهادة: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<CertificateDTO> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في إنشاء الشهادة: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * البحث عن شهادة برقم معين
     * GET /api/certificates/{certificateNumber}
     */
    public CompletableFuture<Optional<CertificateDTO>> getCertificateByNumber(String certificateNumber) {
        String url = baseUrl + "/api/certificates/" + certificateNumber;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            CertificateDTO cert = objectMapper.readValue(response.body(), CertificateDTO.class);
                            return Optional.of(cert);
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    } else {
                        return Optional.empty();
                    }
                });
    }

    /**
     * التحقق من صحة الشهادة
     * GET /api/certificates/verify/{certificateNumber}
     */
    public CompletableFuture<Map<String, Object>> verifyCertificate(String certificateNumber) {
        String url = baseUrl + "/api/certificates/verify/" + certificateNumber;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                        return gson.fromJson(response.body(), mapType);
                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("valid", false);
                        errorResult.put("message", "خطأ في التحقق من الشهادة");
                        return errorResult;
                    }
                });
    }

    /**
     * تحميل PDF الشهادة
     * GET /api/certificates/{certificateId}/download
     */
    public CompletableFuture<Path> downloadCertificatePdf(Long certificateId, String userName) {
        String url = baseUrl + "/api/certificates/" + certificateId + "/download";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/pdf")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            // مجلد التنزيل
                            Path downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads");
                            if (!Files.exists(downloadsDir)) {
                                Files.createDirectories(downloadsDir);
                            }

                            String fileName = userName.replaceAll("\\s+", "_") + "_certificate_" + certificateId + ".pdf";
                            Path filePath = downloadsDir.resolve(fileName);

                            Files.write(filePath, response.body());
                            return filePath;
                        } catch (Exception e) {
                            throw new RuntimeException("فشل حفظ ملف الشهادة: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("فشل تحميل ملف الشهادة: رمز الحالة " + response.statusCode());
                    }
                });
    }

    /**
     * عرض PDF الشهادة في المتصفح
     * GET /api/certificates/{certificateId}/view
     */
    public void openCertificateInBrowser(Long certificateId) {
        String url = baseUrl + "/api/certificates/" + certificateId + "/view";

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("فشل في فتح المتصفح: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("النظام لا يدعم فتح المتصفح");
        }
    }

    /**
     * إلغاء شهادة (Admin only)
     * POST /api/certificates/{certificateId}/revoke
     */
    public CompletableFuture<Boolean> revokeCertificate(Long certificateId, String reason) {
        String url = baseUrl + "/api/certificates/" + certificateId + "/revoke";

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() >= 200 && response.statusCode() < 300);

        } catch (Exception e) {
            CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("خطأ في إلغاء الشهادة: " + e.getMessage()));
            return failedFuture;
        }
    }

    /**
     * إحصائيات الشهادات لكورس (Admin only)
     * GET /api/certificates/course/{courseId}/statistics
     */
    public CompletableFuture<Map<String, Object>> getCertificateStatistics(Long courseId) {
        String url = baseUrl + "/api/certificates/course/" + courseId + "/statistics";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                            return gson.fromJson(response.body(), mapType);
                        } catch (Exception e) {
                            throw new RuntimeException("فشل في تحليل إحصائيات الشهادات: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("فشل في جلب إحصائيات الشهادات: " + response.statusCode());
                    }
                });
    }

    /**
     * جلب تقدم المستخدم في الكورس
     * GET /api/lesson-progress/by-enrollment/{enrollmentId}
     */
    public CompletableFuture<List<LessonProgress>> getLessonProgressByEnrollmentId(Long enrollmentId) {
        String url = baseUrl + "/api/lesson-progress/by-enrollment/" + enrollmentId;

        return HttpRequestUtil.sendGetRequest(url)
                .thenApply(responseBody -> {
                    Type listType = new TypeToken<List<LessonProgress>>() {}.getType();
                    return new Gson().fromJson(responseBody, listType);
                });
    }

    /**
     * تسجيل درس كمكتمل
     * POST /api/lesson-progress/complete
     */
    public CompletableFuture<Void> markLessonAsCompleted(Long enrollmentId, Long lessonId) {
        String url = baseUrl + "/api/lesson-progress/complete";

        Map<String, Long> payload = new HashMap<>();
        payload.put("enrollmentId", enrollmentId);
        payload.put("lessonId", lessonId);

        String json = new Gson().toJson(payload);

        return HttpRequestUtil.sendPostRequest(url, json)
                .thenAccept(response -> {
                    System.out.println("تم تسجيل مشاهدة الدرس");
                });
    }

    /**
     * حساب نسبة التقدم
     */
    public double calculateProgressPercentage(List<LessonProgress> progressList) {
        if (progressList == null || progressList.isEmpty()) return 0.0;

        long completedCount = progressList.stream()
                .filter(LessonProgress::isCompleted)
                .count();

        return (completedCount * 100.0) / progressList.size();
    }

}







