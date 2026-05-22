package com.medkernel.notification;

import javax.validation.Valid;
import java.util.List;

/**
 * 批量订阅设置请求 DTO
 */
public class BatchSubscriptionRequest {
    private List<@Valid SubscriptionRequest> subscriptions;

    public List<SubscriptionRequest> getSubscriptions() { return subscriptions; }
    public void setSubscriptions(List<SubscriptionRequest> subscriptions) { this.subscriptions = subscriptions; }
}
