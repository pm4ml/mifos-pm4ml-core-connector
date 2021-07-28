package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;


public class TransfersRouter extends RouteBuilder {

    private static final String TIMER_NAME_POST = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";

    public static final Counter reqCounterPost = Counter.build()
            .name("counter_post_transfers_requests_total")
            .help("Total requests for POST /transfers.")
            .register();

    public static final Counter reqCounterPut = Counter.build()
            .name("counter_put_transfers_requests_total")
            .help("Total requests for PUT /transfers.")
            .register();

    private static final Histogram reqLatencyPost = Histogram.build()
            .name("histogram_post_transfers_request_latency")
            .help("Request latency in seconds for POST /transfers.")
            .register();

    private static final Histogram reqLatencyPut = Histogram.build()
            .name("histogram_put_transfers_request_latency")
            .help("Request latency in seconds for PUT /transfers.")
            .register();

    private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
    private final TrimIdValueFromToTransferRequestInbound trimMFICode = new TrimIdValueFromToTransferRequestInbound();
    private final SetAmountForPostTransfer setAmountForPostTransfer = new SetAmountForPostTransfer();
    private final SetPaymentTypeName setPaymentTypeName = new SetPaymentTypeName();
    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
    private GenerateTimestamp generateTimestamp = new GenerateTimestamp();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // POST /transfers to send the bill payment
        from("direct:postTransfers").routeId("com.modusbox.postTransfers").doTry()
                .process(exchange -> {
                    reqCounterPost.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_POST, reqLatencyPost.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Request transfer API called (loan repayment)")
                .log("POST transfers")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, POST /Transfer', " +
                        "null, null, 'Input Payload: ${body}')")
                /*
                 * END processing
                 */
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_POST)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:putTransfers").routeId("com.modusbox.transfersTransferIdPut").doTry()
                .process(exchange -> {
                    reqCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, reqLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Request transfer API called (loan repayment)")
                .log("PUT /transfers/{transferId}")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/{transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
                .process(trimMFICode)
                .process(setAmountForPostTransfer)
                // Generate timeStamp which we need in mapping
                .process(generateTimestamp)
                .process(setPaymentTypeName)
                .to("direct:getAllPaymentTypes")
                // Prepare request body
                .bean("postTransfersRequest")
                // Get a valid Authorization header for oAuth
                //.to("direct:getAuthHeader")
                //  Set a valid Basic Auth Header and encode it
                .setProperty("authHeader", simple("${properties:dfsp.username}:${properties:dfsp.password}"))
                .process(encodeAuthHeader)
                // Prepare downstream call
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Fineract-Platform-TenantId", constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}/transactions?command=repayment")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos API, LoanRepaymentResponse: ${body}', " +
                        "'Tracking the loan repayment response', 'Verify the response', null)")
                .log("Mifos response,${body}")
                // Format the response
                .bean("postTransfersResponse")
                .log("postTransfersResponse,${body}")
                /*
                 * END processing
                 */
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        // API call to Mifos to get all payment types
        from("direct:getAllPaymentTypes")
                .routeId("getAllPaymentTypes")
                .log("Get All PaymentTypes")
                .setBody(simple("{}"))
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("Fineract-Platform-TenantId", constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setProperty("authHeader", simple("${properties:dfsp.username}:${properties:dfsp.password}"))
                .process(encodeAuthHeader)
                .toD("{{dfsp.host}}/v1/paymenttypes")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos API, PaymentTypes: ${body}', " +
                        "'Tracking the payment response', 'Verify the response', null)")
                // Save response as property to use later
                .bean("getPaymentTypesResponse")
                .unmarshal().json()
                .setHeader("paymentType", body())
        ;
    }
}
