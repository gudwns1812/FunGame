package com.fungame.songquiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = LayerDependencyTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class BeanNamingTest {

    @ArchTest
    static final ArchRule 주입되는_빈은_복수형_이름을_쓰지_않는다 =
            noClasses()
                    .that().areMetaAnnotatedWith(Component.class)
                    .and().haveSimpleNameNotEndingWith("Metrics")
                    .should().haveSimpleNameEndingWith("s")
                    .as("복수형은 값 묶음을 든 일급 컬렉션의 이름이다. 주입되는 빈은 *Registry · *Cache · *Tracker · *Factory 처럼 역할로 부른다");
}
