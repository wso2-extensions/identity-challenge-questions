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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import static org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionsConstants.CHALLENGE_QUESTIONS_STORAGE_CONFIG;

/**
 * Factory class to create instances of ChallengeQuestionsDAO based on the configured storage type.
 */
public class ChallengeQuestionPersistenceManagerFactory {

    private static final Log LOG = LogFactory.getLog(ChallengeQuestionPersistenceManagerFactory.class);
    private static String CHALLENGE_QUESTIONS_STORAGE_TYPE =
            IdentityUtil.getProperty(CHALLENGE_QUESTIONS_STORAGE_CONFIG);
    private static final String HYBRID = "hybrid";
    private static final String REGISTRY = "registry";

    public ChallengeQuestionDAO getChallengeQuestionPersistenceManager() {

        ChallengeQuestionDAO challengeQuestionDAO = new JDBCChallengeQuestionDAOImpl();

        if (StringUtils.isNotBlank(CHALLENGE_QUESTIONS_STORAGE_TYPE)) {
            switch (CHALLENGE_QUESTIONS_STORAGE_TYPE) {
                case HYBRID:
                    // Initialize Hybrid Challenge Question storage.
                    challengeQuestionDAO = new HybridChallengeQuestionDAOImpl();
                    LOG.info("Hybrid Challenge Question storage initialized.");
                    break;
                case REGISTRY:
                    // Initialize Registry Challenge Question storage.
                    challengeQuestionDAO = new RegistryChallengeQuestionDAOImpl();
                    LOG.warn("Registry based Challenge Question storage initialized.");
                    break;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Challenge Question DAO initialized with the type: " + challengeQuestionDAO.getClass());
        }
        return challengeQuestionDAO;
    }
}
