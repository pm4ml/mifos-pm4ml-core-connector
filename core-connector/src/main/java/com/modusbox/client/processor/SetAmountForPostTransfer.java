package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import com.modusbox.client.model.TransferRequest;
import com.modusbox.client.model.TransferRequestInbound;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.LinkedHashMap;

public class SetAmountForPostTransfer implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        FulfilNotification fulfilNotification = exchange.getIn().getBody(FulfilNotification.class);
        LinkedHashMap<String,Object> prepareBody  = (LinkedHashMap<String, Object>) fulfilNotification.getPrepare().getBody();
        LinkedHashMap<String,String> amountObjMap = (LinkedHashMap<String,String>) prepareBody.get("amount");
        String amount =amountObjMap.get("amount");
        exchange.getIn().setHeader("amount", amount);
    }
}
