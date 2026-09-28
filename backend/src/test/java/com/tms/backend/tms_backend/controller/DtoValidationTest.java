package com.tms.backend.tms_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.backend.tms_backend.dto.CommentCreateRequest;
import com.tms.backend.tms_backend.dto.TicketCreateRequest;
import com.tms.backend.tms_backend.dto.TicketResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DtoValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void acceptsExactTicketTitleBoundariesAndRejectsOutsideThem() {
        assertThat(validator.validate(new TicketCreateRequest("12345", "description", null))).isEmpty();
        assertThat(validator.validate(new TicketCreateRequest("1".repeat(150), "description", null))).isEmpty();
        assertThat(validator.validate(new TicketCreateRequest("1234", "description", null)))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("title"));
        assertThat(validator.validate(new TicketCreateRequest("1".repeat(151), "description", null)))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("title"));
    }

    @Test
    void rejectsBlankTicketDescriptionAndInvalidCommentContent() {
        assertThat(validator.validate(new TicketCreateRequest("Valid title", " ", null))).isNotEmpty();
        assertThat(validator.validate(new CommentCreateRequest(" ", null))).isNotEmpty();
        assertThat(validator.validate(new CommentCreateRequest("1".repeat(1001), null))).isNotEmpty();
        assertThat(validator.validate(new CommentCreateRequest("1", null))).isEmpty();
    }

    @Test
    void nullRequestValuesAreRejectedAndResponseHasNoPersistenceOrCommentFields() {
        assertThat(validator.validate(new TicketCreateRequest(null, null, null))).hasSizeGreaterThanOrEqualTo(2);

        Set<String> responseProperties = Set.of(java.util.Arrays.stream(TicketResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new));
        assertThat(responseProperties).containsExactlyInAnyOrder(
                "id", "title", "description", "status", "assigneeId", "createdAt", "updatedAt");
        assertThat(responseProperties).doesNotContain("version", "comments", "priority");
    }

    @Test
    void requestRecordsDeserializeWithJackson() throws Exception {
        TicketCreateRequest request = new ObjectMapper().readValue(
                "{\"title\":\"Valid title\",\"description\":\"Details\"}",
                TicketCreateRequest.class);

        assertThat(request.title()).isEqualTo("Valid title");
        assertThat(request.description()).isEqualTo("Details");
        assertThat(request.assigneeId()).isNull();
    }
}