package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.EncodeAuthHeader;
import com.modusbox.client.processor.TrimIdValueFromToQuoteRequest;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class QuotesRouter extends RouteBuilder {

	private static final String TIMER_NAME = "histogram_post_quoterequests_timer";

    public static final Counter reqCounter = Counter.build()
            .name("counter_post_quoterequests_requests_total")
            .help("Total requests for POST /quoterequests.")
            .register();

    private static final Histogram reqLatency = Histogram.build()
            .name("histogram_post_quoterequests_request_latency")
            .help("Request latency in seconds for POST /quoterequests.")
            .register();

    private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
    private final TrimIdValueFromToQuoteRequest trimMFICode = new TrimIdValueFromToQuoteRequest();
    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:postQuoterequests").routeId("com.modusbox.postQuoterequests").doTry()
                .process(exchange -> {
                    reqCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
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
                .setHeader("Fineract-Platform-TenantId", constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}/transactions/template?command=repayment")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos API, postProcessBillerPayments: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
                .log("Mifos response,${body}")
                .transform(datasonnet("resource:classpath:mappings/postQuoterequestsResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of POST /quoterequests API')")
                /*
                 * END processing
                 */
                .doFinally().process(exchange -> {
					((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
				}).end()
        ;
    }
}
