package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.EncodeAuthHeader;
import com.modusbox.client.processor.TrimIdValueFromToQuoteRequest;
import com.modusbox.client.processor.TrimIdValueFromToTransferRequestInbound;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class QuotesRouter extends RouteBuilder {

	private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
	private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
	private final TrimIdValueFromToQuoteRequest trimMFICode = new TrimIdValueFromToQuoteRequest();

	public void configure() {
		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

		from("direct:postQuoterequests")
				.routeId("com.modusbox.postQuoterequests")
				.log("POST Quotes API called")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Request received, POST /quoterequests', " +
						"null, null, 'Input Payload: ${body}')")
				.process(trimMFICode)
				.marshal().json()
				.setProperty("origPayload", simple("${body}"))
				.setBody(simple("{}"))
				.setProperty("authHeader", simple("${properties:dfsp.username}:${properties:dfsp.password}"))
				.process(encodeAuthHeader)
				.removeHeaders("CamelHttp*")
				.setHeader("Content-Type", constant("application/json"))
				.setHeader("Fineract-Platform-TenantId",constant("mfi5"))
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}/transactions/template?command=repayment")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Response from Mifos API, postProcessBillerPayments: ${body}', " +
						"'Tracking the response', 'Verify the response', null)")
				.log("Mifos response,${body}")
				.bean("postQuoterequestsResponse")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Final Response: ${body}', " +
						"null, null, 'Response of POST /quoterequests API')")
		;
    }
}
