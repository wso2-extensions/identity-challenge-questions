package org.wso2.carbon.identity.challenge.questions.recovery.model;

/**
 * encapsulates challenge questions data
 */
public class ChallengeQuestion {

    private String question;

    private String questionId;

    private String questionSetId;

    private String locale;

    public ChallengeQuestion() {
        //default constructor
    }

    public ChallengeQuestion(String questionSetId, String question) {
        this.question = question;
        this.questionSetId = questionSetId;
    }

    public ChallengeQuestion(String questionSetId, String questionId, String question, String locale) {
        this.questionSetId = questionSetId;
        this.questionId = questionId;
        this.question = question;
        this.locale = locale;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionSetId() {
        return questionSetId;
    }

    public void setQuestionSetId(String questionSetId) {
        this.questionSetId = questionSetId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
