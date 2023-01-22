package at.fhtw.bic.ode_project.Service;


public interface GameObserver {
    public void setGuesserMode();
    public void setDrawerMode();
    public void resetMode();
    public void outputWords(String words);
    public void setDisplayWord(String word);
    public void clearCanvas();
    public void transformMessageToLine(String message);
}
