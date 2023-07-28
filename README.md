# Keycloak REST API Mapper

This custom mapper for keycloak allows querying a URL (e.g. REST API) and add the result as a claim.
You can configure the URL, Username/Password for HTTP Basic Auth and a timeout in seconds.

If anything fails, "ERROR" will simply be put in the claim for now.

# Setup 

This has been tested with Keycloak 21 using the official Quarkus Container and OpenJDK 17.

# Shoutouts

This project is heavily based/inspired by the following repos:

https://github.com/mschwartau/keycloak-custom-protocol-mapper-example
https://github.com/dasniko/keycloak-tokenmapper-example
https://github.com/tholst/keycloak-json-graphql-remote-claim
