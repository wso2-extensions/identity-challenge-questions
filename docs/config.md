# Configuring Challenge Questions Connector

This guide provides instructions on how to configure the challenge questions connector in WSO2 Identity Server.

## Definitions

Before proceeding with the configuration, familiarize yourself with the following directory placeholders used in this document:

- **`<IS_HOME>`** or **`<PRODUCT_HOME>`**: The root directory of your WSO2 Identity Server installation. For example:
  - Linux/Mac: `/home/user/wso2is-7.0.0` or `/opt/wso2is-7.0.0`
  - Windows: `C:\Program Files\WSO2\IS\7.0.0`

- **`<CONNECTOR_HOME>`**: The directory created when you extract the challenge questions connector zip file. This is a temporary directory used during installation.

## Installation Steps

1. Download the connector from [WSO2 Connector Store](https://store.wso2.com/connector/identity-challenge-questions).

2. Navigate to your `<PRODUCT_HOME>` directory, paste the `wso2is-challenge-questions-connector-x.x.x.zip` file downloaded from the WSO2 Connector Store, and extract it. The extracted folder will be referred to as `<CONNECTOR_HOME>` in the rest of this document.
3. Execute the database scripts in the `<CONNECTOR_HOME>/dbscripts` folder against the identity database.

4. If you are using MacOS/Ubuntu, navigate to `<CONNECTOR_HOME>` and execute the following commands:

   ```bash
   chmod u+r+x setup.sh
   ./setup.sh
   ```
   
   **For Windows or manual installation:**

   I. Navigate to `<CONNECTOR_HOME>/dropins` and copy the JAR files to `<PRODUCT_HOME>/repository/components/dropins`.

   II. Navigate to `<CONNECTOR_HOME>/api` and copy the JAR files to `<PRODUCT_HOME>/repository/deployment/server/webapps/api/WEB-INF/lib/`.

5. Navigate back to `<PRODUCT_HOME>` and delete the `<CONNECTOR_HOME>` folder. The connector is now successfully installed.

## Configuration

6. Add the following configurations to the `<PRODUCT_HOME>/repository/conf/deployment.toml` file:
   - **For IS-7.2.0 and later:**

     !!! Note
         SOAP APIs are disabled from IS 7.2.0 onwards. To access the management console at `https://localhost:9443/carbon/`, you must explicitly enable admin services by setting `enable_admin_services = true`.

     ```toml
     [server]
     hide_menu_items = []
     enable_admin_services = true

     [connector.challenge_questions]
     enable = true
     ```

   - **For versions below IS-7.2.0:**

     ```toml
     [server]
     hide_menu_items = []

     [connector.challenge_questions]
     enable = true
     ```

## API Configuration

7. Follow these steps based on your Identity Server version:

   ### For IS-7.1.0 and Later

   - Open the `web.xml` file located at `<PRODUCT_HOME>/repository/deployment/server/webapps/api/WEB-INF/web.xml`.
   - Under the `<param-value>` in `jaxrs.serviceClasses` of the `<servlet-name>ServerV1ApiServlet</servlet-name>` tag, add the following class:
     ```
     org.wso2.carbon.identity.rest.api.server.challenge.v1.ChallengesApi
     ```

   - Under the `<param-value>` in `jaxrs.serviceClasses` of the `<servlet-name>UserV1Servlet</servlet-name>` tag, add the following classes:
     ```
     org.wso2.carbon.identity.rest.api.user.challenge.v1.MeApi,
     org.wso2.carbon.identity.rest.api.user.challenge.v1.UserIdApi
     ```

   - Under the `<param-value>` in `jaxrs.serviceClasses` of the `<servlet-name>IdentityRecoveryV0_9ApiServlet</servlet-name>` tag, add the following classes:
     ```
     org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionApi,
     org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApi,
     org.wso2.carbon.identity.challenge.questions.recovery.endpoint.ValidateAnswerApi
     ```

   ### For Versions Below IS-7.1.0

   - Open the `beans.xml` file located at `<PRODUCT_HOME>/repository/deployment/server/webapps/api/WEB-INF/beans.xml`.
   - Add the following imports to the configuration bean:
     ```xml
     <import resource="classpath:META-INF/cxf/user-challenge-v1-cxf.xml"/>
     <import resource="classpath:META-INF/cxf/challenge-questions-user-recovery-v0-9-cxf.xml"/>
     <import resource="classpath:META-INF/cxf/challenge-server-v1-cxf.xml"/>
     ```

   - Under the `<jaxrs:server id="server" address="/server/v1">` tag, add the following class:
     ```xml
     <bean class="org.wso2.carbon.identity.rest.api.server.challenge.v1.ChallengesApi"/>
     ```

   - Under the `<jaxrs:server id="users" address="/users/v1">` tag, add the following classes:
     ```xml
     <bean class="org.wso2.carbon.identity.rest.api.user.challenge.v1.MeApi"/>
     <bean class="org.wso2.carbon.identity.rest.api.user.challenge.v1.UserIdApi"/>
     ```

   - Under the `<jaxrs:server id="recovery" address="/identity/recovery/v0.9">` tag, add the following classes:
     ```xml
     <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionApi"/>
     <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApi"/>
     <bean class="org.wso2.carbon.identity.challenge.questions.recovery.endpoint.ValidateAnswerApi"/>
     ```

## Server Restart and Claims Configuration

8. Restart the WSO2 Identity Server to apply the changes.

9. Log in to the WSO2 Identity Server Console.

## Configure Claims

10. Configure the following user attributes (claims):

    I. Navigate to **User Attributes & Stores** and select **Attributes**.

    II. Select **Attributes** under **Manage Attributes** and click **New Attribute**.
    
    III. Specify `primaryChallengeQuestion` as the **Attribute Name** and add `Primary Challenge Question` as the **Attribute Display Name**.
    
    IV. Create the following additional claims in the same manner:
    
   !!! Important
       The **Mapped attributes** must match the names used in the protocol mappings.

   | Attribute Name        | Attribute Display Name | Mapped Attributes       |
   |-----------------------|------------------------|-------------------------|
   | challengeQuestionUris | Challenge Question     | challengeQuestionUris   |
   | challengeQuestion1    | Challenge Question1    | firstChallenge          |
   | challengeQuestion2    | Challenge Question2    | secondChallenge         |

## Enable Challenge Questions Feature

11. Access the management console at `https://localhost:9443/carbon/` and enable the challenge questions feature.

    !!! Note
        The challenge questions feature can only be configured from the Carbon Console. Follow the detailed steps described in the [Recover password via Challenge Questions](enable-password-reset-via-challenge-questions.md) guide to complete the configuration.

## Next Steps

After completing the configuration, refer to the [Recover password via Challenge Questions](enable-password-reset-via-challenge-questions.md) guide to:
- Enable password reset via challenge questions
- Configure challenge questions for users
- Test the password recovery flow
