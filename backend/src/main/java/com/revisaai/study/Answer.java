package com.revisaai.study;

public class Answer {

    private String questionId;
    private String respostaUsuario;
    private boolean correta;
    private String area;

    public Answer() {
    }

    public Answer(String questionId, String respostaUsuario, boolean correta, String area) {
        this.questionId = questionId;
        this.respostaUsuario = respostaUsuario;
        this.correta = correta;
        this.area = area;
    }

    public String getQuestionId() { return questionId; }
    public String getRespostaUsuario() { return respostaUsuario; }
    public boolean isCorreta() { return correta; }
    public String getArea() { return area; }
}
