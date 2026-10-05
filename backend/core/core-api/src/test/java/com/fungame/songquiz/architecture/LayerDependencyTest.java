package com.fungame.songquiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = LayerDependencyTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    static final String ROOT_PACKAGE = "com.fungame.songquiz";

    private static final String API = "api";
    private static final String DOMAIN = "domain";
    private static final String SUPPORT = "support";

    private static final String API_PACKAGE = "..api..";
    private static final String DOMAIN_PACKAGE = "..domain..";
    private static final String SUPPORT_PACKAGE = "..support..";

    @ArchTest
    static final ArchRule domain은_api를_모르고_support는_공유_커널이다 =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer(API).definedBy(API_PACKAGE)
                    .layer(DOMAIN).definedBy(DOMAIN_PACKAGE)
                    .layer(SUPPORT).definedBy(SUPPORT_PACKAGE)
                    .whereLayer(API).mayNotBeAccessedByAnyLayer()
                    .whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(API)
                    .whereLayer(SUPPORT).mayOnlyBeAccessedByLayers(API, DOMAIN)
                    .whereLayer(SUPPORT).mayNotAccessAnyLayer()
                    .as("domain 은 api 를 모르고, support 는 모든 레이어가 쓰는 공유 커널이다");

    @ArchTest
    static final ArchRule STOMP_로_내보내는_것은_Notifier_뿐이다 =
            noClasses()
                    .that().resideInAPackage(API_PACKAGE)
                    .and().haveSimpleNameNotEndingWith("Notifier")
                    .and().haveSimpleNameNotEndingWith("StreamListener")
                    .and().haveSimpleNameNotEndingWith("Spreader")
                    .should().dependOnClassesThat().haveSimpleName("StompBroadcaster")
                    .as("STOMP 로 내보내는 입구는 *Notifier 하나다. 컨트롤러에서 바로 쏘지 않는다");

    @ArchTest
    static final ArchRule api에는_서비스_계층을_두지_않는다 =
            noClasses()
                    .that().resideInAPackage(API_PACKAGE)
                    .should().beAnnotatedWith(Service.class)
                    .as("api 는 presentation 이다. @Service 는 domain 에만 둔다");
}
