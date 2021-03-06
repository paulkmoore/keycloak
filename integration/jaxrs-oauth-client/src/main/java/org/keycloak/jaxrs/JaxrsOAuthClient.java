package org.keycloak.jaxrs;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.util.BasicAuthHelper;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URL;

/**
 * Helper code to obtain oauth access tokens via browser redirects
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JaxrsOAuthClient extends AbstractOAuthClient {
    protected static final Logger logger = Logger.getLogger(JaxrsOAuthClient.class);
    protected Client client;

    /**
     * Creates a Client for obtaining access token from code
     */
    public void start() {
        if (client == null) {
            client = new ResteasyClientBuilder().trustStore(truststore)
                    .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                    .connectionPoolSize(10)
                    .build();
        }
    }

    /**
     * closes cllient
     */
    public void stop() {
        client.close();
    }
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String resolveBearerToken(String redirectUri, String code) {
        redirectUri = stripOauthParametersFromRedirect(redirectUri);
        String authHeader = BasicAuthHelper.createHeader(clientId, password);
        Form codeForm = new Form()
                .param("grant_type", "authorization_code")
                .param("code", code)
                .param("client_id", clientId)
                .param("password", password)
                .param("redirect_uri", redirectUri);
        Response res = client.target(codeUrl).request().header(HttpHeaders.AUTHORIZATION, authHeader).post(Entity.form(codeForm));
        try {
            if (res.getStatus() == 400) {
                throw new BadRequestException();
            } else if (res.getStatus() != 200) {
                throw new InternalServerErrorException(new Exception("Unknown error when getting acess token"));
            }
            AccessTokenResponse tokenResponse = res.readEntity(AccessTokenResponse.class);
            return tokenResponse.getToken();
        } finally {
            res.close();
        }
    }
    public Response redirect(UriInfo uriInfo, String redirectUri) {
        return redirect(uriInfo, redirectUri, null);
    }

    public Response redirect(UriInfo uriInfo, String redirectUri, String path) {
        String state = getStateCode();
        if (path != null) {
            state += "#" + path;
        }

        UriBuilder uriBuilder = UriBuilder.fromUri(authUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state);
        if (scope != null) {
            uriBuilder.queryParam("scope", scope);
        }
        URI url = uriBuilder.build();

        NewCookie cookie = new NewCookie(getStateCookieName(), state, getStateCookiePath(uriInfo), null, null, -1, isSecure, true);
        logger.debug("NewCookie: " + cookie.toString());
        logger.debug("Oauth Redirect to: " + url);
        return Response.status(302)
                .location(url)
                .cookie(cookie).build();
    }

    public String getStateCookiePath(UriInfo uriInfo) {
        if (stateCookiePath != null) return stateCookiePath;
        return uriInfo.getBaseUri().getRawPath();
    }

    public String getBearerToken(UriInfo uriInfo, HttpHeaders headers) throws BadRequestException, InternalServerErrorException {
        String error = getError(uriInfo);
        if (error != null) throw new BadRequestException(new Exception("OAuth error: " + error));
        checkStateCookie(uriInfo, headers);
        String code = getAccessCode(uriInfo);
        if (code == null) throw new BadRequestException(new Exception("code parameter was null"));
        return resolveBearerToken(uriInfo.getRequestUri().toString(), code);
    }

    public String getError(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst("error");
    }

    public String getAccessCode(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst("code");
    }

    public String checkStateCookie(UriInfo uriInfo, HttpHeaders headers) {
        Cookie stateCookie = headers.getCookies().get(stateCookieName);
        if (stateCookie == null) throw new BadRequestException("state cookie not set");
        String state = uriInfo.getQueryParameters().getFirst("state");
        if (state == null) throw new BadRequestException("state parameter was null");
        if (!state.equals(stateCookie.getValue())) {
            throw new BadRequestException("state parameter invalid");
        }
        if (state.indexOf('#') != -1) {
            return state.substring(state.indexOf('#') + 1);
        } else {
            return null;
        }
    }
}
