# Keycloak REST API Mapper

This custom mapper for keycloak allows querying a URL (e.g. REST API) and add the JSON result as a claim.
You can configure the URL, Username/Password for HTTP Basic Auth and a timeout in seconds.
Optionally, you may also add the users username in a configurable URL parameter.

If anything fails (network, auth, ...), the claim will not be included.

Note: The result hast to be valid JSON!

# Setup 

This has been tested with Keycloak 21 using the official Quarkus Container and OpenJDK 17.

Compile the source using maven, then copy the resulting JAR into the `providers/` folder of your keycloak installation.

Afterwards register the new provider using the keycloak CLI: `./kc.sh build`

# Shoutouts

This project is heavily based/inspired by the following repos:

https://github.com/mschwartau/keycloak-custom-protocol-mapper-example

https://github.com/dasniko/keycloak-tokenmapper-example

https://github.com/tholst/keycloak-json-graphql-remote-claim
