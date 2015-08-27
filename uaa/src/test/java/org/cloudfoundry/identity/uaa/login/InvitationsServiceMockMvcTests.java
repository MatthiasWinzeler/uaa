/*
 * *****************************************************************************
 *      Cloud Foundry
 *      Copyright (c) [2009-2015] Pivotal Software, Inc. All Rights Reserved.
 *      This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *      You may not use this product except in compliance with the License.
 *
 *      This product includes a number of subcomponents with
 *      separate copyright notices and license terms. Your use of these
 *      subcomponents is subject to the terms and conditions of the
 *      subcomponent's license, as noted in the LICENSE file.
 * *****************************************************************************
 */

package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.login.util.FakeJavaMailSender;
import org.cloudfoundry.identity.uaa.mock.InjectedMockContextTest;
import org.cloudfoundry.identity.uaa.mock.util.MockMvcUtils;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InvitationsServiceMockMvcTests extends InjectedMockContextTest {

    @Before
    @After
    public void clearOutCodeTable() {
        getWebApplicationContext().getBean(JdbcTemplate.class).update("DELETE FROM expiring_code_store");
    }

    @Test
    public void ensure_that_newly_created_user_has_origin_UNKNOWN() throws Exception {
        String username = new RandomValueStringGenerator().generate()+"@test.org";
        AccountCreationService svc = getWebApplicationContext().getBean(AccountCreationService.class);
        ScimUser user = svc.createUser(username, "password");
        assertEquals(Origin.UNKNOWN, user.getOrigin());
    }

    public FakeJavaMailSender.MimeMessageWrapper inviteUser(String email) throws Exception {
        SecurityContext marissa = MockMvcUtils.utils().getMarissaSecurityContext(getWebApplicationContext());
        getMockMvc().perform(post("/invitations/new.do")
            .accept(MediaType.TEXT_HTML)
            .param("email", email)
            .with(securityContext(marissa))
            .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("sent"));

        assertEquals(Origin.UNKNOWN, getWebApplicationContext().getBean(JdbcTemplate.class).queryForObject("SELECT origin FROM users WHERE username='"+email+"'", String.class));

        FakeJavaMailSender sender = getWebApplicationContext().getBean(FakeJavaMailSender.class);
        assertEquals(1, sender.getSentMessages().size());
        FakeJavaMailSender.MimeMessageWrapper message = sender.getSentMessages().get(0);
        return message;

    }

    @Test
    public void inviteUser_Correct_Origin_Sent() throws Exception {
        String email = new RandomValueStringGenerator().generate()+"@test.org";
        inviteUser(email);
    }

    @Test
    public void invite_user_show_correct_saml_idp_for_acceptance() throws Exception {

    }

    @Test
    public void invite_user_show_correct_saml_and_uaa_idp_for_acceptance() throws Exception {}

    @Test
    public void accept_invite_for_uaa_changes_correct_origin() throws Exception {}

    @Test
    public void accept_invite_for_saml_changes_correct_origin() throws Exception {}

    @Test
    public void accept_invite_for_ldap_changes_correct_origin() throws Exception {}

    @Test
    public void accept_invite_for_existing_user_deletes_invite() throws Exception {}
}
