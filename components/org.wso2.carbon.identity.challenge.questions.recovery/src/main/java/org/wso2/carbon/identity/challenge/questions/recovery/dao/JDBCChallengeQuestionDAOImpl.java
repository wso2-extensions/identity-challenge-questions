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
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.challenge.questions.recovery.model.ChallengeQuestion;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.recovery.IdentityRecoveryClientException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryServerException;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.CREATED_AT;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.LOCALE;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.QUESTION;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.QUESTION_ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.QUESTION_SET_ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.UPDATED_AT;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.ChallengeQuestionTableColumns.VERSION;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.ADD_CHALLENGE_QUESTION;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.DELETE_CHALLENGE_QUESTION;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.DELETE_CHALLENGE_QUESTION_BY_LOCALE;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.DELETE_CHALLENGE_QUESTION_SET;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.DELETE_CHALLENGE_QUESTION_SET_BY_LOCALE;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.GET_CHALLENGE_QUESTION;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.GET_CHALLENGE_QUESTIONS_BY_TENANT_ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.GET_CHALLENGE_QUESTIONS_BY_TENANT_ID_LOCALE;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.GET_CHALLENGE_QUESTION_SET_ID;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.GET_CHALLENGE_QUESTION_WITH_LOCALE;
import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.SQLQueries.UPDATE_CHALLENGE_QUESTION;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.handleServerException;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeQuestionAttributes;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeQuestionMandatoryParams;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateChallengeSetURI;
import static org.wso2.carbon.identity.challenge.questions.recovery.util.Utils.validateLocale;

import static java.time.ZoneOffset.UTC;

/**
 * JDBC implementation of the ChallengeQuestionDAO interface.
 */
public class JDBCChallengeQuestionDAOImpl implements ChallengeQuestionDAO {

    private final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));

    public JDBCChallengeQuestionDAOImpl() {

    }

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain)
            throws IdentityRecoveryServerException {

        try {
            List<ChallengeQuestion> challengeQuestions;
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            challengeQuestions =
                    namedJdbcTemplate.executeQuery(GET_CHALLENGE_QUESTIONS_BY_TENANT_ID, (resultSet, rowNumber) -> {
                        String questionSetId = resultSet.getString(QUESTION_SET_ID);
                        String questionId = resultSet.getString(QUESTION_ID);
                        byte[] questionBytes = resultSet.getBytes(QUESTION);
                        String questionText =
                                (questionBytes != null) ? new String(questionBytes, StandardCharsets.UTF_8) : null;
                        String locale = resultSet.getString(LOCALE);

                        return new ChallengeQuestion(questionSetId, questionId, questionText, locale);
                    }, namedPreparedStatement -> namedPreparedStatement.setInt(TENANT_ID,
                            IdentityTenantUtil.getTenantId(tenantDomain)));
            return challengeQuestions;
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_DATABASE_EXCEPTION_GET_CHALLENGE_QUESTIONS,
                    null, e);
        }
    }

    @Override
    public List<ChallengeQuestion> getAllChallengeQuestions(String tenantDomain, String locale)
            throws IdentityRecoveryException {

        try {
            List<ChallengeQuestion> challengeQuestions;
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            challengeQuestions = namedJdbcTemplate.executeQuery(GET_CHALLENGE_QUESTIONS_BY_TENANT_ID_LOCALE,
                    (resultSet, rowNumber) -> {
                        String questionSetId = resultSet.getString(QUESTION_SET_ID);
                        String questionId = resultSet.getString(QUESTION_ID);
                        byte[] questionBytes = resultSet.getBytes(QUESTION);
                        String questionText =
                                (questionBytes != null) ? new String(questionBytes, StandardCharsets.UTF_8) : null;
                        return new ChallengeQuestion(questionSetId, questionId, questionText, locale);
                    }, namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, IdentityTenantUtil.getTenantId(tenantDomain));
                        namedPreparedStatement.setString(LOCALE, locale);
                    });
            return challengeQuestions;
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_DATABASE_EXCEPTION_GET_CHALLENGE_QUESTIONS,
                    null, e);
        }
    }

    @Override
    public List<String> getAllChallengeQuestionSetsURIs(String tenantDomain) throws IdentityRecoveryServerException {

        try {
            List<String> challengeQuestions;
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            challengeQuestions = namedJdbcTemplate.executeQuery(GET_CHALLENGE_QUESTION_SET_ID,
                    (resultSet, rowNumber) -> resultSet.getString(QUESTION_SET_ID),
                    namedPreparedStatement -> namedPreparedStatement.setInt(TENANT_ID,
                            IdentityTenantUtil.getTenantId(tenantDomain)));
            return challengeQuestions;
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_DATABASE_EXCEPTION_GET_CHALLENGE_QUESTIONS,
                    null, e);
        }
    }

    @Override
    public void addChallengeQuestions(ChallengeQuestion[] questions, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            Timestamp currentTime = new Timestamp(new Date().getTime());
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            for (ChallengeQuestion challengeQuestion : questions) {

                validateChallengeQuestionAttributes(challengeQuestion);
                String locale = validateLocale(challengeQuestion.getLocale());
                byte[] questionText = challengeQuestion.getQuestion().getBytes(StandardCharsets.UTF_8);

                if (isChallengeQuestionExists(challengeQuestion, tenantDomain)) {
                    namedJdbcTemplate.executeUpdate(UPDATE_CHALLENGE_QUESTION, (namedPreparedStatement) -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        namedPreparedStatement.setString(QUESTION_SET_ID, challengeQuestion.getQuestionSetId());
                        namedPreparedStatement.setString(QUESTION_ID, challengeQuestion.getQuestionId());
                        namedPreparedStatement.setBytes(QUESTION, questionText);
                        namedPreparedStatement.setString(LOCALE, locale);
                        namedPreparedStatement.setString(VERSION,
                                ChallengeQuestionsConstants.CHALLENGE_QUESTION_SCHEMA_VERSION);
                        namedPreparedStatement.setTimeStamp(UPDATED_AT, currentTime, CALENDAR);
                    });
                } else {
                    namedJdbcTemplate.executeInsert(ADD_CHALLENGE_QUESTION, (namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        namedPreparedStatement.setString(QUESTION_SET_ID, challengeQuestion.getQuestionSetId());
                        namedPreparedStatement.setString(QUESTION_ID, challengeQuestion.getQuestionId());
                        namedPreparedStatement.setBytes(QUESTION, questionText);
                        namedPreparedStatement.setString(LOCALE, locale);
                        namedPreparedStatement.setString(VERSION,
                                ChallengeQuestionsConstants.CHALLENGE_QUESTION_SCHEMA_VERSION);
                        namedPreparedStatement.setTimeStamp(CREATED_AT, currentTime, CALENDAR);
                        namedPreparedStatement.setTimeStamp(UPDATED_AT, currentTime, CALENDAR);
                    }), challengeQuestion, false);
                }
            }
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_DATABASE_EXCEPTION_SET_CHALLENGE_QUESTIONS,
                    null, e);
        }
    }

    @Override
    public void deleteChallengeQuestions(ChallengeQuestion[] challengeQuestions, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            for (ChallengeQuestion question : challengeQuestions) {
                validateChallengeQuestionMandatoryParams(question);
                if (StringUtils.isNotEmpty(question.getLocale())) {
                    String locale = validateLocale(question.getLocale());
                    namedJdbcTemplate.executeUpdate(DELETE_CHALLENGE_QUESTION_BY_LOCALE, namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        namedPreparedStatement.setString(QUESTION_SET_ID, question.getQuestionSetId());
                        namedPreparedStatement.setString(QUESTION_ID, question.getQuestionId());
                        namedPreparedStatement.setString(LOCALE, locale);
                    });
                } else {
                    namedJdbcTemplate.executeUpdate(DELETE_CHALLENGE_QUESTION, namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        namedPreparedStatement.setString(QUESTION_SET_ID, question.getQuestionSetId());
                        namedPreparedStatement.setString(QUESTION_ID, question.getQuestionId());
                    });
                }
            }
        } catch (DataAccessException e) {
            throw new IdentityRecoveryException("Error deleting challenge questions in " + tenantDomain, e);
        }
    }

    @Override
    public void deleteChallengeQuestionSet(String challengeQuestionUri, String locale, String tenantDomain)
            throws IdentityRecoveryException {

        try {
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            validateChallengeSetURI(challengeQuestionUri);
            if (StringUtils.isEmpty(locale)) {
                namedJdbcTemplate.executeUpdate(DELETE_CHALLENGE_QUESTION_SET, namedPreparedStatement -> {
                    namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    namedPreparedStatement.setString(QUESTION_SET_ID, challengeQuestionUri);
                });
            } else {
                deleteChallengeQuestionSetByLocale(challengeQuestionUri, tenantId, locale);
            }
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CHALLENGE_SET,
                    challengeQuestionUri, e);
        }
    }

    /**
     * Delete challenge questions by locale.
     *
     * @param questionSetId Challenge question URI.
     * @param tenantId      Tenant ID of the user.
     * @param locale        Locale of the challenge questions.
     * @throws IdentityRecoveryServerException If an error occurs while deleting the challenge questions.
     */
    private void deleteChallengeQuestionSetByLocale(String questionSetId, int tenantId, String locale)
            throws IdentityRecoveryServerException {

        try {
            NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
            namedJdbcTemplate.executeUpdate(DELETE_CHALLENGE_QUESTION_SET_BY_LOCALE, namedPreparedStatement -> {
                namedPreparedStatement.setInt(TENANT_ID, tenantId);
                namedPreparedStatement.setString(QUESTION_SET_ID, questionSetId);
                namedPreparedStatement.setString(LOCALE, locale);
            });
        } catch (DataAccessException e) {
            throw handleServerException(
                    ChallengeQuestionsConstants.ErrorMessages.ERROR_CODE_DATABASE_EXCEPTION_DELETE_CHALLENGE_QUESTIONS,
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
     * @throws DataAccessException             If a data access error occurs while checking the challenge question.
     */
    private boolean isChallengeQuestionExists(ChallengeQuestion challengeQuestion, String tenantDomain)
            throws IdentityRecoveryClientException, DataAccessException {

        validateChallengeQuestionMandatoryParams(challengeQuestion);
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();

        if (StringUtils.isNotEmpty(challengeQuestion.getLocale())) {
            String locale = validateLocale(challengeQuestion.getLocale());
            Integer questionId = namedJdbcTemplate.fetchSingleRecord(GET_CHALLENGE_QUESTION_WITH_LOCALE,
                    (resultSet, rowNumber) -> resultSet.getInt(ID), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        namedPreparedStatement.setString(QUESTION_SET_ID, challengeQuestion.getQuestionSetId());
                        namedPreparedStatement.setString(QUESTION_ID, challengeQuestion.getQuestionId());
                        namedPreparedStatement.setString(LOCALE, locale);
                    });

            return questionId != null;
        }
        Integer questionId = namedJdbcTemplate.fetchSingleRecord(GET_CHALLENGE_QUESTION,
                (resultSet, rowNumber) -> resultSet.getInt(ID), namedPreparedStatement -> {
                    namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    namedPreparedStatement.setString(QUESTION_SET_ID, challengeQuestion.getQuestionSetId());
                    namedPreparedStatement.setString(QUESTION_ID, challengeQuestion.getQuestionId());
                });

        return questionId != null;
    }
}
