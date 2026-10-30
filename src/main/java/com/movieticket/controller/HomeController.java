package com.movieticket.controller;

import com.movieticket.os.OsSystemInspector;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Provides a clean localhost landing page explaining the server status,
 * live OS diagnostics (Concept 6), and socket/API connection instructions.
 */
@RestController
public class HomeController {

    private final OsSystemInspector osSystemInspector;

    public HomeController(OsSystemInspector osSystemInspector) {
        this.osSystemInspector = osSystemInspector;
    }

    @GetMapping(value = "/status", produces = MediaType.TEXT_HTML_VALUE)
    public String getHomeStatus() {
        Map<String, Object> diag = osSystemInspector.getSystemDiagnostics();
        String template = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>CineBook - Localhost Server Status</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0b1120; color: #f8fafc; padding: 40px 20px; margin: 0; }
                .card { background: #1e293b; border-radius: 14px; padding: 32px; max-width: 820px; margin: 0 auto; box-shadow: 0 20px 40px rgba(0,0,0,0.6); border: 1px solid #334155; }
                h1 { color: #38bdf8; margin: 0 0 10px 0; font-size: 1.8rem; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 20px; font-weight: 700; background: #10b981; color: #022c22; font-size: 0.85rem; margin-bottom: 20px; }
                .desc { color: #cbd5e1; line-height: 1.6; font-size: 1rem; }
                .section { margin-top: 28px; border-top: 1px solid #334155; padding-top: 22px; }
                h3 { color: #f1f5f9; margin-top: 0; font-size: 1.2rem; }
                .code-block { background: #0f172a; padding: 14px 18px; border-radius: 8px; font-family: Consolas, monospace; color: #38bdf8; border: 1px solid #1e293b; margin: 8px 0; line-height: 1.5; }
                table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #334155; font-size: 0.95rem; }
                th { color: #94a3b8; font-weight: 600; width: 250px; }
                td { color: #f1f5f9; }
                .tag { background: #0284c7; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
            </style>
        </head>
        <body>
            <div class="card">
                <h1>🎬 CineBook: Movie Ticket Booking System</h1>
                <span class="badge">● LOCALHOST SERVER ACTIVE & ONLINE</span>
                <p class="desc">The application backend is actively running on <strong>localhost</strong> strictly limited to the <strong>6 Core Java Concepts</strong> (Fundamentals, Collections, Spring Boot, Databases, Networking Sockets, and Java for OS).</p>
                
                <div class="section">
                    <h3>🔌 How to Connect & Use the Application</h3>
                    <p style="color: #94a3b8; margin-bottom: 6px;"><strong>Option A — Interactive Java Console Client (Concept 5: Java TCP Sockets):</strong></p>
                    <div class="code-block">.\\run_client.bat<br># or in PowerShell: .\\run_client.ps1</div>
                    <p style="color: #94a3b8; margin: 12px 0 6px 0;"><strong>Option B — Core RESTful Endpoints (Port 8080):</strong></p>
                    <div class="code-block">
                        GET <a href="/api/movies" style="color: #38bdf8;">/api/movies</a> — List active movie catalog<br>
                        GET <a href="/api/theatres" style="color: #38bdf8;">/api/theatres</a> — List multiplex cinema theatres<br>
                        GET <a href="/api/shows/upcoming/movie/1" style="color: #38bdf8;">/api/shows/upcoming/movie/1</a> — View movie shows<br>
                        GET <a href="/api/seats/show/1" style="color: #38bdf8;">/api/seats/show/1</a> — View seat layout & pricing
                    </div>
                </div>

                <div class="section">
                    <h3>🖥️ Concept 6: Live Java Operating System Diagnostics</h3>
                    <table>
                        <tr><th>Operating System</th><td>%s (v%s, %s)</td></tr>
                        <tr><th>OS Process ID (PID)</th><td><span class="tag">PID %s</span></td></tr>
                        <tr><th>Java Environment</th><td>%s (%s)</td></tr>
                        <tr><th>Hardware CPU Cores</th><td>%s processor threads</td></tr>
                        <tr><th>JVM Memory Allocation</th><td>%s MB used / %s MB allocated (Max: %s MB)</td></tr>
                        <tr><th>Network TCP Socket</th><td><strong>localhost:8888</strong> (Listening for Java Clients)</td></tr>
                        <tr><th>HTTP Web Server</th><td><strong>localhost:8080</strong> (Listening for HTTP Requests)</td></tr>
                    </table>
                </div>
            </div>
        </body>
        </html>
        """;

        return template.formatted(
                diag.get("os_name"), diag.get("os_version"), diag.get("os_arch"),
                diag.get("current_os_pid"),
                diag.get("java_version"), diag.get("java_vendor"),
                diag.get("available_cpu_cores"),
                diag.get("jvm_used_memory_mb"), diag.get("jvm_total_memory_mb"), diag.get("jvm_max_memory_mb")
        );
    }
}
