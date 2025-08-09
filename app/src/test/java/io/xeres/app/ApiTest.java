/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app;

import io.xeres.common.location.Availability;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.config.OwnIdentityRequest;
import io.xeres.common.rest.config.OwnLocationRequest;
import io.xeres.common.rest.config.OwnProfileRequest;
import io.xeres.common.rest.forum.CreateForumGroupRequest;
import io.xeres.common.rest.profile.RsIdRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.ApiTest.DATADIR_PATH;
import static io.xeres.common.rest.PathConfig.*;
import static org.springframework.boot.test.context.SpringBootTest.UseMainMethod.ALWAYS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(args = {"--no-gui", "--no-https", "--no-control-password", "--fast-shutdown", "--data-dir=" + DATADIR_PATH}, useMainMethod = ALWAYS, webEnvironment = RANDOM_PORT) // Do not add --server-only, or it'll break PeerConnectionJob
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiTest
{
	static final String DATADIR_PATH = "./data-apitest";

	private static final String PROFILE_NAME = "foobar";
	private static final String LOCATION_NAME = "earth";
	private static final String IDENTITY_NAME = "foobar";

	@LocalServerPort
	private int port;

	@Autowired
	private WebTestClient webTestClient;

	// We need to clean on startup and cleanup because it's tricky to do it on
	// shutdown (some files are used still, like the memory mapped bloom filter, and it doesn't
	// seem possible to close it without some nasty hacks)
	@BeforeAll
	static void setup()
	{
		deleteApiDir();
	}

	@AfterAll
	static void cleanup()
	{
		deleteApiDir();
	}

	@Test
	@Order(1)
	void createOwnProfile()
	{
		var profileRequest = new OwnProfileRequest(PROFILE_NAME);

		webTestClient.post()
				.uri(CONFIG_PATH + "/profile")
				.bodyValue(profileRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().location(getServerUri() + PROFILES_PATH + "/1")
				.expectBody().isEmpty();
	}

	@Test
	@Order(2)
	void createOwnLocation()
	{
		var locationRequest = new OwnLocationRequest(LOCATION_NAME);

		webTestClient.post()
				.uri(CONFIG_PATH + "/location")
				.bodyValue(locationRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().location(getServerUri() + LOCATIONS_PATH + "/1")
				.expectBody().isEmpty();
	}

	@Test
	@Order(3)
	void createOwnIdentity()
	{
		var identityRequest = new OwnIdentityRequest(IDENTITY_NAME, false);

		webTestClient.post()
				.uri(CONFIG_PATH + "/identity")
				.bodyValue(identityRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().location(getServerUri() + IDENTITIES_PATH + "/1")
				.expectBody().isEmpty();
	}

	@Test
	@Order(4)
	void importFriend()
	{
		var rsId = "ABBzjqGSBk4/IOdmQ4zJMFvVAQdOZW1lc2lzAxQG1LRG0gnnUvpxGjl5KyDKZX4nBpENBNJmb28uYmFyLmNvbZIGAwIBVQTSkwYyAajABNICFGlwdjQ6Ly84NS4xLjIuNDoxMjM0BAOiD+U=";

		var rsIdRequest = new RsIdRequest(rsId);

		webTestClient.post()
				.uri(PROFILES_PATH)
				.bodyValue(rsIdRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().location(getServerUri() + PROFILES_PATH + "/2")
				.expectBody().isEmpty();
	}

	@Test
	@Order(5)
	void checkFriend()
	{
		webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(PROFILES_PATH)
						.queryParam("name", "Nemesis")
						.build())
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.[0].name").isEqualTo("Nemesis");
	}

	@Test
	@Order(6)
	void changeAvailability()
	{
		webTestClient.put()
				.uri(CONFIG_PATH + "/location/availability")
				.bodyValue(Availability.BUSY)
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();
	}

	@Test
	@Order(7)
	void getSettings()
	{
		webTestClient.get()
				.uri(SETTINGS_PATH)
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	@Order(8)
	void createForum()
	{
		var request = new CreateForumGroupRequest("Test", "Just some test forum");

		webTestClient.post()
				.uri(FORUMS_PATH + "/groups")
				.bodyValue(request)
				.exchange()
				.expectStatus().isCreated()
				.expectBody().isEmpty();
	}

	@Test
	@Order(9)
	void createChatRoom()
	{
		var request = new CreateChatRoomRequest("Test", "Anything, really", ChatRoomVisibility.PUBLIC, true);

		webTestClient.post()
				.uri(CHAT_PATH + "/rooms")
				.bodyValue(request)
				.exchange()
				.expectStatus().isCreated()
				.expectBody().isEmpty();
	}

	private String getServerUri()
	{
		return "http://localhost:" + port;
	}

	private static void deleteApiDir()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try
			{
				deleteRecursively(Path.of(DATADIR_PATH));
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}));
	}

	private static void deleteRecursively(Path path) throws IOException
	{
		final List<IOException> exceptions = new ArrayList<>();
		Files.walkFileTree(path, new SimpleFileVisitor<>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			{
				try
				{
					Files.deleteIfExists(file);
				}
				catch (IOException e)
				{
					exceptions.add(e);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
				if (exc != null)
				{
					throw exc;
				}
				try
				{
					Files.delete(dir);
				}
				catch (IOException e)
				{
					exceptions.add(e);
				}
				return FileVisitResult.CONTINUE;
			}
		});

		// If any exceptions occurred, throw a combined exception
		if (!exceptions.isEmpty())
		{
			var wrapper = new IOException("Errors recursively deleting " + path);
			for (IOException exception : exceptions)
			{
				wrapper.addSuppressed(exception);
			}
			throw wrapper;
		}
	}
}
