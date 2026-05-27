package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PackageVersionPortTest {

    private final PackageVersionPort port = new LenientPackageVersionAdapter();

    @Test
    void existsTrueWhenVersionNonBlank() {
        assertThat(port.exists("tenant-A", "knowledge", "v1.0")).isTrue();
    }

    @Test
    void existsFalseWhenVersionNullOrBlank() {
        assertThat(port.exists("tenant-A", "knowledge", null)).isFalse();
        assertThat(port.exists("tenant-A", "knowledge", "  ")).isFalse();
    }

    @Test
    void getActiveReturnsEmptyForLenientImpl() {
        assertThat(port.getActive("tenant-A", "knowledge"))
            .as("Lenient 实现没有真实包注册，返回 empty 让上游 fail-fast")
            .isEmpty();
    }

    @Test
    void getActiveEmptyWhenMissing() {
        Optional<String> v = port.getActive("tenant-X", "rule");
        assertThat(v).isEmpty();
    }
}
