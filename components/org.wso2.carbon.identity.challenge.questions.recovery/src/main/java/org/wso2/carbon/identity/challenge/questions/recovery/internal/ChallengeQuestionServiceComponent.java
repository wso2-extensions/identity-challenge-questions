package org.wso2.carbon.identity.challenge.questions.recovery.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.PostAuthenticationHandler;
import org.wso2.carbon.identity.challenge.questions.recovery.listener.TenantManagementListener;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.challenge.questions.recovery.handler.ChallengeAnswerValidationHandler;
import org.wso2.carbon.identity.challenge.questions.recovery.handler.request.PostAuthnMissingChallengeQuestionsHandler;
import org.wso2.carbon.identity.challenge.questions.recovery.internal.service.impl.password.ChallengeQuestionPasswordRecoveryManagerImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.ChallengeQuestionManager;
import org.wso2.carbon.identity.challenge.questions.recovery.password.SecurityQuestionPasswordRecoveryManager;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.recovery.services.password.PasswordRecoveryManager;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

@Component(
        name = "org.wso2.challenge.question.Component",
        immediate = true
)
public class ChallengeQuestionServiceComponent {

    private static final Log log = LogFactory.getLog(ChallengeQuestionServiceComponent.class);
    private IdentityRecoveryServiceDataHolder dataHolder = IdentityRecoveryServiceDataHolder.getInstance();

    @Activate
    protected void activate(ComponentContext ctxt) {

        try {
            ChallengeQuestionPasswordRecoveryManagerImpl challengeQuestionPasswordRecoveryManager = new ChallengeQuestionPasswordRecoveryManagerImpl();
            ctxt.getBundleContext()
                    .registerService(ChallengeQuestionManager.class.getName(), ChallengeQuestionManager
                            .getInstance(), null);
            ctxt.getBundleContext().registerService(PasswordRecoveryManager.class.getName(),
                    challengeQuestionPasswordRecoveryManager, null);
            ctxt.getBundleContext().registerService(SecurityQuestionPasswordRecoveryManager.class.getName(),
                    SecurityQuestionPasswordRecoveryManager.getInstance(), null);
            // Registering missing challenge question handler as a post authn handler
            PostAuthenticationHandler postAuthnMissingChallengeQuestions = PostAuthnMissingChallengeQuestionsHandler
                    .getInstance();
            ctxt.getBundleContext().registerService(PostAuthenticationHandler.class.getName(),
                    postAuthnMissingChallengeQuestions, null);
            ctxt.getBundleContext().registerService(AbstractEventHandler.class.getName(),
                    new ChallengeAnswerValidationHandler(), null);
            // register the tenant management listener
            TenantMgtListener tenantMgtListener = new TenantManagementListener();
            ctxt.getBundleContext().registerService(TenantMgtListener.class.getName(), tenantMgtListener, null);
            if (log.isDebugEnabled()) {
                log.debug("Challenge questions recovery bundle is activated");
            }
        } catch (Exception e) {
            log.error(" Error while activating Challenge questions recovery bundle ", e);
        }

        // register default challenge questions
        try {
            if (log.isDebugEnabled()) {
                log.debug("Loading default challenge questions for super tenant.");
            }
            loadDefaultChallengeQuestions();
            // new ChallengeQuestionManager().getAllChallengeQuestions("carbon.super", "lk_LK");
        } catch (IdentityRecoveryException e) {
            log.error("Error persisting challenge question for super tenant.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Challenge Question bundle is deactivated");
        }
    }

    private void loadDefaultChallengeQuestions() throws IdentityRecoveryException {

        String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        ChallengeQuestionManager.getInstance().setDefaultChallengeQuestions(tenantDomain);
    }

    protected void unsetResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {

        dataHolder.setResourceMgtService(null);
        if (log.isDebugEnabled()) {
            log.debug("Setting Identity Resource Mgt service.");
        }
    }

    @Reference(
            name = "RegistryResourceMgtService",
            service = org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceMgtService")
    protected void setResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {

        dataHolder.setResourceMgtService(registryResourceMgtService);
        if (log.isDebugEnabled()) {
            log.debug("Unsetting Identity Resource Mgt service.");
        }
    }
}
