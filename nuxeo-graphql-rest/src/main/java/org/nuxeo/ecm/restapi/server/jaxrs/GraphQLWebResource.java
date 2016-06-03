package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.net.URLDecoder;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.graphql.GraphQLService;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "gql")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLWebResource extends DefaultObject {

    private ObjectMapper mapper = new ObjectMapper();

    @GET
    public String doGet(@QueryParam("q") String query, @QueryParam("pretty") boolean pretty) throws IOException {

        query = URLDecoder.decode(query, "UTF-8");
        return query(query, pretty);
    }

    @POST
    public String doPost(@QueryParam("pretty") boolean pretty, String query) throws IOException {
        return query(query, pretty);
    }

    private String query(String query, boolean pretty) throws IOException {
        CoreSession session = ctx.getCoreSession();

        GraphQLService gql = Framework.getService(GraphQLService.class);
        Object result = gql.query(session, query);

        if (pretty) {
            return mapper.defaultPrettyPrintingWriter().writeValueAsString(result);
        } else {
            return mapper.writeValueAsString(result);
        }
    }
}
