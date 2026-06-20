package cn.xiuxius.askbox.system.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PersistenceConventionsTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void latestMigrationDropsForeignKeysAndTimestampDefaults() throws Exception {
        Path migration = MIGRATION_DIR.resolve("V17__drop_foreign_keys_and_timestamp_defaults.sql");

        assertThat(Files.exists(migration)).isTrue();
        String sql = Files.readString(migration);
        assertThat(sql).contains("con.contype = 'f'");
        assertThat(sql).contains("ALTER COLUMN %I DROP DEFAULT");
    }

    @Test
    void mapperJavaInterfacesDoNotUseAnnotationSql() throws Exception {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/cn/xiuxius/askbox"))) {
            String javaSources = stream.filter(path -> path.toString().contains("/mapper/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::readString)
                    .reduce("", String::concat);

            assertThat(javaSources).doesNotContain("@Select(");
            assertThat(javaSources).doesNotContain("@Insert(");
            assertThat(javaSources).doesNotContain("@Update(");
            assertThat(javaSources).doesNotContain("@Delete(");
        }
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
