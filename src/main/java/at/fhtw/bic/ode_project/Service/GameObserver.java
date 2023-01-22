package at.fhtw.bic.ode_project.Service;


public interface GameObserver {
    public void setGuesserMode();
    public void setDrawerMode();
    public void resetMode();
    public void setWinMode();

    public void outputWords(String words);
    public void setDisplayWord(String word);
    public void setChoosableWords(String word1, String word2, String word3);
    public void clearCanvas();
    public void clearText();
    public void transformMessageToLine(String message);
}
