package mwoelke.keycloak.restapimapper;

import org.apache.http.client.utils.URIBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class RestApiMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

	public static final String PROVIDER_ID = "oidc-rest-api-mapper";

	private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	static final String API_URL = "apiUrl";
	static final String API_USERNAME = "apiUsername";
	static final String API_PASSWORD = "apiPassword";
	static final String API_TIMEOUT = "apiTimout";
	static final String USER_ADD_NAME = "userAddName";
	static final String USER_NAME_PARAMETER = "userNameParameter";

	static {
		// URL Field
		configProperties.add(new ProviderConfigProperty(API_URL, "API URL", "URL of the REST API.",
				ProviderConfigProperty.STRING_TYPE, "https://example.org/api/endpoint"));
		// HTTP Auth username Field
		configProperties.add(new ProviderConfigProperty(API_USERNAME, "API Username", "Username for HTTP Auth.",
				ProviderConfigProperty.STRING_TYPE, "admin"));
		// HTTP Auth Password field
		configProperties.add(new ProviderConfigProperty(API_PASSWORD, "API Password", "Password for HTTP Auth.",
				ProviderConfigProperty.PASSWORD, "pass"));
		// Timeout field
		configProperties.add(new ProviderConfigProperty(API_TIMEOUT, "Timeout", "Timeout in seconds.",
				ProviderConfigProperty.STRING_TYPE, 3));

		// Add username Checkbox
		configProperties.add(new ProviderConfigProperty(USER_ADD_NAME, "Add username?",
				"Should the username be added to the query?", ProviderConfigProperty.BOOLEAN_TYPE, true));

		// Username paramter name
		configProperties.add(new ProviderConfigProperty(USER_NAME_PARAMETER, "Username parameter",
				"Name of the parameter the username will get send with. Only takes effect if \"add username\" is enabled.",
				ProviderConfigProperty.STRING_TYPE, "username"));

		OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, RestApiMapper.class);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getDisplayType() {
		return "REST API";
	}

	@Override
	public String getHelpText() {
		return "Add arbitrary claims from any REST API endpoint.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	protected void setClaim(
			IDToken token,
			ProtocolMapperModel mappingModel,
			UserSessionModel userSession,
			KeycloakSession keycloakSession,
			ClientSessionContext clientSessionCtx) {
		// get config
		final String apiUrl = mappingModel.getConfig().get(API_URL);
		final String apiUsername = mappingModel.getConfig().get(API_USERNAME);
		final String apiPassword = mappingModel.getConfig().get(API_PASSWORD);
		final int timeout = Integer.parseInt(mappingModel.getConfig().get(API_TIMEOUT));
		final boolean addUsername = Boolean.parseBoolean(mappingModel.getConfig().get(USER_ADD_NAME));
		final String usernameParameter = mappingModel.getConfig().get(USER_NAME_PARAMETER);

		// result string
		JsonNode resultJson;

		// create client
		HttpClient client = HttpClient.newHttpClient();

		ObjectMapper mapper = new ObjectMapper();

		try {
			// build URI
			URIBuilder uriBuilder = new URIBuilder(apiUrl);

			// add username to parameter if config is selected and username is not empty
			if (addUsername && !"".equals(usernameParameter)) {
				uriBuilder.addParameter(usernameParameter, userSession.getLoginUsername());
			}

			// make request
			HttpRequest getRequest = HttpRequest.newBuilder()
					.uri(new URI(uriBuilder.toString()))
					.header("Authorization", getBasicAuthHeader(apiUsername, apiPassword))
					.timeout(Duration.ofSeconds(Long.valueOf(timeout)))
					.GET()
					.build();

			HttpResponse<String> response = client.send(getRequest, BodyHandlers.ofString());
			// parse json if status 200
			resultJson = (response.statusCode() == 200) ? mapper.readTree(response.body()) : null;

		} catch (Exception e) {
			// null results in the claim not getting set
			resultJson = null;
		}

		OIDCAttributeMapperHelper.mapClaim(token, mappingModel, resultJson);
	}

	/**
	 * Encode http auth username and password to base64
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	private static final String getBasicAuthHeader(String username, String password) {
		String clearText = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(clearText.getBytes());
	}
}
