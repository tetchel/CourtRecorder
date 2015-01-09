import javax.swing.*;
import java.awt.*;

/*
    @author Tim Etchells
 */
public class CourtRecorderMain extends JPanel
{
    public static void main(String[] a)
    {
        if (a.length != 2) {
            System.out.println("Usage: \"java -jar BallCourtMain.java HORIZONTAL_SIZE LOG_FILE\" \n" +
                    "HORIZONTAL_SIZE is the desired HORIZONTAL resolution to fit your screen.\n" +
                    "LOG_FILE is the file in the same directory as the .jar in which to log mouse clicks. .CSV is the optimal format.\n");
            System.exit(1);
        }
        final int width = Integer.parseInt(a[0]);

        final CourtPanel.CourtConsole cc = new CourtPanel.CourtConsole(width);
        //final CourtPanel.CourtConsole
        final CourtPanel p = new CourtPanel(width, a[1], cc);
        final JFrame f = new JFrame("Court Recorder");

        f.setLayout(new FlowLayout());
        f.add(p);
        f.add(cc);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setBackground(Color.BLACK);
        f.setVisible(true);
    }
}