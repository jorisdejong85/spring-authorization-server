/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import sample.jpa.repository.AuthorizationRepository;

import static io.restassured.RestAssured.given;

/**
 * Integration tests for the sample Authorization Server.
 *
 * @author Daniel Garnier-Moiroux
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DefaultAuthorizationServerJpaAccessTokenTests {

	private static final Integer SLEEP_DURATION = 1000;

	@Autowired
	AuthorizationRepository authorizationRepository;

	@Before
	public void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = 9000;
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

		authorizationRepository.deleteAll();
	}

	/**
	 * Sending multiple requests at once, or shortly after each other, results in multiple authorization records with
	 * the identical access token.
	 */
	@Test
	public void whenSendingParallelTokenRequestsThenRespondWithUniqueAccessTokens() throws InterruptedException {
		String accessToken = null;

		for (int i = 0; i < 10; i++) {
			accessToken = tokenRequest();
		}

		introspectRequest(accessToken);
	}

	/**
	 * Sending multiple requests with a delay (sleep) in between, results in multiple authorization records with unique access
	 * tokens.
	 */
	@Test
	public void whenSendingDelayedTokenRequestsThenRespondWithUniqueAccessTokens() throws InterruptedException {
		String accessToken = null;

		for (int i = 0; i < 10; i++) {
			accessToken = tokenRequest();

			Thread.sleep(SLEEP_DURATION);
		}

		introspectRequest(accessToken);
	}

	private String tokenRequest() {
		return
			given()
				.auth().preemptive().basic("messaging-client", "secret")
				.formParam("grant_type", "client_credentials")
				.formParam("client_id", "messaging-client")
				.formParam("client_secret", "secret")
				.formParam("scope", "message.read")
				.formParam("state", "some-state")
			.when()
				.post("/oauth2/token")
			.then()
				.assertThat()
					.statusCode(200)
				.extract()
					.path("access_token");
	}

	private void introspectRequest(String accessToken) {
		given()
			.auth().preemptive().basic("messaging-client", "secret")
			.formParam("token", accessToken)
		.when()
			.post("/oauth2/introspect")
		.then()
			.assertThat()
				.statusCode(200);
	}

}
