package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetLoanAmountFromBody implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        LinkedHashMap<String,Object>  bodymap = (LinkedHashMap<String, Object>) exchange.getIn().getBody();
        LinkedHashMap<String,Object>summaryMap = (LinkedHashMap<String, Object>) bodymap.get("summary");
        Double principalDisbursed= (Double) summaryMap.get("principalDisbursed");
        exchange.getIn().setHeader("disbursedAmount", principalDisbursed);
    }
}