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

import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;

import java.util.List;

/**
 * This interface is used to manage challenge questions.
 */
public interface ChallengeQuestionDAO {

    /**
     * Get all challenge questions registered for a tenant.
     *
     * @param tenantDomain Tenant domain of the user.
     * @return List of challenge questions.
     * @throws IdentityRecoveryServerException If an error occurs while retrieving the challenge questions.
     */
    List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain) throws IdentityRecoveryServerException;

    /**
     * Get registered challenge questions in tenant based on a locale.
     *
     * @param tenantDomain Tenant domain of the user.
     * @param locale       Challenge questions locale.
     * @return List of challenge questions by locale.
     * @throws IdentityRecoveryException If an error occurs while retrieving the challenge questions.
     */
    List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain, String locale)
            throws IdentityRecoveryException;

    /**
     * Get all challenge questions set URIs registered for a tenant.
     *
     * @param tenantDomain Tenant domain of the user.
     * @return List of challenge question set URIs.
     * @throws IdentityRecoveryServerException If an error occurs while retrieving the challenge question URIs.
     */
    List<String> getAllChallengeQuestionSetsURIs(String tenantDomain) throws IdentityRecoveryServerException;

    /**
     * Add new challenge questions to the registry of a tenant
     *
     * @param questions    List of challenge questions to be added.
     * @param tenantDomain Tenant domain of the user.
     * @throws IdentityRecoveryException If an error occurs while adding the challenge questions.
     */
    void addChallengeQuestions(ChallengeQuestion[] questions, String tenantDomain) throws IdentityRecoveryException;

    /**
     * Delete challenge questions from a tenant registry.
     *
     * @param challengeQuestions List of challenge questions to be deleted.
     * @param tenantDomain       Tenant domain of the user.
     * @throws IdentityRecoveryException If an error occurs while deleting the challenge questions.
     */
    void deleteChallengeQuestions(ChallengeQuestion[] challengeQuestions, String tenantDomain)
            throws IdentityRecoveryException;

    /**
     * Delete challenge question set from a tenant registry.
     *
     * @param challengeQuestionUri Challenge question set URI.
     * @param locale               Challenge questions locale.
     * @param tenantDomain         Tenant domain of the user.
     * @throws IdentityRecoveryException If an error occurs while retrieving a challenge question set.
     */
    void deleteChallengeQuestionSet(String challengeQuestionUri, String locale, String tenantDomain)
            throws IdentityRecoveryException;
}
