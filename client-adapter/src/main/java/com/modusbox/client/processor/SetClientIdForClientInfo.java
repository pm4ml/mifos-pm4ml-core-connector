package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetClientIdForClientInfo implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String clientIdStr = (String) exchange.getIn().getBody();
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(clientIdStr);
        if (m.find()) {
            exchange.getIn().setHeader("processedClientId", m.group());
        }

    }
}
