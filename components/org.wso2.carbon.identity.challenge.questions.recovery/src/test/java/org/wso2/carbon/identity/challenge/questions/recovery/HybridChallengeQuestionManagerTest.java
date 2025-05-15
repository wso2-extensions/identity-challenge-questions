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

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.HybridChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.JDBCChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.RegistryChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.common.testng.WithRegistry;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;
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
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.QUESTION_SET_ID_3;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.TENANT_DOMAIN;
import static org.wso2.carbon.identity.challenge.questions.recovery.constant.TestConstants.UPDATED_QUESTION;

/**
 * This class tests the methods of the HybridChallengeQuestionDAOImpl class.
 */
@WithRegistry
@WithRealmService(initUserStoreManager = true)
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/h2.sql"})
@WithCarbonHome
public class HybridChallengeQuestionManagerTest {

    private ChallengeQuestionManager challengeQuestionManager;
    private ChallengeQuestion challengeQuestion1;
    private ChallengeQuestion challengeQuestion2;
    private ChallengeQuestion challengeQuestion3;
    private ChallengeQuestion challengeQuestion4;
    private ChallengeQuestion challengeQuestion5;
    private ChallengeQuestion challengeQuestion6;
    private List<ChallengeQuestion> sampleChallengeQuestions;

    JDBCChallengeQuestionDAOImpl jdbcChallengeQuestionDAOImpl;
    RegistryChallengeQuestionDAOImpl registryChallengeQuestionDAOImpl;

    @BeforeMethod
    public void setUp() throws Exception {

        challengeQuestionManager = ChallengeQuestionManager.getInstance();

        jdbcChallengeQuestionDAOImpl = Mockito.mock(JDBCChallengeQuestionDAOImpl.class);
        registryChallengeQuestionDAOImpl = Mockito.mock(RegistryChallengeQuestionDAOImpl.class);

        HybridChallengeQuestionDAOImpl hybridChallengeQuestionDAOImpl =
                Mockito.spy(new HybridChallengeQuestionDAOImpl());

        Field jdbcField = HybridChallengeQuestionDAOImpl.class.getDeclaredField("jdbcChallengeQuestionDAOImpl");
        jdbcField.setAccessible(true);
        jdbcField.set(hybridChallengeQuestionDAOImpl, jdbcChallengeQuestionDAOImpl);

        Field registryField = HybridChallengeQuestionDAOImpl.class.getDeclaredField("registryChallengeQuestionDAOImpl");
        registryField.setAccessible(true);
        registryField.set(hybridChallengeQuestionDAOImpl, registryChallengeQuestionDAOImpl);

        challengeQuestionManager.challengeQuestionsImpl = hybridChallengeQuestionDAOImpl;

        setChallengeQuestions();
        sampleChallengeQuestions = getChallengeQuestions();

    }

    @AfterMethod
    public void tearDown() throws Exception {

        Mockito.reset(jdbcChallengeQuestionDAOImpl, registryChallengeQuestionDAOImpl);
    }


    @Test(dataProvider = "challengeQuestionProvider")
    public void testGetAllChallengeQuestions(List<ChallengeQuestion> jdbcChallengeQuestions,
                                             List<ChallengeQuestion> registryChallengeQuestions,
                                             List<ChallengeQuestion> expectedChallengeQuestions) throws Exception {

        when(jdbcChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN)).thenReturn(jdbcChallengeQuestions);
        when(registryChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN)).thenReturn(
                registryChallengeQuestions);

        List<ChallengeQuestion> questionsFromStorage = challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        assertEquals(questionsFromStorage, expectedChallengeQuestions);
    }

    @Test(dataProvider = "challengeQuestionProviderLocale")
    public void testGetAllChallengeQuestionsByLocale(List<ChallengeQuestion> jdbcChallengeQuestions,
                                                     List<ChallengeQuestion> registryChallengeQuestions,
                                                     List<ChallengeQuestion> expectedChallengeQuestions)
            throws Exception {

        when(jdbcChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1)).thenReturn(
                jdbcChallengeQuestions);
        when(registryChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1)).thenReturn(
                registryChallengeQuestions);

        List<ChallengeQuestion> questionsFromStorage =
                challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN, LOCALE_1);
        assertEquals(questionsFromStorage, expectedChallengeQuestions);
    }

    @Test
    public void testGetAllChallengeQuestionSetsURIs() throws Exception {

        when(jdbcChallengeQuestionDAOImpl.getAllChallengeQuestionSetsURIs(TENANT_DOMAIN)).thenReturn(
                Arrays.asList(QUESTION_SET_ID_1, QUESTION_SET_ID_2));
        when(registryChallengeQuestionDAOImpl.getAllChallengeQuestionSetsURIs(TENANT_DOMAIN)).thenReturn(
                Arrays.asList(QUESTION_SET_ID_2, QUESTION_SET_ID_3));

        List<String> questionSetsURIs = challengeQuestionManager.getAllChallengeQuestionSetsURIs(TENANT_DOMAIN);
        assertEquals(questionSetsURIs, Arrays.asList(QUESTION_SET_ID_3, QUESTION_SET_ID_2, QUESTION_SET_ID_1));
    }

    @Test
    public void testAddChallengeQuestions() throws Exception {

        challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
        ArgumentCaptor<ChallengeQuestion[]> captor = ArgumentCaptor.forClass(ChallengeQuestion[].class);
        verify(jdbcChallengeQuestionDAOImpl, times(1)).addChallengeQuestions(captor.capture(), eq(TENANT_DOMAIN));
        assertArrayEquals(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]), captor.getValue());
    }

    @Test
    public void testDeleteChallengeQuestions() throws Exception {

        challengeQuestionManager.addChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);
        challengeQuestionManager.deleteChallengeQuestions(sampleChallengeQuestions.toArray(new ChallengeQuestion[0]),
                TENANT_DOMAIN);

        when(jdbcChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN)).thenReturn(Collections.emptyList());
        when(registryChallengeQuestionDAOImpl.getAllChallengeQuestions(TENANT_DOMAIN)).thenReturn(
                Collections.emptyList());

        List<ChallengeQuestion> remainingQuestions = challengeQuestionManager.getAllChallengeQuestions(TENANT_DOMAIN);
        assertEquals(remainingQuestions.size(), 0);
    }

    @Test
    public void testDeleteChallengeQuestionsInDatabase() throws Exception {

        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionExists(challengeQuestion1, TENANT_DOMAIN)).thenReturn(
                true);
        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionExists(challengeQuestion2, TENANT_DOMAIN)).thenReturn(
                false);
        challengeQuestionManager.deleteChallengeQuestions(
                new ChallengeQuestion[]{challengeQuestion1, challengeQuestion2}, TENANT_DOMAIN);

        ArgumentCaptor<ChallengeQuestion[]> jdbcCaptor = ArgumentCaptor.forClass(ChallengeQuestion[].class);
        verify(jdbcChallengeQuestionDAOImpl, times(1)).deleteChallengeQuestions(jdbcCaptor.capture(),
                eq(TENANT_DOMAIN));
        assertArrayEquals(new ChallengeQuestion[]{challengeQuestion1}, jdbcCaptor.getValue());
    }

    @Test
    public void testDeleteChallengeQuestionsInRegistry() throws Exception {

        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionExists(challengeQuestion1, TENANT_DOMAIN)).thenReturn(
                false);
        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionExists(challengeQuestion2, TENANT_DOMAIN)).thenReturn(
                true);
        challengeQuestionManager.deleteChallengeQuestions(
                new ChallengeQuestion[]{challengeQuestion1, challengeQuestion2}, TENANT_DOMAIN);

        // Verify registry impl is called only with challengeQuestion2
        ArgumentCaptor<ChallengeQuestion[]> registryCaptor = ArgumentCaptor.forClass(ChallengeQuestion[].class);
        verify(registryChallengeQuestionDAOImpl, times(1)).deleteChallengeQuestions(registryCaptor.capture(), eq(TENANT_DOMAIN));
        assertArrayEquals(new ChallengeQuestion[]{challengeQuestion1}, registryCaptor.getValue());
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testDeleteChallengeQuestionsWithException() throws Exception {

        doThrow(new DataAccessException("Data access exception")).when(jdbcChallengeQuestionDAOImpl)
                .isChallengeQuestionExists(challengeQuestion1, TENANT_DOMAIN);

        challengeQuestionManager.deleteChallengeQuestions(
                new ChallengeQuestion[]{challengeQuestion1, challengeQuestion2}, TENANT_DOMAIN);
    }

    @Test
    public void testDeleteChallengeQuestionSetInDatabase() throws Exception {

        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionSetExists(QUESTION_SET_ID_1, TENANT_DOMAIN)).thenReturn(
                true);
        challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, LOCALE_1, TENANT_DOMAIN);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbcChallengeQuestionDAOImpl, times(1)).deleteChallengeQuestionSet(captor.capture(), eq(LOCALE_1),
                eq(TENANT_DOMAIN));
        assertEquals(captor.getValue(), QUESTION_SET_ID_1);
        verify(registryChallengeQuestionDAOImpl, times(0)).deleteChallengeQuestionSet(anyString(), eq(LOCALE_1),
                eq(TENANT_DOMAIN));
    }

    @Test
    public void testDeleteChallengeQuestionSetInRegistry() throws Exception {

        when(jdbcChallengeQuestionDAOImpl.isChallengeQuestionSetExists(QUESTION_SET_ID_1, TENANT_DOMAIN)).thenReturn(
                false);
        challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, LOCALE_1, TENANT_DOMAIN);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(registryChallengeQuestionDAOImpl, times(1)).deleteChallengeQuestionSet(captor.capture(), eq(LOCALE_1),
                eq(TENANT_DOMAIN));
        assertEquals(captor.getValue(), QUESTION_SET_ID_1);

        verify(jdbcChallengeQuestionDAOImpl, times(0)).deleteChallengeQuestionSet(anyString(), eq(LOCALE_1),
                eq(TENANT_DOMAIN));
    }

    @Test(expectedExceptions = IdentityRecoveryException.class)
    public void testDeleteChallengeQuestionSetWithException() throws Exception {

        doThrow(new DataAccessException("Data access exception")).when(jdbcChallengeQuestionDAOImpl)
                .isChallengeQuestionSetExists(QUESTION_SET_ID_1, TENANT_DOMAIN);

        challengeQuestionManager.deleteChallengeQuestionSet(QUESTION_SET_ID_1, LOCALE_1, TENANT_DOMAIN);
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

    @DataProvider(name = "challengeQuestionProvider")
    public Object[][] provideChallengeQuestions() {

        setChallengeQuestions();
        ChallengeQuestion UpdatedChallengeQuestion4 =
                new ChallengeQuestion(QUESTION_SET_ID_2, QUESTION_ID_1, UPDATED_QUESTION, LOCALE_1);

        // First iteration
        List<ChallengeQuestion> jdbcChallengeQuestions1 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3);
        List<ChallengeQuestion> registryChallengeQuestions1 =
                Arrays.asList(challengeQuestion4, challengeQuestion5, challengeQuestion6);
        List<ChallengeQuestion> expectedChallengeQuestions1 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3, challengeQuestion4,
                        challengeQuestion5, challengeQuestion6);

        // Second iteration
        List<ChallengeQuestion> jdbcChallengeQuestions2 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3);
        List<ChallengeQuestion> registryChallengeQuestions2 =
                Arrays.asList(challengeQuestion3, challengeQuestion4, challengeQuestion5, challengeQuestion6);
        List<ChallengeQuestion> expectedChallengeQuestions2 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, challengeQuestion3, challengeQuestion4,
                        challengeQuestion5, challengeQuestion6);

        // Third iteration
        List<ChallengeQuestion> jdbcChallengeQuestions3 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, UpdatedChallengeQuestion4);
        List<ChallengeQuestion> registryChallengeQuestions3 =
                Arrays.asList(challengeQuestion3, challengeQuestion4, challengeQuestion5, challengeQuestion6);
        List<ChallengeQuestion> expectedChallengeQuestions3 =
                Arrays.asList(challengeQuestion1, challengeQuestion2, UpdatedChallengeQuestion4, challengeQuestion3,
                        challengeQuestion5, challengeQuestion6);

        return new Object[][]{{jdbcChallengeQuestions1, registryChallengeQuestions1, expectedChallengeQuestions1},
                {jdbcChallengeQuestions2, registryChallengeQuestions2, expectedChallengeQuestions2},
                {jdbcChallengeQuestions3, registryChallengeQuestions3, expectedChallengeQuestions3}};
    }

    @DataProvider(name = "challengeQuestionProviderLocale")
    public Object[][] provideChallengeQuestionsLocale() {

        setChallengeQuestions();
        ChallengeQuestion UpdatedChallengeQuestion4 =
                new ChallengeQuestion(QUESTION_SET_ID_2, QUESTION_ID_1, UPDATED_QUESTION, LOCALE_1);

        // First iteration
        List<ChallengeQuestion> jdbcChallengeQuestions1 = Arrays.asList(challengeQuestion1, challengeQuestion3);
        List<ChallengeQuestion> registryChallengeQuestions1 = Arrays.asList(challengeQuestion4, challengeQuestion5);
        List<ChallengeQuestion> expectedChallengeQuestions1 =
                Arrays.asList(challengeQuestion1, challengeQuestion3, challengeQuestion4, challengeQuestion5);

        // Second iteration
        List<ChallengeQuestion> jdbcChallengeQuestions2 = Arrays.asList(challengeQuestion1, challengeQuestion3);
        List<ChallengeQuestion> registryChallengeQuestions2 =
                Arrays.asList(challengeQuestion3, challengeQuestion4, challengeQuestion5);
        List<ChallengeQuestion> expectedChallengeQuestions2 =
                Arrays.asList(challengeQuestion1, challengeQuestion3, challengeQuestion4, challengeQuestion5);

        // Third iteration
        List<ChallengeQuestion> jdbcChallengeQuestions3 = Arrays.asList(challengeQuestion1, UpdatedChallengeQuestion4);
        List<ChallengeQuestion> registryChallengeQuestions3 =
                Arrays.asList(challengeQuestion3, challengeQuestion4, challengeQuestion5);
        List<ChallengeQuestion> expectedChallengeQuestions3 =
                Arrays.asList(challengeQuestion1, UpdatedChallengeQuestion4, challengeQuestion3, challengeQuestion5);

        return new Object[][]{{jdbcChallengeQuestions1, registryChallengeQuestions1, expectedChallengeQuestions1},
                {jdbcChallengeQuestions2, registryChallengeQuestions2, expectedChallengeQuestions2},
                {jdbcChallengeQuestions3, registryChallengeQuestions3, expectedChallengeQuestions3}};
    }
}
