#!/bin/bash

set +e

gosu $NUXEO_USER $NUXEO_HOME/bin/nuxeoctl mp-list|grep nuxeo-graphql > /dev/null 2>&1
if [ $? -eq 1 ]; then 
	gosu $NUXEO_USER $NUXEO_HOME/bin/nuxeoctl mp-install /docker-entrypoint-initnuxeo.d/marketplace-nuxeo-graphql-*.zip --relax=false --accept=true
fi
set -e