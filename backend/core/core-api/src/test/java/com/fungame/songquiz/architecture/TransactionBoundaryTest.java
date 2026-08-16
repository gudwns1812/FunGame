package com.fungame.songquiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = TransactionBoundaryTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class TransactionBoundaryTest {

    static final String ROOT_PACKAGE = "com.fungame.songquiz";

    private static final String IMPLEMENT_SUFFIX_WRITER = "Writer";
    private static final String IMPLEMENT_SUFFIX_READER = "Reader";
    private static final String SERVICE_SUFFIX = "Service";
    private static final String DOMAIN_PACKAGE = "..domain..";
    private static final String ATOMIC_SERVICE_METHODS = "requestReset|resetPassword|approveRequest";

    @ArchTest
    static final ArchRule 쓰기_구현체는_스스로_트랜잭션_경계를_연다 =
            methods()
                    .that().arePublic()
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith(IMPLEMENT_SUFFIX_WRITER)
                    .and().areDeclaredInClassesThat().resideInAPackage(DOMAIN_PACKAGE)
                    .should().beAnnotatedWith(Transactional.class)
                    .as("Writer 의 public 메서드는 트랜잭션 없이 호출되면 변경이 조용히 사라지므로 스스로 경계를 연다");

    @ArchTest
    static final ArchRule 읽기_구현체는_스스로_트랜잭션_경계를_연다 =
            methods()
                    .that().arePublic()
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith(IMPLEMENT_SUFFIX_READER)
                    .and().areDeclaredInClassesThat().resideInAPackage(DOMAIN_PACKAGE)
                    .should().beAnnotatedWith(Transactional.class)
                    .as("Reader 의 public 메서드도 자신이 필요한 트랜잭션을 직접 선언한다");

    @ArchTest
    static final ArchRule 서비스는_원자성이_필요할_때만_트랜잭션을_묶는다 =
            noMethods()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith(SERVICE_SUFFIX)
                    .and().areDeclaredInClassesThat().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveNameNotMatching(ATOMIC_SERVICE_METHODS)
                    .should().beAnnotatedWith(Transactional.class)
                    .as("여러 쓰기를 한 단위로 묶어야 하는 세 메서드 외에는 서비스가 트랜잭션을 열지 않는다");
}
