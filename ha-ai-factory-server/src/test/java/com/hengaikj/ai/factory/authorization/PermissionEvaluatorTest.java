package com.hengaikj.ai.factory.authorization;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionEvaluatorTest {
    private final InMemoryRoleAssignments assignments = new InMemoryRoleAssignments();
    private final PermissionEvaluator evaluator = new PermissionEvaluator(assignments);

    @Test
    void allowsPermissionGrantedThroughAssignedRole() {
        assignments.putRole(new Role("reviewer", Set.of("deliverable.review")));
        assignments.assignRole("user-1", "reviewer");

        assertThat(evaluator.hasPermission("user-1", "deliverable.review")).isTrue();
    }

    @Test
    void deniesPermissionAbsentFromAssignedRole() {
        assignments.putRole(new Role("reviewer", Set.of("deliverable.review")));
        assignments.assignRole("user-1", "reviewer");

        assertThat(evaluator.hasPermission("user-1", "project.delete")).isFalse();
    }

    @Test
    void deniesUnknownSubject() {
        assignments.putRole(new Role("reviewer", Set.of("deliverable.review")));

        assertThat(evaluator.hasPermission("unknown", "deliverable.review")).isFalse();
    }

    @Test
    void deniesUnknownRole() {
        assertThatThrownBy(() -> assignments.assignRole("user-1", "missing-role"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(evaluator.hasPermission("user-1", "deliverable.review")).isFalse();
    }
}
