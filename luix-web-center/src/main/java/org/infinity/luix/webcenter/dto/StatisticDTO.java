package org.infinity.luix.webcenter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticDTO {
    private long applicationCount;
    private long activeApplicationCount;
    private long inactiveApplicationCount;

    private long serverCount;
    private long activeServerCount;
    private long inactiveServerCount;

    private long serviceCount;
    private long activeServiceCount;
    private long inactiveServiceCount;

    private long providerCount;
    private long activeProviderCount;
    private long inactiveProviderCount;

    private long consumerCount;
    private long activeConsumerCount;
    private long inactiveConsumerCount;

    private long taskCount;
    private long activeTaskCount;
    private long taskExecutionCount;
    private long dailyTaskExecutionCount;
    private long taskExecutedCount;
    private long taskFailedCount;
}
