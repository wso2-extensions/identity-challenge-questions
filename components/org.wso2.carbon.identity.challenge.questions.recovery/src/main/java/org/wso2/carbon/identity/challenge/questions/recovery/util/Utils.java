/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.challenge.questions.recovery.util;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.challenge.questions.recovery.internal.ChallengeQuestionDataHolder;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryClientException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.LOCALE_EN_US;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    public static ChallengeQuestion[] getDefaultChallengeQuestions() {

        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        // locale en_US, challengeSet1
        int count = 0;
        for (String question : IdentityRecoveryConstants.Questions.SECRET_QUESTIONS_SET01) {
            String setId = IdentityRecoveryConstants.WSO2CARBON_CLAIM_DIALECT + "/" + "challengeQuestion1";
            String questionId = "question" + (++count);
            challengeQuestions.add(
                    new ChallengeQuestion(setId, questionId, question, IdentityRecoveryConstants.LOCALE_EN_US));
        }

        count = 0;
        for (String question : IdentityRecoveryConstants.Questions.SECRET_QUESTIONS_SET02) {
            String setId = IdentityRecoveryConstants.WSO2CARBON_CLAIM_DIALECT + "/" + "challengeQuestion2";
            String questionId = "question" + (++count);
            challengeQuestions.add(
                    new ChallengeQuestion(setId, questionId, question, IdentityRecoveryConstants.LOCALE_EN_US));
        }

        return challengeQuestions.toArray(new ChallengeQuestion[challengeQuestions.size()]);
    }

    public static IdentityRecoveryServerException handleServerException(IdentityRecoveryConstants.ErrorMessages
                                                                                error, String data, Throwable e)
            throws IdentityRecoveryServerException {

        String errorDescription;
        if (StringUtils.isNotBlank(data)) {
            errorDescription = String.format(error.getMessage(), data);
        } else {
            errorDescription = error.getMessage();
        }

        return IdentityException.error(
                IdentityRecoveryServerException.class, error.getCode(), errorDescription, e);
    }

    public static String getClaimFromUserStoreManager(User user, String claim)
            throws UserStoreException {

        String userStoreQualifiedUsername = IdentityUtil.addDomainToName(user.getUserName(), user.getUserStoreDomain());
        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        RealmService realmService = ChallengeQuestionDataHolder.getInstance().getRealmService();
        String claimValue = "";

        int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());
        if (realmService.getTenantUserRealm(tenantId) != null) {
            userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                    getUserStoreManager();
        }

        if (userStoreManager != null) {
            Map<String, String> claimsMap = userStoreManager
                    .getUserClaimValues(userStoreQualifiedUsername, new String[]{claim},
                            UserCoreConstants.DEFAULT_PROFILE);
            if (claimsMap != null && !claimsMap.isEmpty()) {
                claimValue = claimsMap.get(claim);
            }
        }
        return claimValue;
    }

    /**
     * Get the claim values for given claim list of user.
     *
     * @param user       User.
     * @param claimsList Claims list to retrieve.
     * @return Map of claims list with corresponding values.
     * @throws IdentityRecoveryClientException If an invalid input is detected.
     * @throws IdentityRecoveryServerException If an error occurred while retrieving claims.
     */
    public static Map<String, String> getClaimListOfUser(User user, String[] claimsList)
            throws IdentityRecoveryClientException, IdentityRecoveryServerException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = getUserStoreManager(user);
        String userStoreQualifiedUsername = IdentityUtil.addDomainToName(user.getUserName(), user.getUserStoreDomain());
        if (ArrayUtils.isEmpty(claimsList)) {
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_FAILED_TO_LOAD_USER_CLAIMS,
                    null);
        }
        try {
            return userStoreManager
                    .getUserClaimValues(userStoreQualifiedUsername, claimsList, UserCoreConstants.DEFAULT_PROFILE);
        } catch (UserStoreException e) {
            throw handleServerException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_FAILED_TO_LOAD_USER_CLAIMS,
                    user.getUserName(), e);
        }
    }

    /**
     * @param value
     * @return
     * @throws UserStoreException
     */
    @Deprecated
    public static String doHash(String value) throws UserStoreException {

        try {
            return hashCode(value);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    /**
     * @param value     Value to be hashed
     * @return          Hashed value
     * @throws NoSuchAlgorithmException If the algorithm is not found.
     */
    public static String hashCode(String value) throws NoSuchAlgorithmException {

        String digsestFunction = "SHA-256";
        MessageDigest dgst = MessageDigest.getInstance(digsestFunction);
        byte[] byteValue = dgst.digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encode(byteValue);
    }

    /**
     * Set user claims list.
     *
     * @param user      User.
     * @param claimsMap Map of claims list to update.
     * @throws IdentityRecoveryClientException If an invalid input is detected.
     * @throws IdentityRecoveryServerException If an error occurred while updating user claims.
     */
    public static void setClaimsListOfUser(User user, Map<String, String> claimsMap)
            throws IdentityRecoveryClientException, IdentityRecoveryServerException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = getUserStoreManager(user);
        String userStoreQualifiedUsername = IdentityUtil.addDomainToName(user.getUserName(),
                user.getUserStoreDomain());
        try {
            if (MapUtils.isNotEmpty(claimsMap)) {
                userStoreManager.setUserClaimValues(userStoreQualifiedUsername, claimsMap,
                        UserCoreConstants.DEFAULT_PROFILE);
            }
        } catch (UserStoreException e) {
            throw handleServerException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_FAILED_TO_UPDATE_USER_CLAIMS,
                    null, e);
        }
    }

    /**
     * Set claim to user store manager
     *
     * @param user  user
     * @param claim claim uri
     * @param value claim value
     * @throws IdentityException if fails
     */
    public static void setClaimInUserStoreManager(User user, String claim, String value) throws UserStoreException {

        String fullUserName = IdentityUtil.addDomainToName(user.getUserName(), user.getUserStoreDomain());
        int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        RealmService realmService = ChallengeQuestionDataHolder.getInstance().getRealmService();
        if (realmService.getTenantUserRealm(tenantId) != null) {
            userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                    getUserStoreManager();
        }

        if (userStoreManager != null) {
            Map<String, String> values = userStoreManager.getUserClaimValues(fullUserName, new String[]{
                    claim}, UserCoreConstants.DEFAULT_PROFILE);
            String oldValue = values.get(claim);
            if (oldValue == null || !oldValue.equals(value)) {
                Map<String, String> claimMap = new HashMap<String, String>();
                claimMap.put(claim, value);
                userStoreManager.setUserClaimValues(fullUserName, claimMap, UserCoreConstants.DEFAULT_PROFILE);
            }
        }
    }

    public static void removeClaimFromUserStoreManager(User user, String[] claims)
            throws UserStoreException {

        String userStoreQualifiedUsername = IdentityUtil.addDomainToName(user.getUserName(), user.getUserStoreDomain());
        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        RealmService realmService = ChallengeQuestionDataHolder.getInstance().getRealmService();

        int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());
        if (realmService.getTenantUserRealm(tenantId) != null) {
            userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                    getUserStoreManager();
        }

        if (userStoreManager != null) {
            userStoreManager
                    .deleteUserClaimValues(userStoreQualifiedUsername, claims, UserCoreConstants.DEFAULT_PROFILE);
        }
    }

    // challenge question related Util
    public static String getChallengeSetDirFromUri(String challengeSetUri) {

        if (StringUtils.isBlank(challengeSetUri)) {
            return challengeSetUri;
        }

        String[] components = challengeSetUri.split(IdentityRecoveryConstants.WSO2CARBON_CLAIM_DIALECT + "/");
        return components.length > 1 ? components[1] : components[0];
    }

    public static IdentityRecoveryClientException handleClientException(IdentityRecoveryConstants.ErrorMessages
                                                                                error, String data)
            throws IdentityRecoveryClientException {

        String errorDescription;
        if (StringUtils.isNotBlank(data)) {
            errorDescription = String.format(error.getMessage(), data);
        } else {
            errorDescription = error.getMessage();
        }

        return IdentityException.error(IdentityRecoveryClientException.class, error.getCode(), errorDescription);
    }

    private static org.wso2.carbon.user.core.UserStoreManager getUserStoreManager(User user)
            throws IdentityRecoveryClientException, IdentityRecoveryServerException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager;

        // Validate method inputs.
        if (user == null) {
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_USER,
                    "Invalid User Data provided.");
        }

        int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());
        try {
            RealmService realmService = ChallengeQuestionDataHolder.getInstance().getRealmService();
            if (realmService == null || realmService.getTenantUserRealm(tenantId) == null) {
                throw handleServerException(IdentityRecoveryConstants.ErrorMessages.
                        ERROR_CODE_FAILED_TO_LOAD_REALM_SERVICE, user.getTenantDomain());
            }
            userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                    getUserStoreManager();
        } catch (UserStoreException e) {
            throw handleServerException(IdentityRecoveryConstants.ErrorMessages.
                    ERROR_CODE_FAILED_TO_LOAD_REALM_SERVICE, user.getTenantDomain(), e);
        }

        if (userStoreManager == null) {
            throw handleServerException(IdentityRecoveryConstants.ErrorMessages.
                    ERROR_CODE_FAILED_TO_LOAD_USER_STORE_MANAGER, null);
        }
        return userStoreManager;
    }

    public static IdentityRecoveryServerException handleServerException(IdentityRecoveryConstants.ErrorMessages
                                                                                error, String data)
            throws IdentityRecoveryServerException {

        String errorDescription;
        if (StringUtils.isNotBlank(data)) {
            errorDescription = String.format(error.getMessage(), data);
        } else {
            errorDescription = error.getMessage();
        }

        return IdentityException.error(
                IdentityRecoveryServerException.class, error.getCode(), errorDescription);
    }

    /**
     * Handle server exception.
     *
     * @param error Error message.
     * @param data  Data to be formatted in the error message.
     * @param e     Exception.
     * @return IdentityRecoveryServerException.
     * @throws IdentityRecoveryServerException If an error occurred while handling the server exception.
     */
    public static IdentityRecoveryServerException handleServerException(ChallengeQuestionsConstants.ErrorMessages error,
                                                                        String data, Throwable e)
            throws IdentityRecoveryServerException {

        String errorDescription;
        if (StringUtils.isNotBlank(data)) {
            errorDescription = String.format(error.getMessage(), data);
        } else {
            errorDescription = error.getMessage();
        }

        return IdentityException.error(IdentityRecoveryServerException.class, error.getCode(), errorDescription, e);
    }

    /**
     * Validate the locale.
     *
     * @param locale Locale  to be validated.
     * @return Validated locale string.
     * @throws IdentityRecoveryClientException If the locale string is invalid.
     */
    public static String validateLocale(String locale) throws IdentityRecoveryClientException {

        if (StringUtils.isBlank(locale)) {
            locale = LOCALE_EN_US;
        }
        if (locale.matches(IdentityRecoveryConstants.Questions.BLACKLIST_REGEX)) {
            log.error("Invalid locale value provided : " + locale);
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_LOCALE, locale);
        }
        return locale;

    }

    /**
     * Validate the challenge question attributes.
     *
     * @param question Challenge question to be validated.
     * @throws IdentityRecoveryClientException If the challenge question is invalid.
     */
    public static void validateChallengeQuestionAttributes(ChallengeQuestion question)
            throws IdentityRecoveryClientException {

        String setId = question.getQuestionSetId();
        String questionId = question.getQuestionId();
        String questionText = question.getQuestion();
        String questionLocale = question.getLocale();

        if (StringUtils.isBlank(setId) || StringUtils.isBlank(questionId) || StringUtils.isBlank(questionText) ||
                StringUtils.isBlank(questionLocale)) {
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CHALLENGE, null);
        }
        validateChallengePathParams(setId, questionId);
    }

    /**
     * Validate the mandatory parameters of the challenge question.
     *
     * @param question Challenge question to be validated.
     * @throws IdentityRecoveryClientException If the challenge question is invalid.
     */
    public static void validateChallengeQuestionMandatoryParams(ChallengeQuestion question)
            throws IdentityRecoveryClientException {

        String setId = question.getQuestionSetId();
        String questionId = question.getQuestionId();

        if (StringUtils.isBlank(setId) || StringUtils.isBlank(questionId)) {
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CHALLENGE, null);
        }
        validateChallengePathParams(setId, questionId);
    }

    /**
     * Validate the challenge question path parameters.
     *
     * @param setId      Challenge question set ID to be validated.
     * @param questionId Challenge question ID to be validated.
     * @throws IdentityRecoveryClientException If the challenge question path parameters are invalid.
     */
    public static void validateChallengePathParams(String setId, String questionId)
            throws IdentityRecoveryClientException {

        validateChallengeSetURI(setId);
        validateChallengePathParam(questionId, "QuestionId");
    }

    /**
     * Validate the challenge question set URI.
     *
     * @param setId Challenge question set ID to be validated.
     * @throws IdentityRecoveryClientException If the challenge question set URI is invalid.
     */
    public static void validateChallengeSetURI(String setId) throws IdentityRecoveryClientException {

        String challengeSetDir = getChallengeSetDirFromUri(setId);
        validateChallengePathParam(challengeSetDir, "ChallengeSetId");
    }

    /**
     * Validate the challenge question path parameter.
     *
     * @param pathParam     Challenge question path parameter to be validated.
     * @param pathParamName Name of the challenge question path parameter.
     * @throws IdentityRecoveryClientException If the challenge question path parameter is invalid.
     */
    public static void validateChallengePathParam(String pathParam, String pathParamName)
            throws IdentityRecoveryClientException {

        if (StringUtils.isBlank(pathParam) || !StringUtils.isAlphanumeric(pathParam)) {
            throw handleClientException(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CHALLENGE_PATH,
                    pathParamName);
        }
    }
}
