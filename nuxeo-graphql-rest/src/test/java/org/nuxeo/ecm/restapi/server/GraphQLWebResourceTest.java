package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URLEncoder;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.impl.MultiPartWriter;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Deploy({ "nuxeo.graphql", "nuxeo.graphql.rest" })
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class GraphQLWebResourceTest {

    @Inject
    CoreSession session;

    private WebResource resource;

    @Before
    public void doBefore() {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);

        client.addFilter(new HTTPBasicAuthFilter("Administrator", "Administrator"));

        resource = client.resource("http://localhost:18080/api/v1/");

    }

    @Test
    public void should_query_documents_with_query_params() throws Exception {
        // Given a query for a document
        DocumentModel note = RestServerInit.getNote(0, session);
        String query = getNotQuery(note);

        ClientResponse response = resource.path("gql")
                                          .queryParam("q", URLEncoder.encode(query, "UTF-8"))
                                          .get(ClientResponse.class);

        // Then i get a document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        checResponse(note, response);

    }

    @Test
    public void should_query_documents_with_body() throws Exception {
        // Given a query for a document
        DocumentModel note = RestServerInit.getNote(0, session);
        String query = getNotQuery(note);

        ClientResponse response = resource.path("gql").post(ClientResponse.class, query);

        // Then i get a document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        checResponse(note, response);
    }


    private void checResponse(DocumentModel note, ClientResponse response) throws IOException {
        JsonAssert json = JsonAssert.on(response.getEntity(String.class));

        JsonAssert docJson = json.has("document");
        docJson.has("id").isEquals(note.getId());
        docJson.has("path").isEquals(note.getPathAsString());
        JsonAssert noteJson = docJson.has("dc");
        noteJson.has("title").isEquals((String) note.getPropertyValue("dc:title"));
    }

    private String getNotQuery(DocumentModel note) {
        String query = "{ document(path:\"" + note.getPathAsString() + "\") { id path ...on Note {dc{ title}}}}";
        return query;
    }

}
