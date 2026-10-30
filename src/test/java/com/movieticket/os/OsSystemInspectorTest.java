package com.movieticket.os;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OsSystemInspectorTest {

    private final OsSystemInspector inspector = new OsSystemInspector();

    @Test
    @DisplayName("Should retrieve valid OS and JVM diagnostic metrics")
    void testGetSystemDiagnostics() {
        Map<String, Object> stats = inspector.getSystemDiagnostics();

        assertNotNull(stats);
        assertNotNull(stats.get("os_name"));
        assertNotNull(stats.get("java_version"));
        assertTrue((int) stats.get("available_cpu_cores") >= 1);
        assertTrue((long) stats.get("jvm_total_memory_mb") > 0);
        assertTrue((long) stats.get("current_os_pid") > 0);
    }

    @Test
    @DisplayName("Should generate formatted OS diagnostics report")
    void testFormatDiagnosticsReport() {
        String report = inspector.formatDiagnosticsReport();

        assertNotNull(report);
        assertTrue(report.contains("JAVA OPERATING SYSTEM"));
        assertTrue(report.contains("Operating System"));
        assertTrue(report.contains("CPU Cores"));
    }
}
