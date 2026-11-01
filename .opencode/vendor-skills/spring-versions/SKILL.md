---
name: spring-versions
description: Provide current Spring ecosystem version numbers, release schedule, version alignment between projects, and support timelines. Use when generating pom.xml or build.gradle dependencies, choosing which Spring Boot version to target, checking compatibility between Spring projects, or answering which version is latest.
version: 0.1.0
tags: [versions, releases, compatibility, dependencies, bom]
triggers:
  - generating pom.xml or build.gradle with Spring dependencies
  - choosing which Spring Boot or Framework version to use
  - checking compatibility between Spring projects
  - asking which version of a Spring project is current or latest
  - checking when a Spring version reaches end of life
  - upgrading between Spring Boot major or minor versions
spring_versions:
  boot: ">=3.3"
depends_on: []
---

# Spring Versions

**Pattern:** Navigation

Current version numbers and release schedule for the Spring ecosystem. **Last updated: 2026-04-02.**

**Mental model:** Spring Boot drives the version train. Pick your Boot version first — Framework, Security, Data, and Cloud versions follow automatically via the Boot BOM. Never mix Boot and Framework versions manually.

**Do NOT Load** for how to use these frameworks — use the dedicated skill (spring-boot, spring-framework, etc.). This skill is purely for version numbers and compatibility.

## Version Alignment Matrix

Pick a Spring Boot version. Everything else follows:

| Spring Boot | Spring Framework | Spring Security | Spring Cloud | Spring Data BOM | Java Min |
|-------------|-----------------|-----------------|--------------|----------------|----------|
| **4.0.x** | **7.0.x** | **7.0.x** | **2025.1** (Oakwood) | **2025.1** | 17 |
| **3.5.x** | **6.2.x** | **6.5.x** | **2025.0** (Northfields) | **2025.0** | 17 |
| 3.4.x | 6.1.x | 6.4.x | 2024.0 (Moorgate) | 2024.0 | 17 |
| 3.3.x | 6.1.x | 6.3.x | 2023.0 (Leyton) | 2023.0 | 17 |

**Rule:** Never override Spring Framework or Security versions manually when using Boot's BOM. If you need a newer Framework version, upgrade Boot.

## Current Latest Versions

As of **2026-04-02**:

| Project | Current GA | Maintenance | Next |
|---------|-----------|-------------|------|
| Spring Boot | **4.0.5** | 3.5.13 | 4.1.0-M4 |
| Spring Framework | **7.0.6** | 6.2.17 | 7.1.x (Nov 2026) |
| Spring Security | **7.0.4** | 6.5.9 | 7.1.0-M3 |
| Spring Cloud | **2025.1.1** | 2025.0.2 | — |
| Spring Data BOM | **2025.1.4** | 2025.0.10 | 2026.0.0-M2 |
| Spring Modulith | **2.0.5** | 1.4.10 | 2.1.0-M4 |
| Spring AI | **1.1.4** | 1.0.5 | 2.0.0-M4 |
| Spring Authorization Server | **1.5.6** | 1.4.8 | 2.0.0-M2 |

**"Current GA"** = the version to use for new projects.
**"Maintenance"** = previous line, still receiving patches.
**"Next"** = upcoming milestone or release candidate.

## Release Cadence

Spring Boot releases new major or minor versions every **6 months** (May and November), typically the Thursday after the 3rd Monday of the month.

| Month | What happens |
|-------|-------------|
| **May** | New minor/major GA (e.g., Boot 4.0 was Nov 2024, next is 4.1 ~May 2026) |
| **November** | New minor/major GA |
| Monthly | Patch releases for all supported lines |

## Support Policy

| Support tier | Duration | What it covers |
|-------------|----------|----------------|
| **OSS support** | Minor: 12 months, Major: 3+ years | Critical bugs and security fixes |
| **Commercial** (VMware Tanzu) | Extended beyond OSS | Available at spring.io/support |

**Key dates:**
- Spring Framework 6.2.x OSS support ends **June 2026**
- Spring Framework 5.3.x OSS support **ended August 2024**
- Spring Boot 3.3.x — approaching EOL, upgrade to 3.5.x or 4.0.x

## Choosing a Version for New Projects

**Start with Spring Boot 4.0.x** unless:
- Your organization hasn't validated Jakarta EE 11 → use **3.5.x**
- You need a Spring Cloud component not yet on 2025.1 → use **3.5.x**
- You have dependencies requiring Java EE (`javax.*`) → stay on **3.3.x** and plan migration

**For Spring AI:** Use **1.1.x** on Boot 3.4+ or 3.5.x. Spring AI 2.0 targets Boot 4.0 but is still in milestones — use 1.1.x until 2.0 reaches GA.

## Spring AI Version Alignment

Spring AI has its own version line independent of Boot:

| Spring AI | Spring Boot | Status |
|-----------|-------------|--------|
| **1.1.x** | 3.4.x, 3.5.x | Current stable |
| 1.0.x | 3.4.x | Maintenance |
| 2.0.0-M4 | 4.0.x | Preview (not for production) |

## Spring Cloud Release Train Naming

Spring Cloud uses CalVer (`YYYY.MINOR.MICRO`) with London Underground station codenames:

| Train | Codename | Boot |
|-------|----------|------|
| 2025.1 | Oakwood | 4.0.x |
| 2025.0 | Northfields | 3.5.x |
| 2024.0 | Moorgate | 3.4.x |
| 2023.0 | Leyton | 3.3.x, 3.2.x |

## Dependency Declaration

Always use the Spring Boot BOM. Never declare individual Spring project versions:

```xml
<!-- Maven: parent POM manages all versions -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.5</version>
</parent>
```

```kotlin
// Gradle: Boot plugin manages all versions
plugins {
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}
```

For Spring AI (not managed by Boot BOM):
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Resource Files

Load on demand for specific topics:

- **`references/release-calendar.md`** — Planned release dates for upcoming versions, iCal feed URL, monthly schedule

## References

- [Spring Projects Overview](https://spring.io/projects)
- [Spring Release Calendar (iCal)](https://calendar.spring.io/ical)
- [Spring Boot Supported Versions](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions)
- [Spring Cloud Version Compatibility](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions)
