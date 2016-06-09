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


## How to build

        # mvn clean install

## How to deploy 

On a vanilla Nuxeo installation you can run :

        # $NUXEO_HOME/bin/nuxeoCtl mp-install $GRAPHQL_HOME/marketplace-graphql/marketplace/target/marketplace-nuxeo-graphql-*.zip

## How to use

Your should now have a new API endpoint that you can use like this :

        GET http://localhost:8080/nuxeo/api/v1/gql?q={documents(nxql:%22SELECT%20*%20FROM%20Document%22)%20{%20id%20path%20...on%20Workspace%20{%20dc%20{%20title}}}}

or a more readable way with `POST` :

        POST http://localhost:8080/nuxeo/api/v1/gql
        {documents(nxql:"SELECT * FROM Document") {
                id
                path
                ... on Workspace {
                  dc {
                    title
                  }
                }
        }}

another query :

        # Several documents with aliases in one call 
        { 
          onedoc: document(path:"/default-domain/workspaces/test/note") {
            id 
            path
            ... on Note {
              dc {
                title
            }
          }
          anotherdoc: document(id:"1234-1234567-12341234-3456") {
            id path
          }
        }

# Run on docker ?

It's far more simple with Docker Compose and the Nuxeo image.
  
    mvn clean install
    docker-compose up -d

Then you can run your first query : http://$DOCKER_HOST:8080/nuxeo/api/v1/gql?q={documents(nxql:%22SELECT%20*%20FROM%20Document%22)%20{%20id%20path%20__typename}}

## Supported features

As this is a simple POC on top of GraphQL, very few features are supported :

 - Only scalar properties are usable (no complex, no content)
 - The GraphQL query schema only has two types :
         + document(id path)
         + documents(nxql)

## Expected features

 - full support of complex types
 - other query type like `user`, `directory`, `workflow`, `task`...
 - GraphQL mutations

## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
