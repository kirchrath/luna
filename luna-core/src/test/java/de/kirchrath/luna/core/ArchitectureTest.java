package de.kirchrath.luna.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "de.kirchrath.luna.core", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DATA = "de.kirchrath.luna.core.data..";
    private static final String DTO = "de.kirchrath.luna.core.dto..";

    @ArchTest
    void thatCheckingDataShouldResultIntoNoDependencyOnDto(JavaClasses classes) {
        // given
        var rule = noClasses().that().resideInAPackage(DATA).should().dependOnClassesThat().resideInAPackage(DTO);

        // when
        var result = rule.evaluate(classes);

        // then
        assertThat(result.hasViolation()).as(result.getFailureReport().toString()).isFalse();
    }

    @ArchTest
    void thatCheckingDtoShouldResultIntoNoDependencyOnData(JavaClasses classes) {
        // given
        var rule = noClasses().that().resideInAPackage(DTO).should().dependOnClassesThat().resideInAPackage(DATA);

        // when
        var result = rule.evaluate(classes);

        // then
        assertThat(result.hasViolation()).as(result.getFailureReport().toString()).isFalse();
    }
}
