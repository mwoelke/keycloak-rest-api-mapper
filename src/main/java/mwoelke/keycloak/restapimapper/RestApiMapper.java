package mwoelke.keycloak.restapimapper;

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

	static {
		configProperties.add(new ProviderConfigProperty(API_URL, "API URL", "URL of the REST API.", ProviderConfigProperty.STRING_TYPE, "https://example.org/api/endpoint"));
		configProperties.add(new ProviderConfigProperty(API_USERNAME, "API Username", "Username for HTTP Auth.", ProviderConfigProperty.STRING_TYPE, "admin"));
        configProperties.add(new ProviderConfigProperty(API_PASSWORD, "API Password", "Password for HTTP Auth.", ProviderConfigProperty.PASSWORD, "pass"));
        configProperties.add(new ProviderConfigProperty(API_TIMEOUT, "Timeout", "Timeout in seconds.", ProviderConfigProperty.STRING_TYPE, 3));

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
		ClientSessionContext clientSessionCtx
	) {
        String apiUrl = mappingModel.getConfig().get(API_URL);
        String apiUsername = mappingModel.getConfig().get(API_USERNAME);
		String apiPassword = mappingModel.getConfig().get(API_PASSWORD);
		int timeout = Integer.parseInt(mappingModel.getConfig().get(API_TIMEOUT));

		String res;

		//create client
		HttpClient client = HttpClient.newHttpClient();

		//make request
		try {
			HttpRequest getRequest = HttpRequest.newBuilder()
				.uri(new URI(apiUrl))
				.header("Authorization", getBasicAuthHeader(apiUsername, apiPassword))
				.timeout(Duration.ofSeconds(Long.valueOf(timeout)))
				.GET()
				.build();

			HttpResponse<String> response = client.send(getRequest, BodyHandlers.ofString());
			res = response.statusCode() == 200 ? response.body() : "ERROR";

			//res = response.body();
		} catch (Exception e) {
			//hardmap to ERROR on any exception (timeout, invalid url, ...)
			res = "ERROR";
		}

		OIDCAttributeMapperHelper.mapClaim(token, mappingModel, res);
	}

	private static final String getBasicAuthHeader(String username, String password) {
		String clearText = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(clearText.getBytes());
	}
}
