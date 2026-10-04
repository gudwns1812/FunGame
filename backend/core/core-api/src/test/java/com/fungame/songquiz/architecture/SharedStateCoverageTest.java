package com.fungame.songquiz.architecture;

import com.fungame.songquiz.support.SharedStateCleanerCoverage;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합 테스트는 애플리케이션을 한 번만 띄우므로, 빈이 들고 있는 상태는 테스트 사이를 넘어간다.
 * 상태를 든 빈이 새로 생겼는데 정리 대상에 넣지 않으면 그 순간이 아니라 한참 뒤 엉뚱한
 * 테스트가 깨진다. 그걸 여기서 막는다.
 */
class SharedStateCoverageTest {

    private static final Set<Class<?>> MUTABLE_HOLDERS = Set.of(
            Map.class, Collection.class,
            AtomicBoolean.class, AtomicInteger.class, AtomicLong.class);

    private static final Set<String> BEAN_ANNOTATIONS = Set.of(
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository");

    @Test
    @DisplayName("상태를 든 빈은 모두 테스트 사이에 정리된다.")
    void everyStatefulBeanIsCleanedBetweenTests() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.fungame.songquiz");

        List<String> uncovered = classes.stream()
                .filter(SharedStateCoverageTest::isBean)
                .filter(SharedStateCoverageTest::holdsMutableState)
                .map(JavaClass::getName)
                .filter(name -> !SharedStateCleanerCoverage.isClassified(name))
                .sorted()
                .toList();

        assertThat(uncovered)
                .describedAs("""
                        상태를 든 빈인데 분류되지 않았다. SharedStateCleaner 에서 둘 중 하나로 정한다.
                        테스트 사이에 비워야 하면 STATEFUL_BEANS,
                        기동 때 채우고 안 바뀌는 레지스트리면 IMMUTABLE_REGISTRIES.""")
                .isEmpty();
    }

    private static boolean isBean(JavaClass type) {
        return type.getAnnotations().stream()
                .anyMatch(annotation -> BEAN_ANNOTATIONS.contains(annotation.getRawType().getName()));
    }

    private static boolean holdsMutableState(JavaClass type) {
        return type.getFields().stream()
                .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                .anyMatch(SharedStateCoverageTest::isMutableHolder);
    }

    private static boolean isMutableHolder(JavaField field) {
        return MUTABLE_HOLDERS.stream()
                .anyMatch(holder -> field.getRawType().isAssignableTo(holder));
    }
}
