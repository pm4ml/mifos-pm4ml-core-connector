package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class AuthRouter extends RouteBuilder {

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() throws Exception {
        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:getAuthHeader")
                .setProperty("downstreamRequestBody", simple("${body}"))
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setBody(simple("{}"))
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                                                                    "'Calling Finflux API, getAuthToken', " +
                                                                    "'Tracking the request', 'Track the response', " +
                                                                    "'Request sent to, GET https://{{finflux.hana.host}}/oauth/token')")
                .to("https://{{finflux.hana.host}}/oauth/token?" +
                        "username={{finflux.hana.username}}" +
                        "&password={{finflux.hana.password}}" +
                        "&client_id={{finflux.hana.client_id}}" +
                        "&grant_type={{finflux.hana.grant_type}}" +
                        "&client_secret={{finflux.hana.client_secret}}")
                .unmarshal().json(JsonLibrary.Jackson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                                                                    "'Response from Finflux API, getAuthToken: Auth Token has been generated', " +
                                                                    "null, null, null)")
                .setHeader("Authorization", simple("Bearer ${body['access_token']}"))
                .setBody(simple("${exchangeProperty.downstreamRequestBody}"))
        ;
    }
}
