package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 渠道配置请求 DTO
 */
public class ChannelConfigRequest {
    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 50, message = "渠道编码最长50字符")
    private String channelCode;

    @Size(max = 100, message = "渠道名称最长100字符")
    private String channelName;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    private boolean enabled;
    private String configJson;

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
