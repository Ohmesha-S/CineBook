package com.movieticket.os;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concept 6: Java for Operating System
 * Demonstrates:
 * 1. Java System Properties & OS Environment variables.
 * 2. JVM Runtime memory management (free, total, max memory).
 * 3. CPU hardware concurrency (availableProcessors).
 * 4. Java ProcessHandle (OS Process ID).
 * 5. OS File System roots and storage capacity.
 */
@Component
public class OsSystemInspector {

    public Map<String, Object> getSystemDiagnostics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        // OS Identification
        stats.put("os_name", System.getProperty("os.name"));
        stats.put("os_version", System.getProperty("os.version"));
        stats.put("os_arch", System.getProperty("os.arch"));
        stats.put("user_name", System.getProperty("user.name"));
        stats.put("user_home", System.getProperty("user.home"));
        stats.put("user_dir", System.getProperty("user.dir"));

        // Java & JVM details
        stats.put("java_version", System.getProperty("java.version"));
        stats.put("java_vendor", System.getProperty("java.vendor"));
        stats.put("java_vm_name", System.getProperty("java.vm.name"));

        // Process details via Java ProcessHandle (Java 9+ OS feature)
        stats.put("current_os_pid", ProcessHandle.current().pid());

        // Hardware & Concurrency
        stats.put("available_cpu_cores", runtime.availableProcessors());

        // Memory Metrics (in Megabytes)
        long totalMemMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemMb = runtime.freeMemory() / (1024 * 1024);
        long maxMemMb = runtime.maxMemory() / (1024 * 1024);
        long usedMemMb = totalMemMb - freeMemMb;

        stats.put("jvm_total_memory_mb", totalMemMb);
        stats.put("jvm_used_memory_mb", usedMemMb);
        stats.put("jvm_free_memory_mb", freeMemMb);
        stats.put("jvm_max_memory_mb", maxMemMb);

        // Storage / Disk Drives
        StringBuilder diskInfo = new StringBuilder();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                long totalSpaceGb = root.getTotalSpace() / (1024 * 1024 * 1024);
                long freeSpaceGb = root.getFreeSpace() / (1024 * 1024 * 1024);
                diskInfo.append(String.format("[%s: %d GB free / %d GB total] ", root.getAbsolutePath(), freeSpaceGb, totalSpaceGb));
            }
        }
        stats.put("os_disk_roots", diskInfo.toString().trim());

        return stats;
    }

    public String formatDiagnosticsReport() {
        Map<String, Object> diag = getSystemDiagnostics();
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("               JAVA OPERATING SYSTEM & HARDWARE DIAGNOSTICS         \n");
        sb.append("====================================================================\n");
        sb.append(String.format(" Operating System      : %s (v%s, arch: %s)\n", diag.get("os_name"), diag.get("os_version"), diag.get("os_arch")));
        sb.append(String.format(" Current OS Process ID : %s\n", diag.get("current_os_pid")));
        sb.append(String.format(" Logged-in OS User     : %s (Home: %s)\n", diag.get("user_name"), diag.get("user_home")));
        sb.append(String.format(" Working Directory     : %s\n", diag.get("user_dir")));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format(" Java Runtime          : %s (%s)\n", diag.get("java_version"), diag.get("java_vm_name")));
        sb.append(String.format(" Available CPU Cores   : %s threads\n", diag.get("available_cpu_cores")));
        sb.append(String.format(" JVM Memory Allocation : %s MB used / %s MB total (Max: %s MB)\n",
                diag.get("jvm_used_memory_mb"), diag.get("jvm_total_memory_mb"), diag.get("jvm_max_memory_mb")));
        sb.append(String.format(" Local Storage Disks   : %s\n", diag.get("os_disk_roots")));
        sb.append("====================================================================\n");
        return sb.toString();
    }
}
