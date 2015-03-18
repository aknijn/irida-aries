package ca.corefacility.bioinformatics.irida.ria.web.oauth;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for handling OAuth2 authorization codes for the Galaxy exporter
 * 
 * @author Joel Thiessen <joel.thiessen@phac-aspc.gc.ca>
 *
 */

@Controller
public class GalaxyRedirectionEndpointController {

	private static final Logger logger = LoggerFactory.getLogger(GalaxyRedirectionEndpointController.class);

	/**
	 * Receive the OAuth2 authorization code from IRIDA and pass it on to the client-side code
	 * @param model
	 *            the model to write to
	 * @param request
	 *            the incoming request
	 * @param session
	 *            the user's session
	 * @return a template that will pass on the authorization code
	 * @throws OAuthProblemException if a valid OAuth authorization response cannot be created
	 * @throws IllegalStateException if the callback URL is removed from an invalid session
	 */
	@RequestMapping("galaxy/auth_code")
	public String passAuthCode(Model model, HttpServletRequest request, HttpSession session
			) throws OAuthProblemException, IllegalStateException {
		logger.debug("\nParsing auth code from HttpServletRequest");
		// Get the OAuth2 authorization code
		OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
		String code = oar.getCode();
		logger.debug("\nReceived auth code: " + code);
		model.addAttribute("auth_code", code);
		
		session.removeAttribute("galaxyExportToolCallbackURL");
		
		return "templates/galaxy_auth_code.tmpl";
	}
}