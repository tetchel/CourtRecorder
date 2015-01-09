/*
    @author Tim Etchells
 */

public class Play
{
    private int age = 0;
    private int x;
    private int y;
    private String  type = "",
                    oplayer = "",
                    dplayer = "";

    private boolean clickable = true;

    public Play(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getAge() {
        return this.age;
    }

    public void incrementAge() {
        this.age++;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String s) {
        this.type = s;
        //if it is a non-clickable type, set it as such
    }

    public void setOPlayer(String s) {
        this.oplayer = s;
    }

    public String getOPlayer() {
        return this.oplayer;
    }

    public void setDPlayer(String s) {
        this.dplayer = s;
    }

    public String getDPlayer() {
        return this.dplayer;
    }
}