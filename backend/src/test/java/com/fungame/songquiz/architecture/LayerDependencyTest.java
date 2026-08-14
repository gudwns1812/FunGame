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
    private static final String STORAGE = "storage";
    private static final String ENUMS = "enums";

    private static final String CONTROLLER_PACKAGE = "..controller..";
    private static final String DOMAIN_PACKAGE = "..domain..";
    private static final String STORAGE_PACKAGE = "..storage..";
    private static final String ENUMS_PACKAGE = "..enums..";

    @ArchTest
    static final ArchRule 의존은_controller에서_domain_storage_enums_한_방향으로만_흐른다 =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer(CONTROLLER).definedBy(CONTROLLER_PACKAGE)
                    .layer(DOMAIN).definedBy(DOMAIN_PACKAGE)
                    .layer(STORAGE).definedBy(STORAGE_PACKAGE)
                    .layer(ENUMS).definedBy(ENUMS_PACKAGE)
                    .whereLayer(CONTROLLER).mayNotBeAccessedByAnyLayer()
                    .whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(CONTROLLER)
                    .whereLayer(STORAGE).mayOnlyBeAccessedByLayers(DOMAIN)
                    .whereLayer(ENUMS).mayNotAccessAnyLayer()
                    .as("의존은 controller에서 domain, storage, enums 한 방향으로만 흐른다");
}
