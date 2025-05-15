/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.challenge.questions.recovery;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.JDBCChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithRegistry;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.LOCALE_1;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.LOCALE_2;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_1;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_2;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_3;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_4;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_5;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_6;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_ID_1;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_ID_2;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_SET_ID_1;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_SET_ID_2;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.TENANT_DOMAIN;

/**
 * This class tests the methods of the JDBCChallengeQuestionDAOImpl class.
 */
@WithRegistry
@WithRealmService(initUserStoreManager = true)
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/h2.sql"})
@WithCarbonHome
public class JDBCChallengeQuestionManagerTest {

    public ChallengeQuestionManager challengeQuestionManager;
    ChallengeQuestion challengeQuestion1;
    ChallengeQuestion challengeQuestion2;
    ChallengeQuestion challengeQuestion3;
    ChallengeQuestion challengeQuestion4;
    ChallengeQuestion challengeQuestion5;
    ChallengeQuestion challengeQuestion6;
    List<ChallengeQuestion> sampleChallengeQuestions;

    @BeforeMethod
    public void setUp() throws Exception {

        challengeQuestionManager = ChallengeQuestionManager.getInstance();
        challengeQuestionManager.challengeQuestionsImpl = new JDBCChallengeQuestionDAOImpl();
        setChallengeQuestions();
        sampleChallengeQuestions = getChallengeQuestions();
    }

    @AfterMethod
    public void tearDown() throws Exception {

        challengeQuestionManager.deleteChallengeQuestions(getChallengeQuestions().toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
    }

    @Test
    public void testGetAllChallengeQuestions() throws Exception {

        addChallengeQuestions();
        List<ChallengeQuestion> questionsFromStorage = challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        assertEquals(questionsFromStorage, sampleChallengeQuestions);
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testGetAllChallengeQuestionsWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeQuery(any(), any(), any());

            challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        }
    }

    @Test
    public void testGetAllChallengeQuestionsByLocale() throws Exception {

        addChallengeQuestions();
        List<ChallengeQuestion> challengeQuestionsFromStorage =
                challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1);
        List<ChallengeQuestion> filteredChallengeQuestions = new ArrayList<>(
                Arrays.asList(challengeQuestion1, challengeQuestion3, challengeQuestion4, challengeQuestion5));

        assertEquals(challengeQuestionsFromStorage, filteredChallengeQuestions);
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testGetAllChallengeQuestionsByLocaleWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeQuery(any(), any(), any());

            challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1);
        }
    }

    @Test
    public void testGetAllChallengeQuestionSetsURIs() throws Exception {

        addChallengeQuestions();
        List<String> setURIs = challengeQuestionManager.getAllChallengeQuestionSetsURIs(TENANT_DOMAIN);
        assertEquals(Arrays.asList(QUESTION_SET_ID_1, QUESTION_SET_ID_2), setURIs);
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testGetAllChallengeQuestionSetsURIsWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeQuery(any(), any(), any());

            challengeQuestionManager.getAllChallengeQuestionSetsURIs(TENANT_DOMAIN);
        }
    }

    @Test(priority = 1)
    public void testAddChallengeQuestions() throws Exception {

        challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
        List<ChallengeQuestion> storedQuestions = challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        assertEquals(storedQuestions, sampleChallengeQuestions);
    }

    @Test
    public void testAddExistingChallengeQuestions() throws Exception {

        challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);

        challengeQuestion2.setQuestion("Updated Question");
        challengeQuestionManager.addChallengeQuestions(new ChallengeQuestion[]{challengeQuestion1, challengeQuestion2},
                TENANT_DOMAIN);

        List<ChallengeQuestion> storedQuestions = challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        assertEquals(storedQuestions, sampleChallengeQuestions);
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testAddChallengeQuestionsWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeInsert(any(), any(), any(), eq(false));

            challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                    TENANT_DOMAIN);
        }
    }

    @Test
    public void testDeleteChallengeQuestions() throws Exception {

        addChallengeQuestions();
        List<ChallengeQuestion> challengeQuestionsToDelete =
                new ArrayList<>(Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3));
        List<ChallengeQuestion> remainingChallengeQuestions =
                new ArrayList<>(Arrays.asList(challengeQuestion4, challengeQuestion5, challengeQuestion6));

        challengeQuestionManager.deleteChallengeQuestions(challengeQuestionsToDelete.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
        assertEquals(challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN), remainingChallengeQuestions);
    }

    @Test
    public void testDeleteChallengeQuestionsWithoutLocale() throws Exception {

        addChallengeQuestions();
        challengeQuestion1.setLocale("");
        challengeQuestion5.setLocale("");
        List<ChallengeQuestion> challengeQuestionsToDelete =
                new ArrayList<>(Arrays.asList(challengeQuestion1, challengeQuestion5));
        List<ChallengeQuestion> remainingChallengeQuestions =
                new ArrayList<>(Arrays.asList(challengeQuestion3, challengeQuestion4));

        challengeQuestionManager.deleteChallengeQuestions(challengeQuestionsToDelete.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
        assertEquals(challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN), remainingChallengeQuestions);
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testDeleteChallengeQuestionsWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeUpdate(any(), any());

            challengeQuestionManager.deleteChallengeQuestions(
                    new ChallengeQuestion[]{challengeQuestion1, challengeQuestion2, challengeQuestion3}, TENANT_DOMAIN);
        }
    }

    @Test
    public void testDeleteChallengeQuestionSet() throws Exception {

        addChallengeQuestions();
        List<ChallengeQuestion> remainingChallengeQuestions =
                new ArrayList<>(Arrays.asList(challengeQuestion4, challengeQuestion5, challengeQuestion6));

        challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, "", TENANT_DOMAIN);
        assertEquals(challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN), remainingChallengeQuestions);

    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testDeleteChallengeQuestionSetWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate)
                    .executeUpdate(any(), any());

            challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, "", TENANT_DOMAIN);
        }
    }

    @Test
    public void testDeleteChallengeQuestionSetByLocale() throws Exception {

        addChallengeQuestions();
        List<ChallengeQuestion> remainingChallengeQuestions =
                new ArrayList<>(Arrays.asList(challengeQuestion4, challengeQuestion5));

        challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, LOCALE_1, TENANT_DOMAIN);
        assertEquals(challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1),
                remainingChallengeQuestions);

    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testDeleteChallengeQuestionSetByLocaleWithException() throws Exception {

        try (MockedStatic<JdbcUtils> jdbcUtilsMockedStatic = mockStatic(JdbcUtils.class)) {
            NamedJdbcTemplate namedJdbcTemplate = mock(NamedJdbcTemplate.class);
            jdbcUtilsMockedStatic.when(JdbcUtils::getNewNamedJdbcTemplate).thenReturn(namedJdbcTemplate);
            doThrow(new DataAccessException("Data access exception")).when(namedJdbcTemplate).executeUpdate(any(), any());

            challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, LOCALE_1, TENANT_DOMAIN);
        }
    }

    @Test
    public void isChallengeQuestionExistsWithLocale() throws Exception {

        addChallengeQuestions();
        JDBCChallengeQuestionDAOImpl jdbcChallengeQuestionDAO =
                (JDBCChallengeQuestionDAOImpl) challengeQuestionManager.challengeQuestionsImpl;
        assertTrue(jdbcChallengeQuestionDAO.isChallengeQuestionExists(challengeQuestion1, TENANT_DOMAIN));
    }

    @Test
    public void isChallengeQuestionExistsWithoutLocale() throws Exception {

        addChallengeQuestions();
        challengeQuestion1.setLocale("");
        JDBCChallengeQuestionDAOImpl jdbcChallengeQuestionDAO =
                (JDBCChallengeQuestionDAOImpl) challengeQuestionManager.challengeQuestionsImpl;
        assertTrue(jdbcChallengeQuestionDAO.isChallengeQuestionExists(challengeQuestion1, TENANT_DOMAIN));

    }

    @Test
    public void isChallengeQuestionSetExists() throws Exception {

        addChallengeQuestions();
        JDBCChallengeQuestionDAOImpl jdbcChallengeQuestionDAO =
                (JDBCChallengeQuestionDAOImpl) challengeQuestionManager.challengeQuestionsImpl;
        assertTrue(jdbcChallengeQuestionDAO.isChallengeQuestionSetExists(QUESTION_SET_ID_1, TENANT_DOMAIN));

    }

    private void setChallengeQuestions() {

        challengeQuestion1 = new ChallengeQuestion(QUESTION_SET_ID_1, QUESTION_ID_1, QUESTION_1, LOCALE_1);
        challengeQuestion2 = new ChallengeQuestion(QUESTION_SET_ID_1, QUESTION_ID_1, QUESTION_2, LOCALE_2);
        challengeQuestion3 = new ChallengeQuestion(QUESTION_SET_ID_1, QUESTION_ID_2, QUESTION_3, LOCALE_1);
        challengeQuestion4 = new ChallengeQuestion(QUESTION_SET_ID_2, QUESTION_ID_1, QUESTION_4, LOCALE_1);
        challengeQuestion5 = new ChallengeQuestion(QUESTION_SET_ID_2, QUESTION_ID_2, QUESTION_5, LOCALE_1);
        challengeQuestion6 = new ChallengeQuestion(QUESTION_SET_ID_2, QUESTION_ID_2, QUESTION_6, LOCALE_2);

    }

    private List<ChallengeQuestion> getChallengeQuestions() {

        return new ArrayList<>(
                Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3, challengeQuestion4,
                        challengeQuestion5, challengeQuestion6));
    }

    private void addChallengeQuestions() throws IdentityRecoveryException {

        challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
    }
}
