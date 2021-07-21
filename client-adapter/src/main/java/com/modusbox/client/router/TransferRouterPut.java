package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class TransferRouterPut extends RouteBuilder {

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
    private final EncodeAuthHeader encodeAuthHeader = new EncodeAuthHeader();
    private GenerateTimestamp generateTimestamp = new GenerateTimestamp();
    private final TrimIdValueFromToTransferRequestInbound trimMFICode = new TrimIdValueFromToTransferRequestInbound();
    private final SetAmountForPostTransfer  setAmountForPostTransfer = new SetAmountForPostTransfer();
    private final SetPaymentTypeName setPaymentTypeName =new SetPaymentTypeName();

    @Override
    public void configure() throws Exception {

        exceptionHandlingConfigurer.configureExceptionHandling(this);
        // POST /transfers to send the bill payment
        from("direct:putTransfers")
                .routeId("com.modusbox.transfersTransferIdPut")
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
                .setProperty("authHeader", simple("${properties:cbs.username}:${properties:cbs.password}"))
                .process(encodeAuthHeader)
                // Prepare downstream call
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Fineract-Platform-TenantId",constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("{{cbs.host}}/v1/loans/${header.idValueTrimmed}/transactions?command=repayment")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos API, LoanRepaymentResponse: ${body}', " +
                        "'Tracking the loan repayment response', 'Verify the response', null)")
                // Finflux always returns HTTP code 200, so we need to check response body for actual status code

                .log("Mifos response,${body}")
                // Format the response
                .bean("postTransfersResponse")
                .log("postTransfersResponse,${body}")

        ;


        // API call to Mifos to get all payment types
        from("direct:getAllPaymentTypes")
                .routeId("getAllPaymentTypes")
                .log("Get All PaymentTypes")
                .setBody(simple("{}"))
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("Fineract-Platform-TenantId",constant("mfi5"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setProperty("authHeader", simple("${properties:cbs.username}:${properties:cbs.password}"))
                .process(encodeAuthHeader)
                .toD("{{cbs.host}}/v1/paymenttypes")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Mifos API, PaymentTypes: ${body}', " +
                        "'Tracking the payment response', 'Verify the response', null)")
                // Save response as property to use later
                .bean("getPaymentTypesResponse")
                .unmarshal().json()
                .setHeader("paymentType",body());
    }
}
