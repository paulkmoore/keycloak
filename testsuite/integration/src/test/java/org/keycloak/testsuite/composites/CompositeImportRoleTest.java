/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.composites;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.ApplicationServlet;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.security.PublicKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CompositeImportRoleTest {

    public static PublicKey realmPublicKey;
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule(){
        @Override
        protected void configure(RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/testcomposite.json"));
            RealmModel realm = manager.getRealmByName("Test");
            realmPublicKey = realm.getPublicKey();



            deployServlet("app", "/app", ApplicationServlet.class);

        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void testAppCompositeUser() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("APP_COMPOSITE_APPLICATION");
        oauth.doLogin("APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("APP_COMPOSITE_USER", token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }


    @Test
    public void testRealmAppCompositeUser() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("APP_ROLE_APPLICATION");
        oauth.doLogin("REALM_APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_APP_COMPOSITE_USER", token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
    }



    @Test
    public void testRealmOnlyWithUserCompositeAppComposite() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_COMPOSITE_1_USER", token.getSubject());

        Assert.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppRole() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_ROLE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_COMPOSITE_1_USER", token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserRoleAppComposite() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_ROLE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_ROLE_1_USER", token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }




}
