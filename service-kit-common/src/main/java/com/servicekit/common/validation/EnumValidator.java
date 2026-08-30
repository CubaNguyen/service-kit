package com.servicekit.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, CharSequence> {
    private Set<String> acceptedValues;
    private boolean ignoreCase;

    @Override
    public void initialize(ValidEnum annotation) {
        this.ignoreCase = annotation.ignoreCase();
        this.acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .map(val -> ignoreCase ? val.toUpperCase() : val)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Để @NotNull xử lý nếu trường này bắt buộc
        }
        String checkValue = ignoreCase ? value.toString().toUpperCase() : value.toString();
        return acceptedValues.contains(checkValue);
    }
}