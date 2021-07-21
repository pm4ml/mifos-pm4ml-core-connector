package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.LinkedHashMap;

public class SetPaymentTypeName implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        FulfilNotification fulfilNotification = exchange.getIn().getBody(FulfilNotification.class);
        LinkedHashMap<String,String> fulfillHeader  = (LinkedHashMap<String, String>) fulfilNotification.getFulfil().getHeaders();
        String paymentTypeName =fulfillHeader.get("fspiop-source");
        exchange.getIn().setHeader("paymentTypeName", paymentTypeName);

    }
}
