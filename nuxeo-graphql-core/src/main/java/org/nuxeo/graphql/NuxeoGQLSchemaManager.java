package org.nuxeo.graphql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;

public class NuxeoGQLSchemaManager {

    private GraphQLInterfaceType documentInterface;

    private Map<String, GraphQLObjectType> docTypeToGQLType = new HashMap<>();

    public GraphQLSchema getNuxeoSchema() {
        buildNuxeoTypes();
        Set<GraphQLType> dictionary = new HashSet<>(docTypeToGQLType.values());
        return GraphQLSchema.newSchema().query(buildQueryType()).build(dictionary);
    }

    private GraphQLObjectType buildQueryType() {
        return newObject().name("RootQueryType")
                          .field(getDocumentQueryTypeField())
                          .field(getNXQLQueryTypeField())
                          .build();

    }

    private GraphQLFieldDefinition getNXQLQueryTypeField() {
        return newFieldDefinition().name("documents")
                                   .type(new GraphQLList(documentInterface))
                                   .argument(new GraphQLArgument("nxql", new GraphQLNonNull(GraphQLString)))
                                   .dataFetcher(getNxqlQueryFetcher())
                                   .build();
    }

    /**
     * Builds the document query type.
     *
     * @return
     */
    private GraphQLFieldDefinition getDocumentQueryTypeField() {

        return newFieldDefinition().name("document")
                                   .type(documentInterface)
                                   .argument(new GraphQLArgument("path", GraphQLString))
                                   .argument(new GraphQLArgument("id", GraphQLString))
                                   .dataFetcher(getDocFetcher())
                                   .build();
    }

    /**
     * Build a list of GraphQL types corresponding to each Nuxeo doc type.
     *
     * @return
     */
    private void buildNuxeoTypes() {
        if (documentInterface == null) {
            documentInterface = newInterface().name("document")
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("path")
                                                                         .dataFetcher(
                                                                                 getDocPropertyFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("id")
                                                                         .dataFetcher(getDocPropertyFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(new GraphQLList(new GraphQLTypeReference("document")))
                                                                         .name("children")
                                                                         .dataFetcher(getChildrenDataFetcher())
                                                                         .build())

                                              .typeResolver(getNuxeoDocumentTypeResolver())
                                              .build();

            docTypeToGQLType = new HashMap<>();
            SchemaManager sm = Framework.getService(SchemaManager.class);

            for (DocumentType type : sm.getDocumentTypes()) {
                Builder docTypeBuilder = newObject().name(type.getName()).withInterface(documentInterface);
                docTypeBuilder.field(newFieldDefinition().type(GraphQLString)//
                                                         .name("path")
                                                         .dataFetcher(getDocPropertyFetcher())
                                                         .build())
                              .field(newFieldDefinition().type(GraphQLString)//
                                                         .name("id")
                                                         .dataFetcher(getDocPropertyFetcher())
                                                         .build())
                                .field(newFieldDefinition().type(new GraphQLList(new GraphQLTypeReference("document")))
                                        .name("children")
                                        .dataFetcher(getChildrenDataFetcher())
                                        .build());

                for (Schema schema : type.getSchemas()) {
                    String name = schema.getNamespace().prefix;
                    name = StringUtils.isNotBlank(name) ? name : schema.getName();
                    docTypeBuilder.field(newFieldDefinition().name(name)
                                                             .type(typeForSchema(schema.getName()))
                                                             .dataFetcher(getSchemaFetcher(schema))
                                                             .build());
                }
                docTypeToGQLType.put(type.getName(), docTypeBuilder.build());
            }
        }

    }

    /**
     * Creates a GQL type for a Nuxeo schema
     *
     * @param schemaName
     * @return
     */
    private GraphQLObjectType typeForSchema(String schemaName) {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        Schema s = sm.getSchema(schemaName);

        Builder schemaBuilder = newObject().name("schema_" + schemaName);

        for (Field f : s.getFields()) {
            Type t = f.getType();
            if (t.isSimpleType()) {
                graphql.schema.GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition().name(
                        f.getName().getLocalName()).dataFetcher(
                                dataModelPropertyFetcher(f.getName().getPrefixedName()));
                if (t instanceof StringType) {
                    fieldBuilder.type(GraphQLString);
                    schemaBuilder.field(fieldBuilder.build());
                } else if (t instanceof BooleanType) {
                    fieldBuilder.type(GraphQLBoolean);
                    schemaBuilder.field(fieldBuilder.build());
                } else if (t instanceof DateType) {
                    fieldBuilder.type(GraphQLString);
                    schemaBuilder.field(fieldBuilder.build());
                } else if (t instanceof DoubleType) {
                    fieldBuilder.type(GraphQLFloat);
                    schemaBuilder.field(fieldBuilder.build());
                } else if (t instanceof IntegerType) {
                    fieldBuilder.type(GraphQLInt);
                    schemaBuilder.field(fieldBuilder.build());
                } else if (t instanceof LongType) {
                    fieldBuilder.type(GraphQLLong);
                    schemaBuilder.field(fieldBuilder.build());
                }

            }
        }
        return schemaBuilder.build();
    }

    private DataFetcher dataModelPropertyFetcher(final String prefixedName) {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                Object source = environment.getSource();
                if (source instanceof DataModel) {
                    DataModel dm = (DataModel) source;
                    return dm.getValue(prefixedName);
                }
                return null;
            }
        };
    }

    private TypeResolver getNuxeoDocumentTypeResolver() {
        return new TypeResolver() {

            @Override
            public GraphQLObjectType getType(Object object) {
                if (object instanceof DocumentModel) {
                    return docTypeToGQLType.get(((DocumentModel) object).getType());
                } else {
                    return null;
                }
            }
        };
    }

    private DataFetcher getDocFetcher() {
        return new DataFetcher() {

            public Object get(DataFetchingEnvironment environment) {
                String path = environment.getArgument("path");
                String id = environment.getArgument("id");

                Object ctx = environment.getContext();
                if (ctx instanceof CoreSession) {
                    if (path != null) {
                        return ((CoreSession) ctx).getDocument(new PathRef(path));
                    }
                    if (id != null) {
                        return ((CoreSession) ctx).getDocument(new IdRef(id));
                    }
                }
                return null;
            }
        };

    }

    private DataFetcher getNxqlQueryFetcher() {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                String nxql = environment.getArgument("nxql");
                Object ctx = environment.getContext();
                if (ctx instanceof CoreSession) {
                    return ((CoreSession) ctx).query(nxql);
                }
                return null;
            }
        };
    }

    private DataFetcher getChildrenDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                Object ctx = environment.getContext();
                if (ctx instanceof CoreSession) {

                    if (environment.getSource() instanceof DocumentModel) {
                        DocumentModel doc = (DocumentModel) environment.getSource();
                        return ((CoreSession) ctx).getChildren(doc.getRef());
                    }
                    return null;
                }
                return null;
            }

        };
    }

    private DataFetcher getSchemaFetcher(final Schema schema) {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) environment.getSource();
                    return doc.getDataModel(schema.getName());
                }
                return null;
            }

        };
    }

    private DataFetcher getDocPropertyFetcher() {
        return new DataFetcher() {
            public Object get(DataFetchingEnvironment environment) {
                String fieldName = environment.getFields().get(0).getName();
                if (environment.getSource() instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) environment.getSource();
                    if ("path".equals(fieldName)) {
                        return doc.getPathAsString();
                    } else if ("id".equals(fieldName)) {
                        return doc.getId();
                    }
                }
                return null;
            }

        };
    }

}
