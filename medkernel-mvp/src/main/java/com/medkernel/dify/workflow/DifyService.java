package com.medkernel.dify.workflow;

import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class DifyService {

    private final DifyTemplateManager templateManager;
    private final DifyWorkflowExecutor workflowExecutor;
    private final DifyInvocationRecorder invocationRecorder;

    public DifyService(DifyProperties properties, EnginePersistenceService persistenceService) {
        this.invocationRecorder = new DifyInvocationRecorder(persistenceService);
        this.templateManager = new DifyTemplateManager(persistenceService);
        this.workflowExecutor = new DifyWorkflowExecutor(properties, templateManager, invocationRecorder);
    }

    @PostConstruct
    public void rebuildFromPersistence() {
        templateManager.rebuildFromPersistence();
    }

    public List<DifyWorkflowTemplate> importTemplates(Object request) {
        return templateManager.importTemplates(request);
    }

    public List<DifyWorkflowTemplate> listTemplates() {
        return templateManager.listTemplates();
    }

    public DifyWorkflowTemplate getTemplate(String workflowCode, String workflowVersion) {
        return templateManager.getTemplate(workflowCode, workflowVersion);
    }

    public Map<String, Object> runWorkflow(Map<String, Object> request) {
        return workflowExecutor.runWorkflow(request);
    }

    public Map<String, Object> summarizeInvocations(Map<String, String> filters) {
        return invocationRecorder.summarizeInvocations(filters);
    }
}
