FROM busybox
ADD marketplace-graphql/marketplace/target/marketplace-nuxeo-graphql-*.zip /docker-entrypoint-initnuxeo.d/
ADD docker /docker-entrypoint-initnuxeo.d/
VOLUME /docker-entrypoint-initnuxeo.d/
RUN /bin/true
