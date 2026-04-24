package com.nexus.common.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回归守卫：后端业务代码禁止直接调用 BigDecimal.divide(...)，
 * 统一使用 BigDecimalSafeUtils 以避免除零与无限小数异常风险。
 */
class BigDecimalDivideUsageGuardTest {

    private static final Pattern DIVIDE_PATTERN = Pattern.compile("\\.divide\\s*\\(");
    private static final List<String> BACKEND_MODULES = List.of(
            "nexus-auth", "nexus-common", "nexus-erp", "nexus-gateway",
            "nexus-oa", "nexus-system", "nexus-wage"
    );

    @Test
    void shouldNotUseRawBigDecimalDivideInBackendModules() throws IOException {
        Path repoRoot = locateRepoRoot();
        List<String> violations = new ArrayList<>();

        for (String module : BACKEND_MODULES) {
            Path javaMainDir = repoRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(javaMainDir)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(javaMainDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("BigDecimalSafeUtils.java"))
                        .forEach(path -> scanFile(path, repoRoot, violations));
            }
        }

        assertThat(violations)
                .withFailMessage("检测到禁止的 BigDecimal.divide 直接调用，请改用 BigDecimalSafeUtils:\n%s",
                        String.join("\n", violations))
                .isEmpty();
    }

    private static void scanFile(Path file, Path repoRoot, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (DIVIDE_PATTERN.matcher(line).find()) {
                    violations.add(repoRoot.relativize(file) + ":" + (i + 1) + " -> " + line.trim());
                }
            }
        } catch (IOException e) {
            violations.add(repoRoot.relativize(file) + ":读取失败(" + e.getMessage() + ")");
        }
    }

    private static Path locateRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            boolean looksLikeRepoRoot = Files.exists(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("nexus-common"))
                    && Files.isDirectory(current.resolve("nexus-system"));
            if (looksLikeRepoRoot) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录，BigDecimal 除法守卫无法执行");
    }
}
