/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.challenge.questions.recovery.endpoint.Utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.MDC;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.captcha.util.CaptchaConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.challenge.questions.recovery.bean.ChallengeQuestionResponse;
import org.wso2.carbon.identity.challenge.questions.recovery.bean.ChallengeQuestionsResponse;
import org.wso2.carbon.identity.recovery.endpoint.Constants;
import org.wso2.carbon.identity.recovery.endpoint.Exceptions.BadRequestException;
import org.wso2.carbon.identity.recovery.endpoint.Exceptions.InternalServerErrorException;
import org.wso2.carbon.identity.recovery.endpoint.dto.ClaimDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.ErrorDTO;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.dto.InitiateAllQuestionResponseDTO;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.dto.InitiateQuestionResponseDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.LinkDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.PropertyDTO;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.dto.QuestionDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.ReCaptchaResponseTokenDTO;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.dto.SecurityAnswerDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.UserClaimDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.UserDTO;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.recovery.model.Property;
import org.wso2.carbon.identity.challenge.questions.recovery.model.UserChallengeAnswer;
import org.wso2.carbon.identity.recovery.model.UserClaim;
import org.wso2.carbon.identity.recovery.password.NotificationPasswordRecoveryManager;
import org.wso2.carbon.identity.challenge.questions.recovery.password.SecurityQuestionPasswordRecoveryManager;
import org.wso2.carbon.identity.recovery.signup.UserSelfRegistrationManager;
import org.wso2.carbon.identity.recovery.username.NotificationUsernameRecoveryManager;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.commons.MiscellaneousUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class Utils {
    private static final String USERNAME_CLAIM = "http://wso2.org/claims/username";
    private static final Log LOG = LogFactory.getLog(Utils.class);

    public static SecurityQuestionPasswordRecoveryManager getSecurityQuestionBasedPwdRecoveryManager() {

        return (SecurityQuestionPasswordRecoveryManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(SecurityQuestionPasswordRecoveryManager.class, null);
    }
    
    public static InitiateQuestionResponseDTO getInitiateQuestionResponseDTO
            (ChallengeQuestionResponse challengeQuestionResponse) {

        InitiateQuestionResponseDTO initiateQuestionResponseDTO = new InitiateQuestionResponseDTO();

        QuestionDTO questionDTO = new QuestionDTO();

        if (challengeQuestionResponse.getQuestion() != null) {
            questionDTO.setQuestion(challengeQuestionResponse.getQuestion().getQuestion());
            questionDTO.setQuestionSetId(challengeQuestionResponse.getQuestion().getQuestionSetId());

            initiateQuestionResponseDTO.setQuestion(questionDTO);
        }

        initiateQuestionResponseDTO.setKey(challengeQuestionResponse.getCode());

        LinkDTO linkDTO = new LinkDTO();

        if (IdentityRecoveryConstants.RECOVERY_STATUS_COMPLETE.equals(challengeQuestionResponse.getStatus())) {
            linkDTO.setRel("set-password");
            linkDTO.setUri("/api/identity/recovery/v0.9");
        } else {
            linkDTO.setRel("validate-answer");
            linkDTO.setUri("/api/identity/recovery/v0.9");
        }

        initiateQuestionResponseDTO.setLink(linkDTO);
        return initiateQuestionResponseDTO;
    }

    public static InitiateAllQuestionResponseDTO getInitiateQuestionResponseDTO
            (ChallengeQuestionsResponse challengeQuestionsResponse) {

        InitiateAllQuestionResponseDTO initiateAllQuestionResponseDTO = new InitiateAllQuestionResponseDTO();

        List<QuestionDTO> questionDTOs = new ArrayList<>();

        for (ChallengeQuestion challengeQuestion : challengeQuestionsResponse.getQuestion()) {
            QuestionDTO questionDTO = new QuestionDTO();
            questionDTO.setQuestion(challengeQuestion.getQuestion());
            questionDTO.setQuestionSetId(challengeQuestion.getQuestionSetId());
            questionDTOs.add(questionDTO);

        }

        initiateAllQuestionResponseDTO.setQuestions(questionDTOs);
        initiateAllQuestionResponseDTO.setKey(challengeQuestionsResponse.getCode());

        LinkDTO linkDTO = new LinkDTO();
        linkDTO.setRel("validate-answer");
        linkDTO.setUri("/api/identity/recovery/v0.9");
        initiateAllQuestionResponseDTO.setLink(linkDTO);
        return initiateAllQuestionResponseDTO;
    }

    public static UserChallengeAnswer[] getUserChallengeAnswers(List<SecurityAnswerDTO> securityAnswerDTOs) {

        UserChallengeAnswer[] userChallengeAnswers = new UserChallengeAnswer[securityAnswerDTOs.size()];

        for (int i = 0; i < securityAnswerDTOs.size(); i++) {
            ChallengeQuestion challengeQuestion = new ChallengeQuestion(securityAnswerDTOs.get(i).getQuestionSetId(),
                    null);
            UserChallengeAnswer userChallengeAnswer = new UserChallengeAnswer(challengeQuestion, securityAnswerDTOs
                    .get(i).getAnswer());
            userChallengeAnswers[i] = userChallengeAnswer;
        }

        return userChallengeAnswers;
    }
}
