package io.github.fbiville.testcontainers.neo4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

public class ClearDatabaseExtensionTest {

	private static final String ADMIN_PASSWORD = "j4eon";

	private static final Neo4jContainer<?> CONTAINER = new Neo4jContainer<>(String.format("neo4j:%s", neo4jVersion()))
			.withAdminPassword(ADMIN_PASSWORD)
			.withPlugins(MountableFile.forHostPath(extensionPath()))
			.withNeo4jConfig("dbms.unmanaged_extension_classes", String
					.format("%s=/ext", ClearDatabaseExtension.class.getPackage().getName()))
			.withEnv("NEO4J_dbms_unmanagedExtensionClasses", String
					.format("%s=/ext", ClearDatabaseExtension.class.getPackage().getName()));;

	private static final OkHttpClient httpClient = new OkHttpClient.Builder()
			.authenticator((route, response) -> response.request()
					.newBuilder()
					.header("Authorization", Credentials.basic("neo4j", ADMIN_PASSWORD))
					.build())
			.build();

	@BeforeClass
	public static void prepare_all() {
		CONTAINER.start();
	}

	@AfterClass
	public static void clean_up_all() {
		CONTAINER.stop();
	}

	@Test
	public void unmanaged_extension_is_called() throws IOException {
		String url = String.format("%s/ext/clearDb", CONTAINER.getHttpUrl());
		Request request = new Request.Builder()
				.url(url)
				.header("Accept", "application/json")
				.post(RequestBody.create("", MediaType.parse("application/json")))
				.build();

		try (Response response = httpClient.newCall(request).execute()) {
			assertThat(response.isSuccessful())
					.describedAs(String
							.format("Expected to successfully call unmanaged extension at URL: %s but got status: %d", url, response
									.code()))
					.isTrue();
		}
	}

	private static String neo4jVersion() {
		return readSingleLine("/neo4j.version");
	}

	private static String extensionPath() {
		return readSingleLine("/extension.location");
	}


	private static String readSingleLine(String classpathResource) {
		List<String> lines = readLines(classpathResource);
		int lineCount = lines.size();
		if (lineCount != 1) {
			throw new RuntimeException(String
					.format("%s (filtered) resource should contain exactly 1 line, found: %d", classpathResource, lineCount));
		}
		return lines.iterator().next();
	}

	private static List<String> readLines(String classpathResource) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ClearDatabaseExtensionTest.class
				.getResourceAsStream(classpathResource)))) {
			return reader.lines().collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
