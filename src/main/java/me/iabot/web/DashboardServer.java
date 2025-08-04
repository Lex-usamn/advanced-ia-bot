package me.iabot.web;

import me.iabot.Main;
import me.iabot.BotAI;
import me.iabot.BotManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;

public class DashboardServer {
    private Main plugin;
    private HttpServer server;
    private int port;
    private AuthManager authManager;
    private WebAPI webAPI;

    public DashboardServer(Main plugin, int port) {
        this.plugin = plugin;
        this.port = port;
        this.authManager = new AuthManager(plugin);
        this.webAPI = new WebAPI(plugin);
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Rotas estáticas
            server.createContext("/", new StaticFileHandler());
            server.createContext("/login", new LoginHandler());
            server.createContext("/logout", new LogoutHandler());
            server.createContext("/dashboard", new DashboardHandler());
            
            // Rotas da API
            server.createContext("/api/bots", new BotsAPIHandler());
            server.createContext("/api/bot", new BotAPIHandler());
            server.createContext("/api/stats", new StatsAPIHandler());
            
            server.setExecutor(null);
            server.start();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao iniciar servidor web: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("")) {
                path = "/index.html";
            }
            
            serveStaticFile(exchange, path);
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Processar login
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse form data
                String[] pairs = requestBody.split("&");
                String username = "";
                String password = "";
                
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        if ("username".equals(key)) username = value;
                        if ("password".equals(key)) password = value;
                    }
                }
                
                if (authManager.authenticate(username, password)) {
                    String sessionId = authManager.createSession(username);
                    Headers headers = exchange.getResponseHeaders();
                    headers.add("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
                    headers.add("Location", "/dashboard");
                    exchange.sendResponseHeaders(302, -1);
                } else {
                    serveStaticFile(exchange, "/login.html?error=1");
                }
            } else {
                serveStaticFile(exchange, "/login.html");
            }
        }
    }

    private class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Set-Cookie", "session=; Path=/; HttpOnly; Max-Age=0");
            headers.add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            
            serveStaticFile(exchange, "/dashboard.html");
        }
    }

    private class BotsAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                String json = webAPI.getBotsList();
                sendJSONResponse(exchange, json, 200);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                // Criar novo bot
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String json = webAPI.createBot(requestBody);
                sendJSONResponse(exchange, json, 201);
            }
        }
    }

    private class BotAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("name=")) {
                String botName = query.split("=")[1];
                
                if ("DELETE".equals(exchange.getRequestMethod())) {
                    String json = webAPI.removeBot(botName);
                    sendJSONResponse(exchange, json, 200);
                } else if ("GET".equals(exchange.getRequestMethod())) {
                    String json = webAPI.getBotDetails(botName);
                    sendJSONResponse(exchange, json, 200);
                }
            } else {
                sendErrorResponse(exchange, "Nome do bot não especificado", 400);
            }
        }
    }

    private class StatsAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            
            String json = webAPI.getStats();
            sendJSONResponse(exchange, json, 200);
        }
    }

    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        try {
            String resourcePath = "web" + path;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (is == null) {
                sendErrorResponse(exchange, "Página não encontrada", 404);
                return;
            }
            
            byte[] content = is.readAllBytes();
            String contentType = getContentType(path);
            
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", contentType);
            
            exchange.sendResponseHeaders(200, content.length);
            OutputStream os = exchange.getResponseBody();
            os.write(content);
            os.close();
            
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erro interno do servidor", 500);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        List<String> cookies = headers.get("Cookie");
        
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("session=")) {
                    String sessionId = cookie.substring(8).split(";")[0];
                    return authManager.isValidSession(sessionId);
                }
            }
        }
        return false;
    }

    private void sendJSONResponse(HttpExchange exchange, String json, int statusCode) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        String json = "{\"error\":\"" + message + "\"}";
        sendJSONResponse(exchange, json, statusCode);
    }

    private void sendUnauthorized(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("WWW-Authenticate", "Bearer realm=\"IA Bot Dashboard\"");
        sendErrorResponse(exchange, "Não autorizado", 401);
    }
}