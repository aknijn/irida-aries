package ca.corefacility.bioinformatics.irida.security.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.google.common.collect.Sets;

/**
 * Security context factory listening for {@link WithMockOAuth2Client}
 * annotations on junit tests. Adds a OAuth2Authentication object into the
 * security context.
 * 
 *
 */
public class WithMockOAuth2SecurityContextFactory implements WithSecurityContextFactory<WithMockOAuth2Client> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SecurityContext createSecurityContext(WithMockOAuth2Client withClient) {
		// Get the username
		String username = withClient.username();
		if (username == null) {
			throw new IllegalArgumentException("Username cannot be null");
		}

		// Get the user roles
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		for (String role : withClient.roles()) {
			if (role.startsWith("ROLE_")) {
				throw new IllegalArgumentException("roles cannot start with ROLE_ Got " + role);
			}
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		// Get the client id
		String clientId = withClient.clientId();
		// get the oauth scopes
		String[] scopes = withClient.scope();
		Set<String> scopeCollection = Sets.newHashSet(scopes);

		// Create the UsernamePasswordAuthenticationToken
		User principal = new User(username, withClient.password(), true, true, true, true, authorities);
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
				principal.getAuthorities());

		// Create the authorization request and OAuth2Authentication object
		OAuth2Request authRequest = new OAuth2Request(null, clientId, null, true, scopeCollection, null, null, null,
				null);
		OAuth2Authentication oAuth = new OAuth2Authentication(authRequest, authentication);

		// Add the OAuth2Authentication object to the security context
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(oAuth);
		return context;
	}

}
