/*
 * Copyright (c) 2016, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.challenge.questions.recovery;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionDAO;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionPersistenceManagerFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.IdentityEventServerException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.recovery.IdentityRecoveryClientException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.challenge.questions.recovery.model.UserChallengeAnswer;
import org.wso2.carbon.identity.recovery.util.Utils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;

import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateLocale;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ErrorMessages.*;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_CHALLENGE_QUESTION_VALUE;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.LOCALE_EN_US;

/**
 * This class is used to manage challenge questions.
 */
public class ChallengeQuestionManager {

    private static final Log log = LogFactory.getLog(ChallengeQuestionManager.class);
    private static ChallengeQuestionManager instance = new ChallengeQuestionManager();
    ChallengeQuestionPersistenceManagerFactory challengeQuestionPersistenceManagerFactory =
            new ChallengeQuestionPersistenceManagerFactory();
    ChallengeQuestionDAO challengeQuestionsImpl =
            challengeQuestionPersistenceManagerFactory.getChallengeQuestionPersistenceManager();

    private ChallengeQuestionManager() {

    }

    public static ChallengeQuestionManager getInstance() {
        return instance;
    }

    /**
     * Get all challenge questions registered for a tenant.
     *
     * @param tenantDomain
     * @return
     * @throws IdentityRecoveryServerException
     */
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain) throws IdentityRecoveryServerException {

        tenantDomain = validateTenantDomain(tenantDomain);

        return challengeQuestionsImpl.getAllChallengeQuestions(tenantDomain);

    }

    /**
     * Get registered challenge questions in tenant based on a locale.
     *
     * @param tenantDomain
     * @param locale
     * @return
     * @throws IdentityRecoveryException
     */
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain, String locale)
            throws IdentityRecoveryException {
        // check the value and set defaults if empty or null
        locale = validateLocale(locale);
        tenantDomain = validateTenantDomain(tenantDomain);

        return challengeQuestionsImpl.getAllChallengeQuestions(tenantDomain, locale);
    }

    /**
     * Get all challenge questions set URIs registered for a tenant.
     *
     * @param tenantDomain
     * @return
     * @throws IdentityRecoveryServerException
     */
    public List<String> getAllChallengeQuestionSetsURIs(String tenantDomain) throws
            IdentityRecoveryServerException {

        tenantDomain = validateTenantDomain(tenantDomain);
        return challengeQuestionsImpl.getAllChallengeQuestionSetsURIs(tenantDomain);
    }

    /**
     * Get challenge questions available for a user.
     *
     * @param tenantDomain tenantDomain of the user
     * @param user         User object
     * @return List of available challenge questions in user's locale in the tenantDomain. If no challenge questions
     * are available we return challenge questions from the default en_US locale.
     * @throws IdentityRecoveryException
     */
    public List<ChallengeQuestion> getAllChallengeQuestionsForUser(String tenantDomain,
                                                                   User user) throws IdentityRecoveryException {

        // Identify the locale of the user
        String locale = getLocaleOfUser(user, tenantDomain);
        // get challenge questions in the given tenant domain for give locale.
        List<ChallengeQuestion> challengeQuestions = getAllChallengeQuestions(tenantDomain, locale);

        /*
            If there are no challenge questions found in the locale of the user and the locale is not the default one.
             we return challenge questions from default en_US locale.
         */
        if (challengeQuestions.isEmpty() && !StringUtils.equalsIgnoreCase(LOCALE_EN_US, locale)) {
            String error = "No challenge questions available in '%s' locale in %s tenant. Sending questions of " +
                    "default '%s' locale";
            log.error(String.format(error, locale, tenantDomain, LOCALE_EN_US));
            challengeQuestions = getAllChallengeQuestions(tenantDomain, LOCALE_EN_US);
        }

        return challengeQuestions;
    }


    /**
     * Set default challenge questions to a tenant registry. (This is done during startup)
     *
     * @param tenantDomain
     * @throws IdentityRecoveryException
     */
    public void setDefaultChallengeQuestions(String tenantDomain) throws IdentityRecoveryException {

        tenantDomain = validateTenantDomain(tenantDomain);

        // check whether we already have default questions.
        boolean isDefaultAvailable = !getAllChallengeQuestions(tenantDomain).isEmpty();
        if (isDefaultAvailable) {
            if (log.isDebugEnabled()) {
                log.debug("Default Challenge Questions already available.");
            }
            return;
        }

        ChallengeQuestion[] questions = ChallengeQuesionsUtil.getDefaultChallengeQuestions();
        addChallengeQuestions(questions, tenantDomain);

        if (log.isDebugEnabled()) {
            String errorMsg = "%d default challenge questions added to registry of %s tenant.";
            log.debug(String.format(errorMsg, questions.length, tenantDomain));
        }
    }

    /**
     * Add new challenge questions to the registry of a tenant
     *
     * @param questions
     * @param tenantDomain
     * @throws IdentityRecoveryException
     */
    public void addChallengeQuestions(ChallengeQuestion[] questions, String tenantDomain) throws IdentityRecoveryException {

        tenantDomain = validateTenantDomain(tenantDomain);
        challengeQuestionsImpl.addChallengeQuestions(questions, tenantDomain);
    }

    /**
     * Delete challenge questions from a tenant registry.
     *
     * @param challengeQuestions
     * @param tenantDomain
     * @throws IdentityRecoveryException
     */
    public void deleteChallengeQuestions(ChallengeQuestion[] challengeQuestions, String tenantDomain)
            throws IdentityRecoveryException {

        tenantDomain = validateTenantDomain(tenantDomain);
        challengeQuestionsImpl.deleteChallengeQuestions(challengeQuestions, tenantDomain);
    }

    /**
     * Delete challenge question set from a tenant registry.
     *
     * @param challengeQuestionUri
     * @param locale
     * @param tenantDomain
     * @throws IdentityRecoveryException
     */
    public void deleteChallengeQuestionSet(String challengeQuestionUri, String locale, String tenantDomain)
            throws IdentityRecoveryException {

        tenantDomain = validateTenantDomain(tenantDomain);
        challengeQuestionsImpl.deleteChallengeQuestionSet(challengeQuestionUri, locale, tenantDomain);
    }

    /**
     * Get challenge questions answered by a user.
     *
     * @param user
     * @return
     */
    public UserChallengeAnswer[] getChallengeAnswersOfUser(User user) throws IdentityRecoveryException {

        validateUser(user);

        List<UserChallengeAnswer> userChallengeAnswers = new ArrayList<>();
        if (log.isDebugEnabled()) {
            log.debug("Retrieving Challenge question from the user profile.");
        }

        List<String> challengesUris = getChallengeQuestionUris(user);
        for (String challengesUri1 : challengesUris) {
            String challengesUri = challengesUri1.trim();
            String challengeValue;
            try {
                challengeValue = Utils.getClaimFromUserStoreManager(user, challengesUri);
            } catch (UserStoreException e) {
                throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                        .ERROR_CODE_GETTING_CHALLENGE_QUESTIONS, user.getUserName(), e);
            }

            String challengeQuestionSeparator = getChallengeSeparator();

            String[] challengeValues = challengeValue.split(challengeQuestionSeparator);
            if (challengeValues != null && challengeValues.length == 2) {
                ChallengeQuestion userChallengeQuestion = new ChallengeQuestion(challengesUri,
                        challengeValues[0].trim());
                UserChallengeAnswer userChallengeAnswer = new UserChallengeAnswer(userChallengeQuestion,
                        challengeValues[1].trim());
                userChallengeAnswers.add(userChallengeAnswer);
            }
        }

        if (!userChallengeAnswers.isEmpty()) {
            return userChallengeAnswers.toArray(new UserChallengeAnswer[userChallengeAnswers.size()]);
        } else {
            return new UserChallengeAnswer[0];
        }
    }

    /**
     * Retrieve the challenge question answered from a particular challenge question set.
     *
     * @param user
     * @param challengesUri claim uri of the challenge set
     * @return
     * @throws IdentityRecoveryException
     */
    public ChallengeQuestion getUserChallengeQuestion(User user, String challengesUri) throws IdentityRecoveryException {

        validateUser(user);

        ChallengeQuestion userChallengeQuestion = null;
        if (log.isDebugEnabled()) {
            log.debug("Retrieving Challenge question from the user profile.");
        }

        String challengeValue = null;
        try {
            challengeValue = Utils.getClaimFromUserStoreManager(user, challengesUri);
        } catch (UserStoreException e) {
            throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                    .ERROR_CODE_GETTING_CHALLENGE_QUESTION, user.getUserName(), e);
        }

        if (challengeValue != null) {

            String challengeQuestionSeparator = getChallengeSeparator();

            String[] challengeValues = challengeValue.split(challengeQuestionSeparator);
            if (challengeValues != null && challengeValues.length == 2) {
                userChallengeQuestion = new ChallengeQuestion(challengesUri, challengeValues[0].trim());
            }
        }
        return userChallengeQuestion;

    }

    public String[] getUserChallengeQuestionIds(User user)
            throws IdentityRecoveryException {

        validateUser(user);

        if (log.isDebugEnabled()) {
            log.debug("Retrieving answered Challenge question set ids from the user profile.");
        }
        List<String> challengesUris = getChallengeQuestionUris(user);

        if (challengesUris.isEmpty()) {
            String msg = "No associated challenge question found for the user : " + user.getUserName();
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            return new String[0];
        }
        String[] urls = new String[challengesUris.size()];
        return challengesUris.toArray(urls);

    }

    /**
     * Get the claims URIs of the challenge sets answered by the user
     *
     * @param user
     * @return
     */
    public List<String> getChallengeQuestionUris(User user) throws IdentityRecoveryException {

        validateUser(user);

        if (log.isDebugEnabled()) {
            String msg = String.format("Getting answered challenge question uris from %s's profile.", user.toString());
            log.debug(msg);
        }

        List<String> challenges = new ArrayList<String>();
        String claimValue = null;
        String[] challengesUris;

        try {
            claimValue = Utils.getClaimFromUserStoreManager(user, IdentityRecoveryConstants.CHALLENGE_QUESTION_URI);
        } catch (UserStoreException e) {
            throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                    .ERROR_CODE_GETTING_CHALLENGE_URIS, user.getUserName(), e);
        }

        if (claimValue != null) {

            String challengeQuestionSeparator = getChallengeSeparator();

            if (claimValue.contains(challengeQuestionSeparator)) {
                challengesUris = claimValue.split(challengeQuestionSeparator);
            } else {
                challengesUris = new String[]{claimValue.trim()};
            }

            for (String challengesUri : challengesUris) {
                if (StringUtils.isNotBlank(challengesUri)) {
                    challenges.add(challengesUri.trim());
                }
            }
        }

        return challenges;
    }

    /**
     * Get the existing answers for the challenge questions.
     *
     * @param user                 User
     * @param userChallengeAnswers List of UserChallengeAnswer objects
     * @return Existing challenge questions and answers.
     */
    private Map<String, String> retrieveAnsweredChallenges(User user, UserChallengeAnswer[] userChallengeAnswers)
            throws IdentityRecoveryException {

        Map<String, String> existingQuestionAndAnswers = new HashMap<>();
        if (ArrayUtils.isNotEmpty(userChallengeAnswers)) {
            List<String> claimsList = new ArrayList<>();
            for (UserChallengeAnswer answer : userChallengeAnswers) {
                if (answer != null && answer.getQuestion() != null
                        && StringUtils.isNotBlank(answer.getQuestion().getQuestionSetId())) {
                    claimsList.add(answer.getQuestion().getQuestionSetId().trim());
                }
            }
            existingQuestionAndAnswers = Utils.getClaimListOfUser(user, claimsList.toArray(new String[0]));
        }
        if (log.isDebugEnabled()) {
            if (MapUtils.isEmpty(existingQuestionAndAnswers)) {
                log.debug("No previous questions set for the user: " + user.getUserName());
            }
        }
        return existingQuestionAndAnswers;
    }

    /**
     * @param user
     * @param userChallengeAnswers
     * @throws IdentityException
     */
    public void setChallengesOfUser(User user, UserChallengeAnswer[] userChallengeAnswers) throws IdentityRecoveryException {

        validateUser(user);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting user challenge question answers in %s's profile.", user.toString()));
        }

        try {
            String tenantDomain = StringUtils.isBlank(user.getTenantDomain()) ?
                    MultitenantConstants.SUPER_TENANT_DOMAIN_NAME : user.getTenantDomain();

            // validate whether two questions from the same set has been answered.
            validateSecurityQuestionDuplicate(userChallengeAnswers);

            // check whether the answered questions exist in the tenant domain
            checkChallengeQuestionExists(userChallengeAnswers, tenantDomain);

            // Get the existing challenge questions and answers for the user.
            Map<String, String> existingQuestionAndAnswers = retrieveAnsweredChallenges(user, userChallengeAnswers);

            triggerChallengeAnswersValidation(user, userChallengeAnswers,
                    existingQuestionAndAnswers, IdentityEventConstants.Event.PRE_SET_CHALLENGE_QUESTION_ANSWERS);

            List<String> challengesUris = new ArrayList<String>();
            String challengesUrisValue = "";
            String separator = getChallengeSeparator();

            Map<String, String> challengeQuestionToUpdate = new HashMap<>();

            if (!ArrayUtils.isEmpty(userChallengeAnswers)) {
                for (UserChallengeAnswer userChallengeAnswer : userChallengeAnswers) {

                    if (StringUtils.isNotBlank(userChallengeAnswer.getQuestion().getQuestionSetId()) &&
                            StringUtils.isNotBlank(userChallengeAnswer.getQuestion().getQuestion()) &&
                            StringUtils.isNotBlank(userChallengeAnswer.getAnswer())) {

                        // Get the previous answer for the question.
                        String oldValue = existingQuestionAndAnswers
                                .get(userChallengeAnswer.getQuestion().getQuestionSetId().trim());

                        if (oldValue != null && oldValue.contains(separator)) {
                            String oldAnswer = oldValue.split(separator)[1];
                            if (!oldAnswer.trim().equals(userChallengeAnswer.getAnswer().trim())) {
                                String claimValue = userChallengeAnswer.getQuestion().getQuestion().trim() + separator +
                                        Utils.doHash(userChallengeAnswer.getAnswer().trim().toLowerCase());
                                challengeQuestionToUpdate
                                        .put(userChallengeAnswer.getQuestion().getQuestionSetId().trim(), claimValue);
                            }
                        } else {
                            String claimValue = userChallengeAnswer.getQuestion().getQuestion().trim() + separator +
                                    Utils.doHash(userChallengeAnswer.getAnswer().trim().toLowerCase());
                            challengeQuestionToUpdate
                                    .put(userChallengeAnswer.getQuestion().getQuestionSetId().trim(), claimValue);
                        }
                        challengesUris.add(userChallengeAnswer.getQuestion().getQuestionSetId().trim());
                    }
                }

                for (String challengesUri : challengesUris) {
                    if ("".equals(challengesUrisValue)) {
                        challengesUrisValue = challengesUri;
                    } else {
                        challengesUrisValue = challengesUrisValue +
                                separator + challengesUri;
                    }
                }
                challengeQuestionToUpdate.put(IdentityRecoveryConstants.CHALLENGE_QUESTION_URI, challengesUrisValue);
                if (MapUtils.isNotEmpty(challengeQuestionToUpdate)) {
                    Utils.setClaimsListOfUser(user, challengeQuestionToUpdate);
                }
                triggerChallengeAnswersValidation(user, userChallengeAnswers,
                        existingQuestionAndAnswers, IdentityEventConstants.Event.POST_SET_CHALLENGE_QUESTION_ANSWERS);
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw Utils.handleServerException(
                    ERROR_CODE_REMOVING_CHALLENGE_QUESTIONS, user.getUserName(), e);
        }
    }

    /**
     * Trigger challenge question answers validation according to the given event name.
     *
     * @param user                 User
     * @param userChallengeAnswers Array of challenge answers
     * @param eventName            Event name
     * @throws IdentityRecoveryClientException Error while validating the challenge answers
     * @throws IdentityRecoveryServerException Error while getting the user store manager or triggering the event.
     */
    private void triggerChallengeAnswersValidation(User user, UserChallengeAnswer[] userChallengeAnswers,
                                                   Map<String, String> existingQuestionAndAnswers, String eventName)
            throws IdentityRecoveryClientException, IdentityRecoveryServerException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(IdentityEventConstants.EventProperty.USER, user);
        properties.put(IdentityEventConstants.EventProperty.USER_CHALLENGE_ANSWERS, userChallengeAnswers);
        properties.put(IdentityEventConstants.EventProperty.USER_OLD_CHALLENGE_ANSWERS, existingQuestionAndAnswers);

        try {
            UserStoreManager userStoreManager;
            if (IdentityUtil.getPrimaryDomainName().equals(user.getUserStoreDomain())) {
                userStoreManager = (UserStoreManager) CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                        .getUserStoreManager();
            } else {
                userStoreManager = ((UserStoreManager) CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                        .getUserStoreManager()).getSecondaryUserStoreManager(user.getUserStoreDomain());
            }
            properties.put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        } catch (UserStoreException e) {
            throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages.
                    ERROR_CODE_FAILED_TO_LOAD_USER_STORE_MANAGER, null, e);
        }

        Event identityMgtEvent = new Event(eventName, properties);
        try {
            IdentityRecoveryServiceDataHolder.getInstance().getIdentityEventService().handleEvent(identityMgtEvent);
        } catch (IdentityEventClientException e) {
            throw new IdentityRecoveryClientException(e.getErrorCode(), e.getMessage(), e);
        } catch (IdentityEventServerException e) {
            throw new IdentityRecoveryServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (IdentityEventException e) {
            throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages.
                    ERROR_CODE_PUBLISH_EVENT, eventName, e);
        }
    }

    private String getChallengeSeparator() {
        String separator = IdentityUtil.getProperty(IdentityRecoveryConstants.ConnectorConfig
                .QUESTION_CHALLENGE_SEPARATOR);

        if (StringUtils.isEmpty(separator)) {
            separator = IdentityRecoveryConstants.DEFAULT_CHALLENGE_QUESTION_SEPARATOR;
        }
        return separator;
    }

    /**
     * @param user
     * @param userChallengeAnswer
     * @throws IdentityException
     */
    public void setChallengeOfUser(User user, UserChallengeAnswer userChallengeAnswer) throws IdentityRecoveryException {

        validateUser(user);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting user challenge question answers in %s's profile.", user.toString()));
        }

        try {
            String tenantDomain = StringUtils.isBlank(user.getTenantDomain()) ?
                    MultitenantConstants.SUPER_TENANT_DOMAIN_NAME : user.getTenantDomain();

            // validate whether two questions from the same set has been answered.
            validateSecurityQuestionDuplicate(new UserChallengeAnswer[]{userChallengeAnswer});

            // check whether the answered questions exist in the tenant domain
            checkChallengeQuestionExists(new UserChallengeAnswer[]{userChallengeAnswer}, tenantDomain);

            Set<String> challengesUris = new HashSet<>(getChallengeQuestionUris(user));
            String separator = getChallengeSeparator();

            if (userChallengeAnswer.getQuestion().getQuestionSetId() != null && userChallengeAnswer.getQuestion().getQuestion() !=
                    null && userChallengeAnswer.getAnswer() != null) {
                String claimValue = userChallengeAnswer.getQuestion().getQuestion().trim() + separator +
                        Utils.doHash(userChallengeAnswer.getAnswer().trim().toLowerCase());
                Utils.setClaimInUserStoreManager(user, userChallengeAnswer.getQuestion().getQuestionSetId().trim(),
                        claimValue);
                challengesUris.add(userChallengeAnswer.getQuestion().getQuestionSetId().trim());

                setUserChallengesURI(user, challengesUris, separator);
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw Utils.handleServerException(
                    ERROR_CODE_REMOVING_CHALLENGE_QUESTIONS, user.getUserName(), e);
        }
    }

    private void setUserChallengesURI(User user, Set<String> challengesUris, String separator) throws UserStoreException {
        String challengesUrisValue = getUserChallengesUriValue(challengesUris, separator);
        Utils.setClaimInUserStoreManager(user, IdentityRecoveryConstants.CHALLENGE_QUESTION_URI, challengesUrisValue);
    }

    private void setUserChallengesURI(User user, Set<String> challengesUris) throws UserStoreException {
        setUserChallengesURI(user, challengesUris, getChallengeSeparator());
    }

    private String getUserChallengesUriValue(Set<String> challengesUris, String separator) {
        String challengesUrisValue = StringUtils.EMPTY;
        for (String challengesUri : challengesUris) {
            if (StringUtils.EMPTY.equals(challengesUrisValue)) {
                challengesUrisValue = challengesUri;
            } else {
                challengesUrisValue = challengesUrisValue +
                        separator + challengesUri;
            }
        }
        return challengesUrisValue;
    }

    /**
     * @param user
     * @throws IdentityException
     */
    public void removeChallengeAnswersOfUser(User user) throws
            IdentityRecoveryException {

        validateUser(user);
        if (log.isDebugEnabled()) {
            log.debug("Removing Challenge question answers from the user profile.");
        }

        List<String> challengesUris = getChallengeQuestionUris(user);
        challengesUris.add(IdentityRecoveryConstants.CHALLENGE_QUESTION_URI);
        try {
            Utils.removeClaimFromUserStoreManager(user, challengesUris.toArray(new String[challengesUris.size()]));
        } catch (UserStoreException e) {
            throw Utils.handleServerException(
                    ERROR_CODE_REMOVING_CHALLENGE_QUESTIONS, user.getUserName(), e);
        }
    }

    /**
     * @param user
     * @throws IdentityException
     */
    public void removeChallengeAnswerOfUser(User user, String questionURI) throws
            IdentityRecoveryException {

        validateUser(user);
        if (log.isDebugEnabled()) {
            log.debug("Removing a Challenge answer from the user profile.");
        }

        Set<String> challengesUris = new HashSet<>(getChallengeQuestionUris(user));
        if (challengesUris.contains(questionURI)) {
            try {
                Utils.removeClaimFromUserStoreManager(user, new String[]{questionURI});
                challengesUris.remove(questionURI);
                setUserChallengesURI(user, challengesUris);
            } catch (UserStoreException e) {
                throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                        .ERROR_CODE_GETTING_CHALLENGE_QUESTIONS, user.getUserName(), e);
            }
        }
    }

    /**
     * @param user
     * @param userChallengeAnswers
     * @return
     */
    public boolean verifyChallengeAnswer(User user, UserChallengeAnswer[] userChallengeAnswers) throws
            IdentityRecoveryException {

        validateUser(user);

        boolean verification = false;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Verifying challenge question answers for %s.", user.toString()));
        }

        UserChallengeAnswer[] storedAnswers = getChallengeAnswersOfUser(user);

        for (UserChallengeAnswer userChallengeAnswer : userChallengeAnswers) {
            if (StringUtils.isBlank(userChallengeAnswer.getAnswer())) {
                return false;
            }

            for (UserChallengeAnswer storedAnswer : storedAnswers) {
                if ((userChallengeAnswer.getQuestion().getQuestionSetId() == null || !userChallengeAnswer.getQuestion().getQuestionSetId()
                        .trim().equals(storedAnswer.getQuestion().getQuestionSetId())) &&
                        (userChallengeAnswer.getQuestion().getQuestion() == null || !userChallengeAnswer.getQuestion().getQuestion().
                                trim().equals(storedAnswer.getQuestion().getQuestion()))) {
                    continue;

                }

                String hashedAnswer = null;
                try {
                    hashedAnswer = Utils.doHash(userChallengeAnswer.getAnswer().trim().toLowerCase());
                } catch (UserStoreException e) {
                    throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                            .ERROR_CODE_NO_HASHING_ALGO, null, e);
                }

                if (hashedAnswer.equals(storedAnswer.getAnswer())) {
                    verification = true;
                } else {
                    return false;
                }
            }
        }

        return verification;
    }

    public boolean verifyUserChallengeAnswer(User user, UserChallengeAnswer userChallengeAnswer)
            throws IdentityRecoveryException {

        // check whether user data are valid.
        validateUser(user);

        boolean verification = false;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Verifying challenge question answer for %s.", user.toString()));
        }

        UserChallengeAnswer[] storedDto = getChallengeAnswersOfUser(user);
        if (StringUtils.isBlank(userChallengeAnswer.getAnswer())) {
            log.error("Invalid. Empty answer provided for the challenge question.");
            return false;
        }

        for (UserChallengeAnswer dto : storedDto) {
            if (dto.getQuestion().getQuestionSetId().equals(userChallengeAnswer.getQuestion().getQuestionSetId())) {
                String hashedAnswer = null;
                try {
                    hashedAnswer = Utils.doHash(userChallengeAnswer.getAnswer().trim().toLowerCase());
                } catch (UserStoreException e) {
                    throw Utils.handleServerException(IdentityRecoveryConstants.ErrorMessages
                            .ERROR_CODE_NO_HASHING_ALGO, null, e);
                }
                if (hashedAnswer.equals(dto.getAnswer())) {
                    verification = true;
                    if (log.isDebugEnabled()) {
                        log.debug("Challenge question answer verified successfully.");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Challenge question answer verification failed.");
                    }
                    return false;
                }
            }
        }

        return verification;
    }

    /**
     * Validate whether two questions from the same question set have been answered (ie. we only allow a maximum of
     * one question from each set)
     *
     * @param userChallengeAnswers
     * @throws IdentityRecoveryException
     */
    private void validateSecurityQuestionDuplicate(UserChallengeAnswer[] userChallengeAnswers)
            throws IdentityRecoveryException {

        Set<String> tmpMap = new HashSet<>();
        UserChallengeAnswer challengeAnswer;
        ChallengeQuestion challengeQuestion;

        for (UserChallengeAnswer userChallengeAnswer : userChallengeAnswers) {
            challengeAnswer = userChallengeAnswer;
            challengeQuestion = challengeAnswer.getQuestion();
            // if there's no challenge question details we throw a client exception
            if (challengeQuestion == null) {
                String errorMsg = "Challenge question details not provided with the challenge answers.";
                throw Utils.handleClientException(
                        IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_CHALLENGE_QUESTION_NOT_FOUND, errorMsg);
            }

            if (tmpMap.contains(challengeQuestion.getQuestionSetId())) {
                log.error(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_DUPLICATE_ANSWERS.getMessage());
                throw Utils.handleClientException(
                        IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_DUPLICATE_ANSWERS, null);
            }
            tmpMap.add(challengeQuestion.getQuestionSetId());
        }
    }


    /**
     * Check whether an answered challenge question actually exists in the tenant registry
     *
     * @param userChallengeAnswers
     * @param tenantDomain
     * @throws IdentityRecoveryClientException
     */
    private void checkChallengeQuestionExists(UserChallengeAnswer[] userChallengeAnswers, String tenantDomain)
            throws IdentityRecoveryException {

        for (UserChallengeAnswer challengeAnswer : userChallengeAnswers) {
            ChallengeQuestion challengeQuestion = challengeAnswer.getQuestion();
            // if challenge question details are missing in the challenge answer we can't proceed further
            if (challengeQuestion == null) {
                throw Utils.handleClientException(ERROR_CODE_CHALLENG_ANSWER_MISSING, null);
            }

            if (StringUtils.isBlank(challengeQuestion.getQuestion())) {
                throw Utils.handleClientException(ERROR_CODE_INVALID_CHALLENGE_QUESTION_VALUE, null);
            }

            String locale = validateLocale(challengeQuestion.getLocale());

            List<ChallengeQuestion> challengeQuestions = getAllChallengeQuestions(tenantDomain, locale);
            boolean isQuestionAvailable = false;
            for (ChallengeQuestion availableQuestion : challengeQuestions) {
                if (StringUtils.equals(availableQuestion.getQuestionSetId(), challengeQuestion.getQuestionSetId().trim())
                        && StringUtils.equals(availableQuestion.getQuestion().trim(),
                        challengeQuestion.getQuestion().trim()
                )) {
                    isQuestionAvailable = true;
                }
            }

            if (!isQuestionAvailable) {
                String error = "Error persisting user challenge answers for user. " +
                        "Challenge question answered is not registered with %s domain.";
                throw Utils.handleClientException(
                        IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_CHALLENGE_QUESTION_NOT_FOUND,
                        String.format(error, tenantDomain));
            }
        }
    }


    private String validateTenantDomain(String tenantDomain) {
        return StringUtils.isBlank(tenantDomain) ? MultitenantConstants.SUPER_TENANT_DOMAIN_NAME : tenantDomain;
    }

    private void validateUser(User user) throws IdentityRecoveryClientException {
        if (user == null || StringUtils.isBlank(user.getUserName())) {
            throw Utils.handleClientException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_USER, "Invalid User Data provided.");
        }
    }

    private String getLocaleOfUser(User user, String tenantDomain) throws IdentityRecoveryException {
        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(user.getUserName());
        String locale = IdentityRecoveryConstants.LOCALE_EN_US;
        try {
            String userLocale =
                    Utils.getClaimFromUserStoreManager(user, IdentityRecoveryConstants.Questions.LOCALE_CLAIM);
            if (StringUtils.isNotBlank(userLocale)) {
                locale = userLocale;
            }
        } catch (UserStoreException e) {
            String errorMsg = String.format(IdentityRecoveryConstants.ErrorMessages
                            .ERROR_CODE_ERROR_RETRIVING_CLAIM.getMessage(),
                    tenantAwareUserName, tenantDomain);
            log.error(errorMsg);
            throw IdentityException.error(IdentityRecoveryServerException.class, IdentityRecoveryConstants.ErrorMessages
                    .ERROR_CODE_ERROR_RETRIVING_CLAIM.getCode(), errorMsg, e);
        }
        return locale;
    }
}
