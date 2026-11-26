package de.kirchrath.luna.note;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(packages = "de.kirchrath.luna.note", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DATA = "de.kirchrath.luna.note.data..";
    private static final String DTO = "de.kirchrath.luna.note.dto..";
    private static final String REPOSITORY = "de.kirchrath.luna.note.data.repository..";
    private static final String INBOUND = "de.kirchrath.luna.note.inbound..";
    private static final String DOMAIN_SERVICE = "DomainService";
    private static final String APPLICATION_SERVICE = "ApplicationService";
    private static final String SCHEDULER = "Scheduler";

    private static final DescribedPredicate<JavaClass> DTO_LIKE = describe(
            "a DTO, a java.* type, a primitive, Pageable or Page",
            javaClass -> javaClass.isPrimitive()
                    || javaClass.getPackageName().startsWith("java.")
                    || resideInAPackage(DTO).test(javaClass)
                    || javaClass.isEquivalentTo(Pageable.class)
                    || javaClass.isEquivalentTo(Page.class));

    private static final DescribedPredicate<JavaClass> MANDATORY_TRANSACTIONAL = describe(
            "annotated with @Transactional(propagation = MANDATORY)",
            javaClass -> javaClass.isAnnotatedWith(Transactional.class)
                    && javaClass.getAnnotationOfType(Transactional.class).propagation() == Propagation.MANDATORY);

    @ArchTest
    void thatCheckingDataShouldResultIntoNoDependencyOnDto(JavaClasses classes) {
        // given
        var rule = noClasses().that().resideInAPackage(DATA).should().dependOnClassesThat().resideInAPackage(DTO);

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingDtoShouldResultIntoNoDependencyOnData(JavaClasses classes) {
        // given
        var rule = noClasses().that().resideInAPackage(DTO).should().dependOnClassesThat().resideInAPackage(DATA);

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingDomainServicesShouldResultIntoAccessOnlyByApplicationServices(JavaClasses classes) {
        // given
        var rule = classes().that().haveSimpleNameEndingWith(DOMAIN_SERVICE)
                .should(onlyBeAccessedByItselfOr(simpleNameEndingWith(APPLICATION_SERVICE)));

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingDomainServicesShouldResultIntoTransactionalClasses(JavaClasses classes) {
        // given
        var rule = classes().that().haveSimpleNameEndingWith(DOMAIN_SERVICE)
                .should().beAnnotatedWith(Transactional.class);

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingApplicationServicesShouldResultIntoAccessOnlyByInboundOrSchedulers(JavaClasses classes) {
        // given
        var rule = classes().that().haveSimpleNameEndingWith(APPLICATION_SERVICE)
                .should(onlyBeAccessedByItselfOr(resideInAPackage(INBOUND).or(simpleNameEndingWith(SCHEDULER))));

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingApplicationServiceMethodsShouldResultIntoExactlyOneDomainServiceCallPerUseCase(
            JavaClasses classes) {
        // given
        var rule = methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith(APPLICATION_SERVICE)
                .should(callTheDomainServiceOncePerPublicMethodOnly());

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingApplicationServiceFieldsShouldResultIntoDomainServicesOnly(JavaClasses classes) {
        // given
        var rule = fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith(APPLICATION_SERVICE)
                .should().haveRawType(simpleNameEndingWith(DOMAIN_SERVICE));

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingApplicationServiceMethodSignaturesShouldResultIntoDtoLikeTypesOnly(JavaClasses classes) {
        // given
        var rule = methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith(APPLICATION_SERVICE)
                .should(onlyUseInSignature(DTO_LIKE));

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingRepositoryAccessShouldResultIntoAccessOnlyWithinTransactions(JavaClasses classes) {
        // given
        var rule = noClasses().that().resideOutsideOfPackage(REPOSITORY)
                .and().haveSimpleNameNotEndingWith(DOMAIN_SERVICE)
                .and(DescribedPredicate.not(MANDATORY_TRANSACTIONAL))
                .should().dependOnClassesThat().resideInAPackage(REPOSITORY);

        // when / then
        assertNoViolation(rule, classes);
    }

    @ArchTest
    void thatCheckingRepositoriesShouldResultIntoNoCustomQueries(JavaClasses classes) {
        // given
        var rule = noMethods().should().beAnnotatedWith(Query.class);

        // when / then
        assertNoViolation(rule, classes);
    }

    private static ArchCondition<JavaClass> onlyBeAccessedByItselfOr(DescribedPredicate<JavaClass> allowedCallers) {
        return new ArchCondition<>("only be accessed by itself or classes " + allowedCallers.getDescription()) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAccessesToSelf().stream()
                        .filter(access -> !access.getOriginOwner().equals(javaClass))
                        .forEach(access -> events.add(new SimpleConditionEvent(
                                access, allowedCallers.test(access.getOriginOwner()), access.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaMethod> callTheDomainServiceOncePerPublicMethodOnly() {
        return new ArchCondition<>("call exactly one domain service method if public and none otherwise") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                var domainServiceCalls = method.getMethodCallsFromSelf().stream()
                        .filter(call -> call.getTargetOwner().getSimpleName().endsWith(DOMAIN_SERVICE))
                        .count();
                var expected = method.getModifiers().contains(JavaModifier.PUBLIC) ? 1 : 0;
                events.add(new SimpleConditionEvent(method, domainServiceCalls == expected,
                        method.getFullName() + " calls " + domainServiceCalls + " domain service method(s), expected "
                                + expected));
            }
        };
    }

    private static ArchCondition<JavaMethod> onlyUseInSignature(DescribedPredicate<JavaClass> allowedTypes) {
        return new ArchCondition<>("only use types in its signature that are " + allowedTypes.getDescription()) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Stream.concat(method.getParameterTypes().stream(), Stream.of(method.getReturnType()))
                        .flatMap(type -> type.getAllInvolvedRawTypes().stream())
                        .forEach(type -> events.add(new SimpleConditionEvent(method, allowedTypes.test(type),
                                method.getFullName() + " uses " + type.getName() + " in its signature")));
            }
        };
    }

    private static void assertNoViolation(ArchRule rule, JavaClasses classes) {
        var result = rule.evaluate(classes);
        assertThat(result.hasViolation()).as(result.getFailureReport().toString()).isFalse();
    }
}
