package com.ambdulghaffar.springstarterapi.dto.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    private static final PhoneNumberUtil util = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true; // champ facultatif
        try {
            return util.isValidNumber(util.parse(value, null));
        } catch (NumberParseException e) {
            return false;
        }
    }
}