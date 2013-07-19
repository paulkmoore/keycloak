package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.PublishedRealmRepresentation;
import org.keycloak.services.models.RealmModel;
import org.picketlink.idm.IdentitySession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmSubResource {
    protected static final  Logger logger = Logger.getLogger(RealmSubResource.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected IdentitySession identitySession;

    protected RealmModel realm;

    public RealmSubResource(RealmModel realm) {
        this.realm = realm;
    }

    @GET
    @Path("json")
    @Produces("application/json")
    public PublishedRealmRepresentation getRealm(@PathParam("realm") String id) {
        return realmRep(realm, uriInfo);
    }

    @GET
    @Path("html")
    @Produces("text/html")
    public String getRealmHtml(@PathParam("realm") String id) {
        StringBuffer html = new StringBuffer();

        String authUri = TokenService.loginPage(uriInfo).build(realm.getId()).toString();
        String codeUri = TokenService.accessCodeRequest(uriInfo).build(realm.getId()).toString();
        String grantUrl = TokenService.grantRequest(uriInfo).build(realm.getId()).toString();
        String idGrantUrl = TokenService.identityGrantRequest(uriInfo).build(realm.getId()).toString();

        html.append("<html><body><h1>Realm: ").append(realm.getName()).append("</h1>");
        html.append("<p>auth: ").append(authUri).append("</p>");
        html.append("<p>code: ").append(codeUri).append("</p>");
        html.append("<p>grant: ").append(grantUrl).append("</p>");
        html.append("<p>identity grant: ").append(idGrantUrl).append("</p>");
        html.append("<p>public key: ").append(realm.getPublicKeyPem()).append("</p>");
        html.append("</body></html>");

        return html.toString();
    }


    public static PublishedRealmRepresentation realmRep(RealmModel realm, UriInfo uriInfo) {
        PublishedRealmRepresentation rep = new PublishedRealmRepresentation();
        rep.setRealm(realm.getName());
        rep.setSelf(uriInfo.getRequestUri().toString());
        rep.setPublicKeyPem(realm.getPublicKeyPem());

        UriBuilder auth = uriInfo.getBaseUriBuilder();
        auth.path(RealmsResource.class).path(RealmsResource.class, "getTokenService")
                .path(TokenService.class, "requestAccessCode");
        rep.setAuthorizationUrl(auth.build(realm.getId()).toString());

        UriBuilder code = uriInfo.getBaseUriBuilder();
        code.path(RealmsResource.class).path(RealmsResource.class, "getTokenService").path(TokenService.class, "accessRequest");
        rep.setCodeUrl(code.build(realm.getId()).toString());

        UriBuilder grant = uriInfo.getBaseUriBuilder();
        grant.path(RealmsResource.class).path(RealmsResource.class, "getTokenService").path(TokenService.class, "accessTokenGrant");
        String grantUrl = grant.build(realm.getId()).toString();
        rep.setGrantUrl(grantUrl);

        UriBuilder idGrant = uriInfo.getBaseUriBuilder();
        grant.path(RealmsResource.class).path(RealmsResource.class, "getTokenService").path(TokenService.class, "identityTokenGrant");
        String idGrantUrl = idGrant.build(realm.getId()).toString();
        rep.setIdentityGrantUrl(idGrantUrl);
        return rep;
    }


}