/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.challenge.questions.recovery.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.recovery.IdentityRecoveryClientException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.recovery.util.Utils;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeSetURI;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeQuestionAttributes;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeQuestionMandatoryParams;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateLocale;

/**
 * Registry implementation of the ChallengeQuestionDAO interface.
 */
public class RegistryChallengeQuestionDAOImpl implements ChallengeQuestionDAO {

    private static final String QUESTIONS_BASE_PATH = IdentityRecoveryConstants.IDENTITY_MANAGEMENT_QUESTIONS;

    private IdentityRecoveryServiceDataHolder dataHolder = IdentityRecoveryServiceDataHolder.getInstance();
    private RegistryResourceMgtService resourceMgtService = dataHolder.getResourceMgtService();

    public RegistryChallengeQuestionDAOImpl() {

    }

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain)
            throws IdentityRecoveryServerException {

        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();

        try {
            Resource questionCollection = resourceMgtService.getIdentityResource(QUESTIONS_BASE_PATH, tenantDomain);
            if (questionCollection != null) {
                Collection questionSetCollection = (Collection) questionCollection;

                for (String questionSetId : questionSetCollection.getChildren()) {
                    Collection questionIdCollection =
                            (Collection) resourceMgtService.getIdentityResource(questionSetId, tenantDomain);
                    // Iterate each question to find the one with correct locale
                    for (String questionIdPath : questionIdCollection.getChildren()) {
                        Collection questions =
                                (Collection) resourceMgtService.getIdentityResource(questionIdPath, tenantDomain);
                        for (String question : questions.getChildren()) {
                            Resource resource = resourceMgtService.getIdentityResource(question, tenantDomain);
                            if (resource != null) {
                                challengeQuestions.add(createChallengeQuestion(resource));
                            }
                        }
                    }
                }
            }
            return challengeQuestions;
        } catch (RegistryException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_REGISTRY_EXCEPTION_GET_CHALLENGE_QUESTIONS, null,
                    e);
        }
    }

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain, String locale)
            throws IdentityRecoveryException {

        List<ChallengeQuestion> questions = new ArrayList<>();
        try {
            Resource questionCollection = resourceMgtService.getIdentityResource(QUESTIONS_BASE_PATH, tenantDomain);
            if (questionCollection != null) {
                Collection questionSetCollection = (Collection) questionCollection;

                for (String questionSetId : questionSetCollection.getChildren()) {
                    Collection questionIdCollection =
                            (Collection) resourceMgtService.getIdentityResource(questionSetId, tenantDomain);
                    for (String questionIdPath : questionIdCollection.getChildren()) {
                        Resource questionResource =
                                resourceMgtService.getIdentityResource(questionIdPath, tenantDomain, locale);
                        if (questionResource != null) {
                            questions.add(createChallengeQuestion(questionResource));
                        }
                    }
                }
            }

        } catch (RegistryException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_REGISTRY_EXCEPTION_GET_CHALLENGE_QUESTIONS, null,
                    e);
        }

        return questions;
    }

    @Override
    public List<String> getAllChallengeQuestionSetsURIs(String tenantDomain) throws IdentityRecoveryServerException {

        List<String> challengeQuestions = new ArrayList<>();

        try {
            Resource questionCollection = resourceMgtService.getIdentityResource(QUESTIONS_BASE_PATH, tenantDomain);
            if (questionCollection != null) {
                Collection questionSetCollection = (Collection) questionCollection;

                for (String questionSetId : questionSetCollection.getChildren()) {
                    challengeQuestions.add(questionSetId.replace(QUESTIONS_BASE_PATH,
                            IdentityRecoveryConstants.WSO2CARBON_CLAIM_DIALECT));
                }
            }
            return challengeQuestions;
        } catch (RegistryException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_REGISTRY_EXCEPTION_GET_CHALLENGE_QUESTIONS, null,
                    e);
        }
    }

    @Override
    public void addChallengeQuestions(ChallengeQuestion[] questions, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            Resource challengeQuestionCollection =
                    resourceMgtService.getIdentityResource(QUESTIONS_BASE_PATH, tenantDomain);

            if (challengeQuestionCollection == null) {
                challengeQuestionCollection = new CollectionImpl();
                resourceMgtService.putIdentityResource(challengeQuestionCollection, QUESTIONS_BASE_PATH, tenantDomain);
            }

            for (ChallengeQuestion challengeQuestion : questions) {
                validateChallengeQuestionAttributes(challengeQuestion);

                String questionPath = getQuestionPath(challengeQuestion);
                String locale = validateLocale(challengeQuestion.getLocale());

                Resource resource = createRegistryResource(challengeQuestion);
                resourceMgtService.putIdentityResource(resource, questionPath, tenantDomain, locale);
            }

        } catch (RegistryException | UnsupportedEncodingException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_REGISTRY_EXCEPTION_SET_CHALLENGE_QUESTIONS, null,
                    e);
        }
    }

    @Override
    public void deleteChallengeQuestions(ChallengeQuestion[] challengeQuestions, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            for (ChallengeQuestion question : challengeQuestions) {
                if (isChallengeQuestionExists(question, tenantDomain)) {
                    String questionPath = getQuestionPath(question);
                    if (StringUtils.isNotEmpty(question.getLocale())) {
                        String locale = question.getLocale();
                        resourceMgtService.deleteIdentityResource(questionPath, tenantDomain, locale);
                    } else {
                        resourceMgtService.deleteIdentityResource(questionPath, tenantDomain);
                    }
                }
            }
        } catch (IdentityRuntimeException e) {
            throw new IdentityRecoveryException("Error deleting challenge questions in " + tenantDomain, e);
        }
    }

    @Override
    public void deleteChallengeQuestionSet(String challengeQuestionUri, String locale, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            if (isChallengeQuestionSetExists(challengeQuestionUri, tenantDomain)) {
                String questionSetPath = getQuestionSetPath(challengeQuestionUri);
                if (StringUtils.isEmpty(locale)) {
                    resourceMgtService.deleteIdentityResource(questionSetPath, tenantDomain);
                } else {
                    deleteChallengeQuestionSetByLocale(questionSetPath, tenantDomain, locale);
                }
            }
        } catch (IdentityRuntimeException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CHALLENGE_SET,
                    challengeQuestionUri, e);
        }
    }

    /**
     * Delete challenge questions by locale.
     *
     * @param questionSetPath Challenge question path.
     * @param tenantDomain    Tenant domain of the user.
     * @param locale          Locale of the challenge questions.
     * @throws IdentityRecoveryServerException If an error occurs while deleting the challenge questions.
     */
    private void deleteChallengeQuestionSetByLocale(String questionSetPath, String tenantDomain, String locale)
            throws IdentityRecoveryServerException {

        try {
            Collection questionIdCollection =
                    (Collection) resourceMgtService.getIdentityResource(questionSetPath, tenantDomain);
            for (String questionIdPath : questionIdCollection.getChildren()) {
                Resource questionResource =
                        resourceMgtService.getIdentityResource(questionIdPath, tenantDomain, locale);
                if (questionResource != null) {
                    resourceMgtService.deleteIdentityResource(questionIdPath, tenantDomain, locale);
                }
            }
        } catch (RegistryException e) {
            throw Utils.handleServerException(
                    IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_REGISTRY_EXCEPTION_DELETE_CHALLENGE_QUESTIONS,
                    locale, e);
        }
    }

    /**
     * Check whether a challenge question exists in the tenant domain. Here we check whether a question exists with the
     * given questionSetID, questionID and locale, if provided.
     *
     * @param challengeQuestion Challenge question to be checked.
     * @param tenantDomain      Tenant domain of the user.
     * @return True if the challenge question exists, false otherwise.
     * @throws IdentityRecoveryClientException If an error occurs while checking the challenge question.
     */
    private boolean isChallengeQuestionExists(ChallengeQuestion challengeQuestion, String tenantDomain)
            throws IdentityRecoveryClientException {

        validateChallengeQuestionMandatoryParams(challengeQuestion);
        String questionPath = getQuestionPath(challengeQuestion);

        if (StringUtils.isNotEmpty(challengeQuestion.getLocale())) {
            String locale = validateLocale(challengeQuestion.getLocale());
            return (resourceMgtService.getIdentityResource(questionPath, tenantDomain, locale) != null);
        }
        return (resourceMgtService.getIdentityResource(questionPath, tenantDomain) != null);
    }

    /**
     * Check whether a challenge question set exists in the tenant domain.
     * Here we check whether a question set exists with questionSetID.
     *
     * @param questionSetID Challenge question set ID to be checked.
     * @param tenantDomain  Tenant domain of the user.
     * @return True if the challenge question set exists, false otherwise.
     * @throws IdentityRecoveryClientException If an error occurs while checking the challenge question set.
     */
    private boolean isChallengeQuestionSetExists(String questionSetID, String tenantDomain)
            throws IdentityRecoveryClientException {

        validateChallengeSetURI(questionSetID);
        String questionPath = getQuestionSetPath(questionSetID);

        return (resourceMgtService.getIdentityResource(questionPath, tenantDomain) != null);
    }

    /**
     * Create a challenge question object from the registry resource.
     *
     * @param resource Registry resource object.
     * @return Challenge question object.
     * @throws RegistryException If an error occurs while creating the challenge question object.
     */
    private ChallengeQuestion createChallengeQuestion(Resource resource) throws RegistryException {

        ChallengeQuestion challengeQuestion = null;

        byte[] resourceContent = (byte[]) resource.getContent();

        String questionText = new String(resourceContent, StandardCharsets.UTF_8);
        String questionSetId = resource.getProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_SET_ID);
        String questionId = resource.getProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_ID);
        String questionLocale = resource.getProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_LOCALE);

        if (questionSetId != null) {
            if (IdentityUtil.isBlank(questionLocale)) {
                questionLocale = IdentityRecoveryConstants.LOCALE_EN_US;
            }
            challengeQuestion = new ChallengeQuestion(questionSetId, questionId, questionText, questionLocale);
        }
        return challengeQuestion;
    }

    /**
     * Create registry resource from a challenge question model object.
     *
     * @param question Challenge question model object.
     * @return Registry resource object.
     * @throws RegistryException If an error occurs while creating the registry resource.
     */
    private Resource createRegistryResource(ChallengeQuestion question)
            throws RegistryException, UnsupportedEncodingException {

        byte[] questionText = question.getQuestion().getBytes(StandardCharsets.UTF_8);
        String questionSetId = question.getQuestionSetId();
        String questionId = question.getQuestionId();
        String locale = question.getLocale();

        Resource resource = new ResourceImpl();
        resource.setContent(questionText);
        resource.addProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_SET_ID, questionSetId);
        resource.addProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_ID, questionId);
        resource.addProperty(IdentityRecoveryConstants.Questions.CHALLENGE_QUESTION_LOCALE, locale);
        resource.setMediaType(RegistryConstants.TAG_MEDIA_TYPE);

        return resource;
    }

    /**
     * Get the relative path to the parent directory of the challenge question resource.
     *
     * @param challengeQuestion Challenge question to be checked.
     * @return Path to the parent of challenge question relative to the root of the registry.
     */
    private String getQuestionPath(ChallengeQuestion challengeQuestion) {

        String questionSetIdUri = challengeQuestion.getQuestionSetId();
        String questionId = challengeQuestion.getQuestionId();

        return getQuestionSetPath(questionSetIdUri) + RegistryConstants.PATH_SEPARATOR + questionId;
    }

    /**
     * Get the relative path to the parent directory of the challenge question resource.
     *
     * @param questionSetIdUri Challenge question set ID to be checked.
     * @return Path to the parent of challenge question relative to the root of the registry.
     */
    private String getQuestionSetPath(String questionSetIdUri) {

        String questionSetId = Utils.getChallengeSetDirFromUri(questionSetIdUri);

        return QUESTIONS_BASE_PATH + RegistryConstants.PATH_SEPARATOR + questionSetId;
    }
}
