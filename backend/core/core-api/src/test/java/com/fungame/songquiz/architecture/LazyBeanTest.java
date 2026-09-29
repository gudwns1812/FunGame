package com.fungame.songquiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Lazy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

@AnalyzeClasses(packages = LayerDependencyTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LazyBeanTest {

    private static final String WHY = """
            기동 시점에 없는 빈은 없는 빈이다. 메트릭 바인딩·워밍업·헬스 인디케이터 등록처럼
            기동 때 한 번 훑고 지나가는 것들이 그 빈을 에러 없이 건너뛴다.
            프레임워크의 @Lazy 빈이 필요하면 주입으로 깨우지 말고 그 빈을 우리가 갖는다.
            가져올 때는 프레임워크가 달던 이름과 별칭을 그대로 붙인다.""";

    @ArchTest
    static final ArchRule 클래스에_Lazy_를_붙이지_않는다 =
            noClasses().should().beAnnotatedWith(Lazy.class).because(WHY);

    @ArchTest
    static final ArchRule 빈_메서드와_필드에_Lazy_를_붙이지_않는다 =
            noMembers().should().beAnnotatedWith(Lazy.class).because(WHY);
}
