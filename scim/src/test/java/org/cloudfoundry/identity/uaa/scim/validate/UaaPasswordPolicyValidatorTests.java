package org.cloudfoundry.identity.uaa.scim.validate;

import org.apache.commons.lang.RandomStringUtils;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.config.PasswordPolicy;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.cloudfoundry.identity.uaa.scim.exception.InvalidPasswordException;
import org.cloudfoundry.identity.uaa.zone.IdentityProviderProvisioning;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ****************************************************************************
 * Cloud Foundry
 * Copyright (c) [2009-2015] Pivotal Software, Inc. All Rights Reserved.
 * <p/>
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 * <p/>
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 * *****************************************************************************
 */
@RunWith(MockitoJUnitRunner.class)
public class UaaPasswordPolicyValidatorTests {

    @Mock
    private IdentityProviderProvisioning provisioning;

    private UaaPasswordPolicyValidator validator;

    @Before
    public void setUp() {
        validator = new UaaPasswordPolicyValidator();
        validator.setPasswordPolicyResolver(new PasswordPolicyResolver() {
            @Override
            public PasswordPolicy resolve() {
                return new PasswordPolicy(10, 23, 1, 1, 1, 1, 6);
            }
        });
    }

    @Test
    public void testValidateSuccess() {
        validatePassword("Password2 ");
        validatePassword("Password2&");
    }

    @Test
    public void specialCharacterNotInListFailsValidation() {
        validatePassword("Passsss1\u007F", "Password must contain at least 1 special characters.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateWithNullPassword() {
        validatePassword(null);
    }

    @Test
    public void testValidateShortPassword() {
        validatePassword("Pas1", "Password must be at least 10 characters in length.");
    }

    @Test
    public void testValidateLongPassword() {
        validatePassword(RandomStringUtils.randomAlphanumeric(23) + "aA9", "Password must be no more than 23 characters in length.");
    }

    @Test
    public void testValidateAllLowerCase() {
        validatePassword("password2", "Password must contain at least 1 uppercase characters.");
    }

    @Test
    public void testValidateAllUpperCase() {
        validatePassword("PASSWORD2", "Password must contain at least 1 lowercase characters.");
    }

    @Test
    public void testValidateNoDigits() {
        validatePassword("Password", "Password must contain at least 1 digit characters.");
    }

    @Test
    public void testValidateWithNoSpecialCharacter() {
        validatePassword("Password123", "Password must contain at least 1 special characters.");
    }

    @Test
    public void testValidationDisabledWhenZoneIsNotDefault() {
        validator.setPasswordPolicyResolver(new PasswordPolicyResolver() {
            @Override
            public PasswordPolicy resolve() {
                return null;
            }
        });
        validatePassword("a");
    }

    private void validatePassword(String password, String ... expectedErrors) {
        ScimUser user = new ScimUser();
        user.setOrigin(Origin.UAA);
        try {
            validator.validate(password);
            if (expectedErrors != null && expectedErrors.length > 0) {
                fail();
            }
        } catch (InvalidPasswordException e) {
            if (expectedErrors.length == 0) {
                fail("Didn't expect InvalidPasswordException, but messages were " + e.getErrorMessages());
            }
            for (int i = 0; i < expectedErrors.length; i++) {
                assertTrue("Errors should contain:"+expectedErrors[i], e.getErrorMessages().contains(expectedErrors[i]));
            }
        }
    }
}
