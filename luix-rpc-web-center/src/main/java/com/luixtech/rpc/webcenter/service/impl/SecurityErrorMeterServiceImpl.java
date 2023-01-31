package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.service.SecurityErrorMeterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Generate the metrics data below:
 * <p>
 * # HELP security_authentication_invalid_tokens_errors_total Indicates validation error count of the tokens presented by the clients.
 * # TYPE security_authentication_invalid_tokens_errors_total counter
 * security_authentication_invalid_tokens_errors_total{application="luix-web-center",cause="malformed",} 3.0
 * security_authentication_invalid_tokens_errors_total{application="luix-web-center",cause="expired",} 0.0
 * security_authentication_invalid_tokens_errors_total{application="luix-web-center",cause="unsupported",} 0.0
 * security_authentication_invalid_tokens_errors_total{application="luix-web-center",cause="invalid-signature",} 0.0
 */
@Service
public class SecurityErrorMeterServiceImpl implements SecurityErrorMeterService {

    protected static final String  INVALID_TOKENS_METER_NAME            = "security_authentication_invalid_tokens";
    private static final   String  INVALID_TOKENS_METER_BASE_UNIT       = "errors";
    private static final   String  INVALID_TOKENS_METER_DESCRIPTION     = "Indicates validation error count of the tokens presented by the clients.";
    private static final   String  INVALID_TOKENS_METER_CAUSE_DIMENSION = "cause";
    private final          Counter tokenInvalidSignatureCounter;
    private final          Counter tokenExpiredCounter;
    private final          Counter tokenUnsupportedCounter;
    private final          Counter tokenMalformedCounter;

    public SecurityErrorMeterServiceImpl(MeterRegistry registry) {
        this.tokenInvalidSignatureCounter = invalidTokensCounterForCauseBuilder("invalid-signature").register(registry);
        this.tokenExpiredCounter = invalidTokensCounterForCauseBuilder("expired").register(registry);
        this.tokenUnsupportedCounter = invalidTokensCounterForCauseBuilder("unsupported").register(registry);
        this.tokenMalformedCounter = invalidTokensCounterForCauseBuilder("malformed").register(registry);
    }

    private Counter.Builder invalidTokensCounterForCauseBuilder(String cause) {
        return Counter
                .builder(INVALID_TOKENS_METER_NAME)
                .baseUnit(INVALID_TOKENS_METER_BASE_UNIT)
                .description(INVALID_TOKENS_METER_DESCRIPTION)
                .tag(INVALID_TOKENS_METER_CAUSE_DIMENSION, cause);
    }

    @Override
    public void trackTokenInvalidSignature() {
        this.tokenInvalidSignatureCounter.increment();
    }

    @Override
    public void trackTokenExpired() {
        this.tokenExpiredCounter.increment();
    }

    @Override
    public void trackTokenUnsupported() {
        this.tokenUnsupportedCounter.increment();
    }

    @Override
    public void trackTokenMalformed() {
        this.tokenMalformedCounter.increment();
    }
}
