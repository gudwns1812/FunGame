package com.fungame.songquiz.architecture;

import com.fungame.songquiz.support.config.ClusterWide;
import com.fungame.songquiz.support.config.InstanceLocal;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = LayerDependencyTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ScheduledScopeTest {

    @ArchTest
    static final ArchRule 주기_작업은_로컬인지_전역인지_밝힌다 =
            methods().that().areAnnotatedWith(Scheduled.class)
                    .should().beAnnotatedWith(InstanceLocal.class)
                    .orShould().beAnnotatedWith(ClusterWide.class)
                    .because("""
                            인스턴스가 늘면 @Scheduled 는 모든 인스턴스에서 각각 돈다.
                            자기 메모리만 건드리는 작업(@InstanceLocal)은 그대로 둬도 되지만,
                            공유 자원이나 외부를 건드리는 작업(@ClusterWide)은 한 번만 돌아야 한다.
                            어느 쪽인지 밝히지 않으면 2대로 늘릴 때 무엇을 고쳐야 할지 알 수 없다.""");
}
