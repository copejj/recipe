package com.braindribbler.recipe;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class ArchitectureStandardsTest {

    // Import all compiled production classes while stripping out external library
    // classes
    private final JavaClasses projectClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.braindribbler.recipe");

    @Test
    public void entitiesMustNotHardcodeSchemaNames() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Table.class)
                .should(new ArchCondition<JavaClass>("not have hardcoded schema prefixes") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        Table tableAnnotation = item.getAnnotationOfType(Table.class);
                        String schema = tableAnnotation.schema();
                        if (!schema.isEmpty()) {
                            events.add(SimpleConditionEvent.violated(item,
                                    String.format("Entity %s has a hardcoded schema prefix: '%s'. " +
                                            "Remove the schema attribute to remain pipeline-safe.",
                                            item.getName(), schema)));
                        }
                    }
                });

        rule.check(projectClasses);
    }

    @Test
    public void repositoriesMustMatchEntityIdTypes() {
        ArchRule rule = classes()
                // FIXED: Changed areAssignTo to areAssignableTo
                .that().areAssignableTo(JpaRepository.class)
                .and().areInterfaces()
                .should(new ArchCondition<JavaClass>("match their entity ID generic types precisely") {

                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        Class<?> repoInterface = item.reflect();

                        // Track down the JpaRepository interface generic parameter type block
                        Type jpaRepoType = Arrays.stream(repoInterface.getGenericInterfaces())
                                .filter(t -> t.getTypeName()
                                        .startsWith("org.springframework.data.jpa.repository.JpaRepository"))
                                .findFirst().orElse(null);

                        if (jpaRepoType instanceof ParameterizedType) {
                            ParameterizedType paramType = (ParameterizedType) jpaRepoType;
                            Type[] typeArgs = paramType.getActualTypeArguments();

                            Class<?> entityClass = (Class<?>) typeArgs[0];
                            Class<?> idClass = (Class<?>) typeArgs[1];

                            // Find the @Id or @EmbeddedId field inside the target Entity class
                            // representation
                            Class<?> actualIdType = Arrays.stream(entityClass.getDeclaredFields())
                                    .filter(f -> f.isAnnotationPresent(jakarta.persistence.Id.class)
                                            || f.isAnnotationPresent(jakarta.persistence.EmbeddedId.class))
                                    .map(f -> f.getType())
                                    .findFirst().orElse(null);

                            if (actualIdType != null && !actualIdType.equals(idClass)) {
                                events.add(SimpleConditionEvent.violated(item,
                                        String.format("❌ ID MISMATCH: Repository '%s' expects an ID of type [%s], " +
                                                "but Entity class '%s' declares its @Id field as [%s]!",
                                                repoInterface.getSimpleName(), idClass.getSimpleName(),
                                                entityClass.getSimpleName(), actualIdType.getSimpleName())));
                            }
                        }
                    }
                });

        rule.check(projectClasses);
    }
}
