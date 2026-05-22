package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 通知模板请求 DTO
 */
public class TemplateRequest {
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码最长50字符")
    private String templateCode;

    @Size(max = 100, message = "模板名称最长100字符")
    private String templateName;

    private String templateType;

    @Size(max = 200, message = "标题模板最长200字符")
    private String titleTemplate;

    @NotBlank(message = "内容模板不能为空")
    @Size(max = 2000, message = "内容模板最长2000字符")
    private String contentTemplate;

    private String channel;
    private boolean enabled;

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
