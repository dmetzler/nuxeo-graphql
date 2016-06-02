package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;

public interface GraphQLService {

    Object query(CoreSession session, String gqlQuery);

}
