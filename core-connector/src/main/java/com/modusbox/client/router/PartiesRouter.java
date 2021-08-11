package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.EncodeAuthHeader;
import com.modusbox.client.processor.SetClientIdForClientInfo;
import com.modusbox.client.processor.TrimIdValueFromHeader;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;


public class PartiesRouter extends RouteBuilder {

    private static final String TIMER_NAME = "histogram_get_parties_timer";

    public static final Counter reqCounter = Counter.build()
            .name("counter_get_parties_requests_total")
            .help("Total requests for GET /parties.")
            .register();

    private static final Histogram reqLatency = Histogram.build()
            .name("histogram_get_parties_request_latency")
            .help("Request latency in seconds for GET /parties.")
            .register();

    private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
    private final TrimIdValueFromHeader trimIdValueFromHeader = new TrimIdValueFromHeader();
    private final SetClientIdForClientInfo clientIDInfo = new SetClientIdForClientInfo();
    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // In this case the GET parties will return the loan account with customer details
        from("direct:getParties").routeId("com.modusbox.getParties").doTry()
                .process(exchange -> {
                    reqCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
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
                .setHeader("Fineract-Platform-TenantId", constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/clients/${header.processedClientId}")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos Clients API, ${body}', " +
                        "'Tracking the clientInfo response', 'Verify the response', null)")
                // Format the response
                .transform(datasonnet("resource:classpath:mappings/getPartiesResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final getPartiesResponse: ${body}', " +
                        "null, null, 'Response of GET parties/${header.idType}/${header.idValue} API')")
                /*
                 * END processing
                 */
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:getLoanInfo")
                .routeId("getLoanInfo")
                .setBody(simple("{}"))
                .setProperty("authHeader", simple("${properties:dfsp.username}:${properties:dfsp.password}"))
                .process(encodeAuthHeader)
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Fineract-Platform-TenantId", constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}")
                .log("Mifos getLoanInfo response,${body}")
                .transform(datasonnet("resource:classpath:mappings/getLoanInfoResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .process(clientIDInfo)
        ;

    }
}
