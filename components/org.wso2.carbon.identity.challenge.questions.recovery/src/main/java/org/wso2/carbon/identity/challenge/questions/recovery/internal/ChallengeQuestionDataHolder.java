package org.wso2.carbon.identity.challenge.questions.recovery.internal;

import org.wso2.carbon.identity.api.user.recovery.commons.UserAccountRecoveryServiceDataHolder;
import org.wso2.carbon.identity.recovery.services.password.PasswordRecoveryManager;
import org.wso2.carbon.user.core.service.RealmService;

public class ChallengeQuestionDataHolder {

    private RealmService realmService;
    private static ChallengeQuestionDataHolder instance = new ChallengeQuestionDataHolder();

    public static ChallengeQuestionDataHolder getInstance() {
        return instance;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }
}
