package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import com.modusbox.client.model.LoanDisbursementToMobileWallet;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class GetIdPhoneNumberWalletFspFromBody implements Processor {
    @Override
        public void process(Exchange exchange) throws Exception {
            LoanDisbursementToMobileWallet loanDisbursementToMobileWallet = exchange.getIn().getBody(LoanDisbursementToMobileWallet.class);
            exchange.getIn().setHeader("accountNumber", loanDisbursementToMobileWallet.getChanges().getAccountNumber());
            exchange.getIn().setHeader("bankNumber", loanDisbursementToMobileWallet.getChanges().getBankNumber());
            exchange.getIn().setHeader("loanId", loanDisbursementToMobileWallet.getLoanId());
        }

}
