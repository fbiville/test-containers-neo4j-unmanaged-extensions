# Deploying an unmanaged extension to `Neo4jContainer`

## Fixes

Thanks https://github.com/michael-simons!

### Missing `@Path` at class level

See https://github.com/fbiville/test-containers-neo4j-unmanaged-extensions/commit/6ef212a02c9e53be87999a8c0aa6a1072b29d780.

It fixes the problem for all latest Neo4j 3.x but not for 3.0.0.

Run:
```shell
$ for v in 3.0.12 3.1.9 3.2.14 3.3.9 3.4.17 3.5.18; do 
	mvn clean package -Dneo4j.version=$v; 
done
```

### Envvar workaround

See https://github.com/fbiville/test-containers-neo4j-unmanaged-extensions/commit/4db6e2542d806c03f040a0b911a8fd4384413081.

Add `.withEnv("NEO4J_dbms_unmanagedExtensionClasses", "...")` to export the unmanaged extension configuration for Neo4j 3.0.0.

Now the extension should work for all Neo4j 3.x versions:
```shell
$ for v in 3.0.12 3.1.9 3.2.14 3.3.9 3.4.17 3.5.18; do 
	mvn clean package -Dneo4j.version=$v; 
done
```

## Reproduction

To reproduce the problem, just run:

```shell
$ git reset --hard 416174be2467dba76b49ea6beb6eb2da7249b75a
$ mvn test
```

The test will fail.

> You can change the Neo4j version via `-Dneo4j.version`. The goal is for this to work across all Neo4j **3.x** versions.

## Container inspection

Suspend the execution of `io.github.fbiville.testcontainers.neo4j.ClearDatabaseExtensionTest.unmanaged_extension_is_called` (set a breakpoint at the beginning for instance).
Then:

1. find the running container via `docker ps`
```
CONTAINER ID        IMAGE                               COMMAND                  CREATED             STATUS              PORTS                                                                       NAMES
61fb36dbdc2f        neo4j:3.0.0                         "/docker-entrypoint.…"   11 seconds ago      Up 10 seconds       0.0.0.0:33511->7473/tcp, 0.0.0.0:33510->7474/tcp, 0.0.0.0:33509->7687/tcp   heuristic_dewdney
3e1e00c9ebcb        quay.io/testcontainers/ryuk:0.2.3   "/app"                   12 seconds ago      Up 11 seconds       0.0.0.0:33508->8080/tcp                                                     testcontainers-ryuk-eb8da151-b1a2-4122-be75-608cae2bcb45
```
1. start an interactive shell session via `docker exec -it ${CONTAINER_ID} /bin/bash` (in the example, `CONTAINER_ID` == `61fb36dbdc2f`)
1. check the environment, notice that the expected unmanaged setting is there:
```
root@61fb36dbdc2f:/var/lib/neo4j# env | grep unmanaged
NEO4J_dbms_unmanaged__extension__classes=io.github.fbiville.testcontainers.neo4j=/ext
```
1. check the configuration, notice it has not been changed and does **not** enable the unmanaged extension
```
root@61fb36dbdc2f:/var/lib/neo4j# grep unmanaged conf/neo4j.conf
# org.neo4j.examples.server.unmanaged.HelloWorldResource.java from
# neo4j-server-examples under /examples/unmanaged, resulting in a final URL of
# http://localhost:7474/examples/unmanaged/helloworld/{nodeId}
#dbms.unmanaged_extension_classes=org.neo4j.examples.server.unmanaged=/examples/unmanaged
```
