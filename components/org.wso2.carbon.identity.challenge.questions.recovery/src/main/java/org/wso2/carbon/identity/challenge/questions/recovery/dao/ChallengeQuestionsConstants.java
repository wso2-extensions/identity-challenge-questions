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

package org.wso2.carbon.identity.challenge.questions.recovery.dao;

/**
 * Constants related to Challenge Questions.
 */
public class ChallengeQuestionsConstants {

    public static final String CHALLENGE_QUESTIONS_STORAGE_CONFIG = "DataStorageType.ChallengeQuestions";
    public static final String CHALLENGE_QUESTION_SCHEMA_VERSION = "1.0.0";

    public static class ChallengeQuestionTableColumns {

        public static final String ID = "ID";
        public static final String TENANT_ID = "TENANT_ID";
        public static final String QUESTION_SET_ID = "QUESTION_SET_ID";
        public static final String QUESTION_ID = "QUESTION_ID";
        public static final String LOCALE = "LOCALE";
        public static final String QUESTION = "QUESTION";
        public static final String VERSION = "VERSION";
        public static final String CREATED_AT = "CREATED_AT";
        public static final String UPDATED_AT = "UPDATED_AT";
    }

    public static class SQLQueries {

        public static final String GET_CHALLENGE_QUESTIONS_BY_TENANT_ID =
                "SELECT QUESTION_SET_ID, QUESTION_ID, LOCALE, QUESTION " +
                        "FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID;";

        public static final String GET_CHALLENGE_QUESTIONS_BY_TENANT_ID_LOCALE =
                "SELECT QUESTION_SET_ID, QUESTION_ID, QUESTION " +
                        "FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; AND LOCALE = :LOCALE;";

        public static final String ADD_CHALLENGE_QUESTION =
                "INSERT INTO IDN_CHALLENGE_QUESTION " +
                        "(TENANT_ID, QUESTION_SET_ID, QUESTION_ID, QUESTION, LOCALE, VERSION, CREATED_AT, UPDATED_AT) " +
                        "VALUES (:TENANT_ID;, :QUESTION_SET_ID;, :QUESTION_ID;, :QUESTION;, :LOCALE;, :VERSION;, " +
                        ":CREATED_AT;, :UPDATED_AT;)";

        public static final String UPDATE_CHALLENGE_QUESTION =
                "UPDATE IDN_CHALLENGE_QUESTION " +
                        "SET QUESTION = :QUESTION; , UPDATED_AT = :UPDATED_AT; " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND QUESTION_ID = :QUESTION_ID; " +
                        "AND LOCALE = :LOCALE;";

        public static final String DELETE_CHALLENGE_QUESTION_BY_LOCALE =
                "DELETE FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND QUESTION_ID = :QUESTION_ID; " +
                        "AND LOCALE = :LOCALE;";

        public static final String DELETE_CHALLENGE_QUESTION =
                "DELETE FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND QUESTION_ID = :QUESTION_ID; ";

        public static final String DELETE_CHALLENGE_QUESTION_SET =
                "DELETE FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; ";

        public static final String DELETE_CHALLENGE_QUESTION_SET_BY_LOCALE =
                "DELETE FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND LOCALE = :LOCALE;";

        public static final String GET_CHALLENGE_QUESTION =
                "SELECT ID FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND QUESTION_ID = :QUESTION_ID;";

        public static final String GET_CHALLENGE_QUESTION_WITH_LOCALE =
                "SELECT ID FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID; " +
                        "AND QUESTION_SET_ID = :QUESTION_SET_ID; " +
                        "AND QUESTION_ID = :QUESTION_ID; " +
                        "AND LOCALE = :LOCALE;";

        public static final String GET_CHALLENGE_QUESTION_SET_ID =
                "SELECT DISTINCT QUESTION_SET_ID FROM IDN_CHALLENGE_QUESTION " +
                        "WHERE TENANT_ID = :TENANT_ID;";
    }

    public enum ErrorMessages {

        ERROR_CODE_DATABASE_EXCEPTION_GET_CHALLENGE_QUESTIONS("20001", "Error while getting challenge question"),
        ERROR_CODE_DATABASE_EXCEPTION_SET_CHALLENGE_QUESTIONS("20002", "Error while setting challenge question"),
        ERROR_CODE_ERROR_DELETING_CHALLENGE_SET("20057", "Error when deleting challenge question set %s."),
        ERROR_CODE_DATABASE_EXCEPTION_DELETE_CHALLENGE_QUESTIONS("20058",
                "Error while deleting challenge question of locale %s in set %s");

        private final String code;
        private final String message;

        private ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return this.code;
        }

        public String getMessage() {

            return this.message;
        }

        public String toString() {

            return this.code + " - " + this.message;
        }
    }
}
