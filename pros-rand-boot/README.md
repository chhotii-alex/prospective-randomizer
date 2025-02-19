 # Spring Boot implementation of Prospective Randomizer
 
To build the Spring Boot version which uses the HTTP protocol, enter the commands:
```
cd prospective-randomizer
mvn install
```

It can then be started using this command:
```
mvn spring-boot:run -pl pros-rand-boot
```

Example code that demonstrates use of Prospective Randomizer appears [in this project](https://github.com/chhotii-alex/example-task).

A single-page app implemented in React which allows you to configure and monitor the Prospective Randomizer can be found
[here](https://github.com/chhotii-alex/randomizer-dashboard).

## API

This server offers the style of HTTP protocol often (inaccuratedly) referred to as a RESTful API: 
each command is implemented as an endpoint or path, and the response, if any, is
JSON-formatted. Complete detailed specifications of each endpoint is available via the server's SwaggerUI interface,
available via (for example) `http://localhost:8080/swagger-ui/index.html`. API endpoints are roughly equivalent to
the CLI or socket commands listed above. However, there are a couple of interesting improvements.

The Spring Boot implementation allows you to run multiple protocols (each with their own list of groups and their own
set of variables) on the same server independently. The groups and variables (along with a name used to identify
the protocol) are specified in an intial POST message that tells the server to start the protocol. That protocol
name must be used in every future protocol-specific communication with the server.

Endpoints:

GET /version returns the software's version identifier

POST /{protocolName}/start The request body must be a JSON object with two keys: 'groupNames' and 'variableSpec'. 
The value for 'groupNames' must be an array of strings, giving the group names. The value for 'variableSpec' must be
and array of strings, giving feature names. Each feature is assumed to be numeric continuous. The given protocol is
started.

GET /{protocolName}/subject/{id}
Equivalent to "EXISTS" above. Responds with true if there is already record of a subject with this ID; otherwise
responds with a 404 NOT FOUND status.

POST /{protocolName}/subject/{id} The request body must be a JSON object with key-value pairs giving the feature
values for the subject whose id appears in the path.

POST /{protocolName}/subject/{id}/group The request body must be a JSON object with key-value pairs giving the feature
values for the subject whose id appears in the path. This furthermore causes the given subject to be assigned to a 
group and the response gives the name of that group. 

GET /{protocolName}/subject/{id}/group This causes the given subject to be assigned to a group, if needed, and the
response gives the name of that group. The subject's feature values must have already been submitted via one of the
POST messages listed above.

POST /{protocolName}/assignall Response body should be empty. This triggers assigning all unassigned subjects to 
groups.

POST /{protocolName}/subject/{id}/commit Response body should be empty. This causes the given subject to be 
"committed"&mdash;that is, any attempt to submit new feature values for the given subject will be forbidden.

GET /{protocolName}/subject/{id}/committed returns true or false depending on whether the given subject has been
committed.

GET /{protocolName}/subjects returns information about the given protocol's subjects

GET /{protocolName}/variables returns information about the given protocol's variables or feature labels

GET /{protocolName}/groups returns information about the given protocol's groups

