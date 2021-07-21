package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;


public class PartiesRouter extends RouteBuilder {

	private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
	private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
	private final TrimIdValueFromHeader trimIdValueFromHeader = new TrimIdValueFromHeader();
	private final SetClientIdForClientInfo clientIDInfo = new SetClientIdForClientInfo();
    public void configure() {
		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

		// In this case the GET parties will return the loan account with customer details
		from("direct:getParties")
				.routeId("com.modusbox.getParties")
				.log("Account lookup API called")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Request received, GET /parties/${header.idType}/${header.idValue}', " +


																	"null, null, 'fspiop-source: ${header.fspiop-source}')")
				.process(trimIdValueFromHeader)
				.setBody(simple("{}"))
				.to("direct:getLoanInfo")
				.log("LoanInfo1, ${header.processedClientId},")
				.removeHeaders("CamelHttp*")
				.setHeader("Content-Type", constant("application/json"))
				.setHeader("Fineract-Platform-TenantId",constant("mfi5"))
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.toD("{{dfsp.host}}/v1/clients/${header.processedClientId}")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Response from Mifos Clients API, ${body}', " +
																	"'Tracking the clientInfo response', 'Verify the response', null)")
				// Finflux always returns HTTP code 200, so we need to check response body for actual status code

				// Format the response
				.bean("getPartiesResponse")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Final getPartiesResponse: ${body}', " +
																	"null, null, 'Response of GET parties/${header.idType}/${header.idValue} API')")
		;


		from("direct:getLoanInfo")
				.routeId("getLoanInfo")
				.setBody(simple("{}"))
				.setProperty("authHeader", simple("${properties:dfsp.username}:${properties:dfsp.password}"))
				.process(encodeAuthHeader)
				.removeHeaders("CamelHttp*")
				.setHeader("Content-Type", constant("application/json"))
				.setHeader("Fineract-Platform-TenantId",constant("mfi5"))
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}")
				.log("Mifos getLoanInfo response,${body}")
				.bean("getLoanInfoResponse")
				.process(clientIDInfo);

	}
}
