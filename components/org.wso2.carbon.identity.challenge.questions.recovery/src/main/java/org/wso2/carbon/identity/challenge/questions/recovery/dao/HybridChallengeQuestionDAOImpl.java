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

import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;
import org.wso2.carbon.identity.recovery.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CHALLENGE_SET;

/**
 * Hybrid implementation of the ChallengeQuestionDAO interface.
 */
public class HybridChallengeQuestionDAOImpl implements ChallengeQuestionDAO {

    JDBCChallengeQuestionDAOImpl jdbcChallengeQuestionDAOImpl = new JDBCChallengeQuestionDAOImpl();
    RegistryChallengeQuestionDAOImpl registryChallengeQuestionDAOImpl = new RegistryChallengeQuestionDAOImpl();

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain)
            throws IdentityRecoveryServerException {

        List<ChallengeQuestion> jdbcChallengeQuestions =
                jdbcChallengeQuestionDAOImpl.getAllChallengeQuestions(tenantDomain);
        List<ChallengeQuestion> registryChallengeQuestions =
                registryChallengeQuestionDAOImpl.getAllChallengeQuestions(tenantDomain);

        return mergeCQLists(jdbcChallengeQuestions, registryChallengeQuestions);
    }

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain, String locale)
            throws IdentityRecoveryException {

        List<ChallengeQuestion> jdbcChallengeQuestions =
                jdbcChallengeQuestionDAOImpl.getAllChallengeQuestions(tenantDomain, locale);
        List<ChallengeQuestion> registryChallengeQuestions =
                registryChallengeQuestionDAOImpl.getAllChallengeQuestions(tenantDomain, locale);

        return mergeCQLists(jdbcChallengeQuestions, registryChallengeQuestions);
    }

    @Override
    public List<String> getAllChallengeQuestionSetsURIs(String tenantDomain) throws IdentityRecoveryServerException {

        List<String> jdbcChallengeQuestionSetURIs =
                jdbcChallengeQuestionDAOImpl.getAllChallengeQuestionSetsURIs(tenantDomain);
        List<String> registryChallengeQuestions =
                registryChallengeQuestionDAOImpl.getAllChallengeQuestionSetsURIs(tenantDomain);

        return mergeLists(jdbcChallengeQuestionSetURIs, registryChallengeQuestions);
    }

    @Override
    public void addChallengeQuestions(ChallengeQuestion[] questions, String tenantDomain)
            throws IdentityRecoveryException {

        jdbcChallengeQuestionDAOImpl.addChallengeQuestions(questions, tenantDomain);
    }

    @Override
    public void deleteChallengeQuestions(ChallengeQuestion[] challengeQuestions, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            for (ChallengeQuestion challengeQuestion : challengeQuestions) {
                if (jdbcChallengeQuestionDAOImpl.isChallengeQuestionExists(challengeQuestion, tenantDomain)) {
                    jdbcChallengeQuestionDAOImpl.deleteChallengeQuestions(new ChallengeQuestion[]{challengeQuestion},
                            tenantDomain);
                } else {
                    registryChallengeQuestionDAOImpl.deleteChallengeQuestions(
                            new ChallengeQuestion[]{challengeQuestion}, tenantDomain);
                }
            }
        } catch (DataAccessException e) {
            throw new IdentityRecoveryException("Error when deleting challenge questions in " + tenantDomain, e);
        }
    }

    @Override
    public void deleteChallengeQuestionSet(String challengeQuestionUri, String locale, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            if (jdbcChallengeQuestionDAOImpl.isChallengeQuestionSetExists(challengeQuestionUri, tenantDomain)) {
                jdbcChallengeQuestionDAOImpl.deleteChallengeQuestionSet(challengeQuestionUri, locale, tenantDomain);
            } else {
                registryChallengeQuestionDAOImpl.deleteChallengeQuestionSet(challengeQuestionUri, locale, tenantDomain);
            }
        } catch (DataAccessException e) {
            throw Utils.handleServerException(ERROR_CODE_ERROR_DELETING_CHALLENGE_SET, challengeQuestionUri, e);
        }
    }

    /**
     * Merges two lists and removes duplicates.
     *
     * @param list1 First list.
     * @param list2 Second list.
     * @return Merged list without duplicates.
     */
    public static <T> List<T> mergeLists(List<T> list1, List<T> list2) {

        Set<T> uniqueElements = new HashSet<>();
        uniqueElements.addAll(list1);
        uniqueElements.addAll(list2);
        return removeNullElements(new ArrayList<>((uniqueElements)));
    }

    /**
     * Removes null elements from a list.
     *
     * @param list List to remove null elements.
     * @return List without null elements.
     */
    public static <T> List<T> removeNullElements(List<T> list) {

        List<T> nonNullElements = new ArrayList<>();
        for (T element : list) {
            if (element != null) {
                nonNullElements.add(element);
            }
        }
        return nonNullElements;
    }

    /**
     * Merges two lists of ChallengeQuestion objects and removes challenge questions with the same
     * Question Set ID, Question ID and Locale prioritizing the first list.
     *
     * @param list1 First list of ChallengeQuestion objects.
     * @param list2 Second list of ChallengeQuestion objects.
     * @return Merged list of ChallengeQuestion objects without duplicates.
     */
    public static List<ChallengeQuestion> mergeCQLists(List<ChallengeQuestion> list1, List<ChallengeQuestion> list2) {

        List<ChallengeQuestion> result = new ArrayList<>(list1);
        Set<String> keys = new HashSet<>();

        for (ChallengeQuestion cq : list1) {
            keys.add(generateKey(cq));
        }
        for (ChallengeQuestion cq : list2) {
            String key = generateKey(cq);
            if (!keys.contains(key)) {
                result.add(cq);
                keys.add(key);
            }
        }
        return result;
    }

    /**
     * Generates a key value using Question Set ID, Question ID and Locale.
     *
     * @param challengeQuestion Challenge question to generate a unique key for.
     * @return Unique key for challenge question.
     */
    private static String generateKey(ChallengeQuestion challengeQuestion) {

        return challengeQuestion.getQuestionSetId() + "|" + challengeQuestion.getQuestionId() + "|" +
                challengeQuestion.getLocale();
    }
}
