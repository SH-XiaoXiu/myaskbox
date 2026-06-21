plugins {
    java
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
}

description = "AskBox backend application."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

springBoot {
    mainClass.set("cn.xiuxius.askbox.AskBoxApplication")
}

base {
    archivesName.set("askbox-backend")
}

val mybatisPlusVersion = "3.5.12"
val knife4jVersion = "4.5.0"
val saTokenVersion = "1.41.0"
val palantirJavaFormatVersion = "2.50.0"
val bucket4jVersion = "8.19.0"
val hutoolVersion = "5.8.46"
val minioVersion = "9.0.3"
val springAiVersion = "1.0.9"

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // MyBatis-Plus
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:$mybatisPlusVersion")
    implementation("com.baomidou:mybatis-plus-jsqlparser:$mybatisPlusVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis & Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Sa-Token
    implementation("cn.dev33:sa-token-spring-boot3-starter:$saTokenVersion")
    implementation("cn.dev33:sa-token-redis-template:$saTokenVersion")

    // API Docs
    implementation("com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:$knife4jVersion")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j_jdk17-core:$bucket4jVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // BCrypt
    implementation("org.springframework.security:spring-security-crypto")

    // Utilities
    implementation("cn.hutool:hutool-all:$hutoolVersion")
    implementation("io.minio:minio:$minioVersion")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-Xshare:off")
}

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        palantirJavaFormat(palantirJavaFormatVersion)
        removeUnusedImports()
        importOrder("java", "javax", "jakarta", "org", "com", "cn", "")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("resources") {
        target(
            "src/main/resources/**/*.yaml",
            "src/main/resources/**/*.yml",
            "src/main/resources/**/*.properties"
        )
        targetExclude("**/db/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("compileJava") {
    dependsOn("spotlessApply")
}

tasks.named("compileTestJava") {
    dependsOn("spotlessApply")
}
