#!/bin/bash

gosu $NUXEO_USER $NUXEO_HOME/bin/nuxeoctl mp-install /docker-entrypoint-initnuxeo.d/marketplace-nuxeo-graphql-*.zip --relax=false --accept=true

