# Nuxeo GraphQL

This project is an attempt to run GraphQL queries on top of a Nuxeo repository.
Sample usage of the service :

        GraphQLService gql = Framework.getService(GraphQLService.class);
        gql.query(session, "{document(path:\"/default-domain/workspaces/test\") "
                + "{ id "
                + "  path "
                + "  ...on Workspace { "
                + "        dc { "
                + "           title"
                + "        }"
                + "     }"
                + "}"
                + "}");

        // {document={id=7c71ee43-a4e9-4c0a-b6ce-73ebcc9077d6, path=/default-domain/workspaces/test, dc={title=workspace}}}



## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
