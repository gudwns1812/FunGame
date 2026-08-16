package com.fungame.songquiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = LayerDependencyTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    static final String ROOT_PACKAGE = "com.fungame.songquiz";

    private static final String CONTROLLER = "controller";
    private static final String DOMAIN = "domain";
    private static final String SUPPORT = "support";

    private static final String CONTROLLER_PACKAGE = "..controller..";
    private static final String DOMAIN_PACKAGE = "..domain..";
    private static final String SUPPORT_PACKAGE = "..support..";

    @ArchTest
    static final ArchRule domain은_controller를_모르고_support는_공유_커널이다 =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer(CONTROLLER).definedBy(CONTROLLER_PACKAGE)
                    .layer(DOMAIN).definedBy(DOMAIN_PACKAGE)
                    .layer(SUPPORT).definedBy(SUPPORT_PACKAGE)
                    .whereLayer(CONTROLLER).mayNotBeAccessedByAnyLayer()
                    .whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(CONTROLLER)
                    .whereLayer(SUPPORT).mayOnlyBeAccessedByLayers(CONTROLLER, DOMAIN)
                    .as("domain 은 controller 를 모르고, support 는 모든 레이어가 쓰는 공유 커널이다");
}
