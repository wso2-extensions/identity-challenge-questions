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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionDAO;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.ChallengeQuestionPersistenceManagerFactory;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.HybridChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.JDBCChallengeQuestionDAOImpl;
import org.wso2.carbon.identity.challenge.questions.recovery.dao.RegistryChallengeQuestionDAOImpl;


import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * This class tests the methods of the ChallengeQuestionPersistenceManagerFactory class.
 */
public class ChallengeQuestionPersistenceManagerFactoryTest {

    private ChallengeQuestionPersistenceManagerFactory factory;

    @BeforeMethod
    public void setUp() {

        URL root = this.getClass().getClassLoader().getResource(".");
        File file = new File(root.getPath());
        System.setProperty("carbon.home", file.getAbsolutePath());
        factory = new ChallengeQuestionPersistenceManagerFactory();
    }

    @AfterMethod
    public void tearDown() throws Exception {

        setPrivateStaticField(ChallengeQuestionPersistenceManagerFactory.class, "CHALLENGE_QUESTIONS_STORAGE_TYPE", "");
        factory = null;
    }

    @Test(dataProvider="storageTypeProvider")
    public void testGetSAMLServiceProviderPersistenceManagerWithDefaultStorage(String storageValue) throws Exception {

        setPrivateStaticField(ChallengeQuestionPersistenceManagerFactory.class, "CHALLENGE_QUESTIONS_STORAGE_TYPE", storageValue);
        ChallengeQuestionDAO challengeQuestionDAO = factory.getChallengeQuestionPersistenceManager();
        assertTrue(challengeQuestionDAO instanceof JDBCChallengeQuestionDAOImpl);
    }

    @Test
    public void testGetSAMLServiceProviderPersistenceManagerWithRegistryStorage() throws Exception {

        setPrivateStaticField(ChallengeQuestionPersistenceManagerFactory.class, "CHALLENGE_QUESTIONS_STORAGE_TYPE", "registry");
        ChallengeQuestionDAO challengeQuestionDAO = factory.getChallengeQuestionPersistenceManager();
        assertTrue(challengeQuestionDAO instanceof RegistryChallengeQuestionDAOImpl);
    }

    @Test
    public void testGetSAMLServiceProviderPersistenceManagerWithHybridStorage() throws Exception {

        setPrivateStaticField(ChallengeQuestionPersistenceManagerFactory.class, "CHALLENGE_QUESTIONS_STORAGE_TYPE", "hybrid");
        ChallengeQuestionDAO challengeQuestionDAO = factory.getChallengeQuestionPersistenceManager();
        assertTrue(challengeQuestionDAO instanceof HybridChallengeQuestionDAOImpl);
    }

    private void setPrivateStaticField(Class<?> clazz, String fieldName, Object newValue)
            throws NoSuchFieldException, IllegalAccessException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, newValue);
    }

    @DataProvider(name = "storageTypeProvider")
    public Object[][] provideChallengeQuestionStorageType() {
        return new Object[][] {
                {"database"},
                {"Database"},
                {"REGISTRY"},
                {null},
                {" "}
        };
    }
}
