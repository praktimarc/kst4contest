package kst4contest.view.map;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP server that proxies OSM tile requests via Java's HttpClient,
 * so JavaFX WebView only needs to connect to localhost (avoids WebKit SSL/sandbox
 * issues in AppImage and Flatpak packaging). Uses only java.base + java.net.http.
 */
final class TileProxyServer {

    private static final int CACHE_MAX = 512;
    private static final String USER_AGENT = "kst4contest/1.0 amateur-radio-contest-tool";

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private final Map<String, byte[]> cache;

    TileProxyServer() throws IOException {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > CACHE_MAX;
            }
        });

        this.serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));

        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "tile-proxy");
            t.setDaemon(true);
            return t;
        });

        executor.submit(this::acceptLoop);
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    void stop() {
        try { serverSocket.close(); } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[TileProxy] accept error: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET ")) {
                sendError(out, 400, "Bad Request");
                return;
            }

            // Drain headers
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) { /* skip */ }

            // Parse: GET /tiles/{s}/{z}/{x}/{y}.png HTTP/1.1
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) { sendError(out, 400, "Bad Request"); return; }
            String path = parts[1];

            String[] segments = path.split("/");
            // segments: ["", "tiles", s, z, x, "y.png"]
            if (segments.length != 6
                    || !segments[2].matches("[abc]")
                    || !segments[3].matches("\\d{1,2}")
                    || !segments[4].matches("\\d+")
                    || !segments[5].matches("\\d+\\.png")) {
                sendError(out, 404, "Not Found");
                return;
            }

            String cacheKey = segments[2] + "/" + segments[3] + "/" + segments[4] + "/" + segments[5];
            byte[] tileData = cache.get(cacheKey);

            if (tileData == null) {
                String tileUrl = "https://" + segments[2] + ".tile.openstreetmap.org/"
                        + segments[3] + "/" + segments[4] + "/" + segments[5];

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tileUrl))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() != 200) {
                    sendError(out, 502, "OSM returned " + response.statusCode());
                    return;
                }

                tileData = response.body();
                cache.put(cacheKey, tileData);
            }

            String header = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: image/png\r\n"
                    + "Content-Length: " + tileData.length + "\r\n"
                    + "Cache-Control: max-age=86400\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(header.getBytes());
            out.write(tileData);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[TileProxy] error: " + e.getMessage());
        }
    }

    private static void sendError(OutputStream out, int code, String message) throws IOException {
        byte[] body = message.getBytes();
        String response = "HTTP/1.1 " + code + " " + message + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        out.write(response.getBytes());
        out.write(body);
    }
}
