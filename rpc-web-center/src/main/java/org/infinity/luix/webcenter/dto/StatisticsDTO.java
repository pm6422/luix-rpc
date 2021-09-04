package org.infinity.luix.webcenter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsDTO {
    private long applicationCount;
    private long activeApplicationCount;

    private long serverCount;
    private long activeServerCount;

    private long serviceCount;
    private long activeServiceCount;

    private long providerCount;
    private long activeProviderCount;

    private long consumerCount;
    private long activeConsumerCount;

    private long taskCount;
    private long activeTaskCount;
    private long taskExecutionCount;
    private long dailyTaskExecutionCount;
    private long taskExecutedCount;
    private long taskFailedCount;
}
