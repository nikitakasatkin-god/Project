package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestProcessingSettings {

    @Value("${request.auto.process.enabled:true}")
    private boolean autoProcessEnabled;

    @Value("${request.auto.process.delay.hours:24}")
    private double autoProcessDelayHours;

    @Value("${request.auto.complete.enabled:true}")
    private boolean autoCompleteEnabled;

    @Value("${request.auto.complete.delay.hours:48}")
    private double autoCompleteDelayHours;

    public boolean isAutoProcessEnabled() {
        return autoProcessEnabled;
    }

    public void setAutoProcessEnabled(boolean autoProcessEnabled) {
        this.autoProcessEnabled = autoProcessEnabled;
    }

    public double getAutoProcessDelayHours() {
        return autoProcessDelayHours;
    }

    public void setAutoProcessDelayHours(double autoProcessDelayHours) {
        this.autoProcessDelayHours = autoProcessDelayHours;
    }

    public boolean isAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

    public void setAutoCompleteEnabled(boolean autoCompleteEnabled) {
        this.autoCompleteEnabled = autoCompleteEnabled;
    }

    public double getAutoCompleteDelayHours() {
        return autoCompleteDelayHours;
    }

    public void setAutoCompleteDelayHours(double autoCompleteDelayHours) {
        this.autoCompleteDelayHours = autoCompleteDelayHours;
    }
}