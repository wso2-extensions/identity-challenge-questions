package org.wso2.carbon.identity.challenge.questions.recovery.internal.service.impl.password;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.recovery.*;
import org.wso2.carbon.identity.challenge.questions.recovery.ChallengeQuestionManager;
import org.wso2.carbon.identity.recovery.dto.*;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.recovery.internal.service.impl.UserAccountRecoveryManager;
import org.wso2.carbon.identity.recovery.services.password.PasswordRecoveryManager;
import org.wso2.carbon.identity.recovery.util.Utils;
import org.wso2.carbon.identity.user.functionality.mgt.UserFunctionalityManager;
import org.wso2.carbon.identity.user.functionality.mgt.exception.UserFunctionalityManagementException;
import org.wso2.carbon.identity.user.functionality.mgt.model.FunctionalityLockStatus;

import java.util.Map;

public class ChallengeQuestionPasswordRecoveryManagerImpl implements PasswordRecoveryManager {

    private static final Log log = LogFactory.getLog(ChallengeQuestionPasswordRecoveryManagerImpl.class);
    private static final boolean isSkipRecoveryWithChallengeQuestionsForInsufficientAnswersEnabled =
            Utils.isSkipRecoveryWithChallengeQuestionsForInsufficientAnswersEnabled();
    private static final boolean isPerUserFunctionalityLockingEnabled = Utils.isPerUserFunctionalityLockingEnabled();
    /**
     * @param claims       User Claims 
     * @param tenantDomain Tenant domain
     * @param properties   Meta properties
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public RecoveryInformationDTO initiate(Map<String, String> claims, String tenantDomain, Map<String, String> properties)
            throws IdentityRecoveryException {

        validateTenantDomain(tenantDomain);
        UserAccountRecoveryManager userAccountRecoveryManager = UserAccountRecoveryManager.getInstance();
        boolean isQuestionBasedRecoveryEnabled = isQuestionBasedRecoveryEnabled(tenantDomain);
        RecoveryInformationDTO recoveryInformationDTO = new RecoveryInformationDTO();

        RecoveryChannelInfoDTO recoveryChannelInfoDTO = userAccountRecoveryManager
                .retrieveUserRecoveryInformation(claims, tenantDomain, RecoveryScenarios.QUESTION_BASED_PWD_RECOVERY,
                        properties);
        String username = recoveryChannelInfoDTO.getUsername();
        String recoveryFlowId = recoveryChannelInfoDTO.getRecoveryFlowId();
        recoveryInformationDTO.setUsername(username);
        recoveryInformationDTO.setRecoveryFlowId(recoveryFlowId);
        if (isSkipRecoveryWithChallengeQuestionsForInsufficientAnswersEnabled) {
            recoveryInformationDTO.setQuestionBasedRecoveryAllowedForUser(isQuestionBasedRecoveryEnabled &&
                    isMinNoOfRecoveryQuestionsAnswered(username, tenantDomain));
        } else {
            recoveryInformationDTO.setQuestionBasedRecoveryAllowedForUser(isQuestionBasedRecoveryEnabled);
        }

        if (isPerUserFunctionalityLockingEnabled) {
            boolean isQuestionBasedRecoveryLocked = getFunctionalityStatusOfUser(tenantDomain,
                    recoveryChannelInfoDTO.getUsername(),
                    IdentityRecoveryConstants.FunctionalityTypes.FUNCTIONALITY_SECURITY_QUESTION_PW_RECOVERY
                            .getFunctionalityIdentifier()).getLockStatus();
            recoveryInformationDTO.setQuestionBasedRecoveryEnabled(!isQuestionBasedRecoveryLocked);
        } else {
            recoveryInformationDTO.setQuestionBasedRecoveryEnabled(isQuestionBasedRecoveryEnabled);
        }

        return recoveryInformationDTO;
    }

    /**
     * @param recoveryId   RecoveryId of the user 
     * @param channelId    Channel Id of the user
     * @param tenantDomain Tenant Domain
     * @param properties   Meta properties in the recovery request
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public PasswordRecoverDTO notify(String recoveryId, String channelId, String tenantDomain, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    /**
     * @param confirmationCode Confirmation code 
     * @param tenantDomain     Tenant domain
     * @param properties       Meta properties in the confirmation request
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public PasswordResetCodeDTO confirm(String confirmationCode, String tenantDomain, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    /**
     * @param otp              One Time Password. 
     * @param confirmationCode Confirmation code.
     * @param tenantDomain     Tenant domain.
     * @param properties       Meta properties in the confirmation request.
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public PasswordResetCodeDTO confirm(String otp, String confirmationCode, String tenantDomain, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    /**
     * @param resetCode  Password reset code 
     * @param password   New password
     * @param properties Properties
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public SuccessfulPasswordResetDTO reset(String resetCode, char[] password, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    /**
     * @param resetCode        Password reset code. 
     * @param confirmationCode Confirmation code.
     * @param password         New password.
     * @param properties       Properties.
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public SuccessfulPasswordResetDTO reset(String resetCode, String confirmationCode, char[] password, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    /**
     * @param tenantDomain Tenant Domain 
     * @param resendCode   Resend code
     * @param properties   Meta properties
     * @return
     * @throws IdentityRecoveryException
     */
    @Override
    public ResendConfirmationDTO resend(String tenantDomain, String resendCode, Map<String, String> properties) throws IdentityRecoveryException {
        return null;
    }

    private boolean isQuestionBasedRecoveryEnabled(String tenantDomain) throws IdentityRecoveryServerException {

        // Check whether the challenge question based recovery is enabled.
        try {
            return Boolean.parseBoolean(
                    Utils.getRecoveryConfigs(IdentityRecoveryConstants.ConnectorConfig.QUESTION_BASED_PW_RECOVERY,
                            tenantDomain));
        } catch (IdentityRecoveryServerException e) {
            // Prepend scenario to the thrown exception.
            String errorCode = Utils
                    .prependOperationScenarioToErrorCode(IdentityRecoveryConstants.PASSWORD_RECOVERY_SCENARIO,
                            e.getErrorCode());
            throw Utils.handleServerException(errorCode, e.getMessage(), null);
        }
    }

    /**
     * Checks if user has set answers for at least the minimum number of questions with answers required for password
     * recovery.
     *
     * @param username     The username of the user.
     * @param tenantDomain The tenant domain of the user.
     * @return True if expected number of challenge question answers have been set for the user.
     * @throws IdentityRecoveryException Error while retrieving challenge question Ids for user.
     */
    private boolean isMinNoOfRecoveryQuestionsAnswered(String username, String tenantDomain) throws
            IdentityRecoveryException {

        User user = Utils.buildUser(username, tenantDomain);
        ChallengeQuestionManager challengeQuestionManager = ChallengeQuestionManager.getInstance();
        String[] ids = challengeQuestionManager.getUserChallengeQuestionIds(user);
        boolean isMinNoOfRecoveryQuestionsAnswered = false;

        if (ids != null) {
            int minNoOfQuestionsToAnswer = Integer.parseInt(Utils.getRecoveryConfigs(IdentityRecoveryConstants
                    .ConnectorConfig.QUESTION_MIN_NO_ANSWER, tenantDomain));
            isMinNoOfRecoveryQuestionsAnswered = ids.length >= minNoOfQuestionsToAnswer;
            if (isMinNoOfRecoveryQuestionsAnswered && log.isDebugEnabled()) {
                log.debug(String.format("User: %s in tenant domain %s has set answers for at least the minimum number" +
                        " of questions with answers required for password recovery.", username, tenantDomain));
            }
        }

        return isMinNoOfRecoveryQuestionsAnswered;
    }

    private FunctionalityLockStatus getFunctionalityStatusOfUser(String tenantDomain, String userName,
                                                                 String functionalityIdentifier)
            throws IdentityRecoveryServerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String userId = Utils.getUserId(userName, tenantId);

        UserFunctionalityManager userFunctionalityManager =
                IdentityRecoveryServiceDataHolder.getInstance().getUserFunctionalityManagerService();

        try {
            return userFunctionalityManager.getLockStatus(userId, tenantId, functionalityIdentifier);
        } catch (UserFunctionalityManagementException e) {
            String mappedErrorCode =
                    Utils.prependOperationScenarioToErrorCode(
                            IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_FAILED_TO_GET_LOCK_STATUS_FOR_FUNCTIONALITY
                                    .getCode(), IdentityRecoveryConstants.PASSWORD_RECOVERY_SCENARIO);
            StringBuilder message =
                    new StringBuilder(
                            IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_FAILED_TO_GET_LOCK_STATUS_FOR_FUNCTIONALITY
                                    .getMessage());

            throw Utils.handleServerException(mappedErrorCode, message.toString(), null);
        }
    }

    private void validateTenantDomain(String tenantDomain) throws IdentityRecoveryClientException {

        if (StringUtils.isBlank(tenantDomain)) {
            throw Utils.handleClientException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_RECOVERY_EMPTY_TENANT_DOMAIN.getCode(),
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_RECOVERY_EMPTY_TENANT_DOMAIN
                            .getMessage(), null);
        }
    }
}
