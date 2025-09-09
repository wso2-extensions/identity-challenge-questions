### **Configuring challenge questions connector**

1. Download the connector from [WSO2 Connector Store](https://store.wso2.com/connector/identity-challenge-questions).
2. Navigate to the <PRODUCT_HOME>, paste the wso2is-challenge-questions-connector-x.x.x.zip file downloaded from the WSO2 Connector Store and extract it. The extracted folder will be referred to as <CONNECTOR_HOME> in the rest of this document.
3. Execute the database scripts in <CONNECTOR_HOME>/dbscripts folder against identity DB.
4. If you are using MacOS/Ubuntu, navigate to <CONNECTOR_HOME> and execute the following commands.

    ```
    chmod u+r+x setup.sh
    ./setup.sh
    ```
   
      Else,

      I. Navigate to <CONNECTOR_HOME>/dropins and copy the jars in that location to <PRODUCT_HOME>/repository/components/dropins.

      II. Navigate to <CONNECTOR_HOME>/api and copy the jars in that location to <PRODUCT_HOME>/repository/deployment/server/webapps/api/WEB-INF/lib/

5. Navigate back to <PRODUCT_HOME> and delete the <CONNECTOR_HOME> folder. Now you have successfully installed the connector.
6. Add the following configs to the deployment.toml file
    - **IS-7.2.0 and later**

        !!! Note: SOAP APIs are disabled from IS 7.2.0 onwards. To access the management console at https://localhost:9443/carbon/ , you must explicitly enable admin services by setting `disable_admin_services = false`.

        ```
        [server]
        hide_menu_items = []
        disable_admin_services = false

        [connector.challenge_questions]
        enable = true
        ```
    - **below IS-7.2.0 versions**
        ```
        [server]
        hide_menu_items = []

        [connector.challenge_questions]
        enable = true
        ```

7. Please follow these steps based on your identity server version.
   - **IS-7.1.0 onwards**
     - Open the web.xml file located in the repository/deployment/server/webapps/api/WEB-INF directory.
     - Under the **<param-value>** in **jaxrs.serviceClasses** of **<servlet-name>ServerV1ApiServlet</servlet-name>** tag, add the following class:`org.wso2.carbon.identity.rest.api.server.challenge.v1.ChallengesApi`
     - Under the **<param-value>** in **jaxrs.serviceClasses** of **<servlet-name>UserV1Servlet</servlet-name>** tag, add the following classes:
      ```
      org.wso2.carbon.identity.rest.api.user.challenge.v1.MeApi,
      org.wso2.carbon.identity.rest.api.user.challenge.v1.UserIdApi
      ```
     - Under the **<param-value>** in **jaxrs.serviceClasses** of **<servlet-name>IdentityRecoveryV0_9ApiServlet</servlet-name>** tag, add the following classes:
      ```
      org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionApi,
      org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApi,
      org.wso2.carbon.identity.challenge.questions.recovery.endpoint.ValidateAnswerApi
      ```
   - **below IS-7.1.0 versions**
     - Open the beans.xml file located in the repository/deployment/server/webapps/api/WEB-INF directory.
     - Add the following imports to the configuration bean.
      ```
      <import resource="classpath:META-INF/cxf/user-challenge-v1-cxf.xml"/>
      <import resource="classpath:META-INF/cxf/challenge-questions-user-recovery-v0-9-cxf.xml"/>
      <import resource="classpath:META-INF/cxf/challenge-server-v1-cxf.xml"/>
      ```
     - Under the **<jaxrs:server id="server" address="/server/v1">** tag, add the following class:
     `<bean class="org.wso2.carbon.identity.rest.api.server.challenge.v1.ChallengesApi"/>`
     - Under the **<jaxrs:server id="server" address="/server/v1">** tag, add the following class:
       ```
       <bean class="org.wso2.carbon.identity.rest.api.user.challenge.v1.MeApi"/>
       <bean class="org.wso2.carbon.identity.rest.api.user.challenge.v1.UserIdApi"/>
       ```
     - Under the **<jaxrs:server id="recovery" address="/identity/recovery/v0.9">** tag, add the following classes:
       ```
       <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionApi"/>
       <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApi"/>
       <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.ValidateAnswerApi"/>
       ```
8. Now restart the server and login to the console.
9. Configure the following claims

    I. Go to `User Attributes & Stores` and select `Attributes`. 

    II. Select `Attributes` under `Manage Attributes` and select `New Attribute`.  
    III. specify `primaryChallengeQuestion` as the `Attribute Name` and add `Primary Challenge Question` as the `Attribute Display Name`.
    Similarly create the following claims as well.
    
    !!! Important: `Mapped attributes` must match the names used in the protocol mappings.

    | Attribute Name        | Attribute Display Name | Mapped attributes |
    |-----------------------|------------------------|-------------------|
    | challengeQuestionUris | Challenge Question | challengeQuestionUris|
    | challengeQuestion1 | Challenge Question1 | firstChallenge|
    | challengeQuestion2 | Challenge Question2 | secondChallenge|

10. Login to the management console via https://localhost:9443/carbon/ and enable the challenge questions feature by following the steps described in the [Recover password via Challenge Questions](/docs/enable-password-reset-via-challenge-questions.md).

> **Note:**
The challenge questions feature can only be configured from the Carbon Console. Hence, please enable the feature by following the steps described in the [Recover password via Challenge Questions](/docs/enable-password-reset-via-challenge-questions.md).
