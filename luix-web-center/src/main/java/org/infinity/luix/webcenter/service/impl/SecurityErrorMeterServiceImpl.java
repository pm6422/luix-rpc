package org.infinity.luix.webcenter.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.infinity.luix.webcenter.service.SecurityErrorMeterService;
import org.springframework.stereotype.Service;

@Service
public class SecurityErrorMeterServiceImpl implements SecurityErrorMeterService {

    public static final String INVALID_TOKENS_METER_NAME            = "security.authentication.invalid-tokens";
    public static final String INVALID_TOKENS_METER_BASE_UNIT       = "errors";
    public static final String INVALID_TOKENS_METER_DESCRIPTION     = "Indicates validation error count of the tokens presented by the clients.";
    public static final String INVALID_TOKENS_METER_CAUSE_DIMENSION = "cause";

    private final Counter tokenInvalidSignatureCounter;
    private final Counter tokenExpiredCounter;
    private final Counter tokenUnsupportedCounter;
    private final Counter tokenMalformedCounter;

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
