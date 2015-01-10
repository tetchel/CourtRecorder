import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/*
    @author Tim Etchells

 */
public class CourtPanel extends JPanel
{
    private BufferedImage bg;
    protected static ArrayList<Play> plays;
    private static CourtConsole cc;
    private static File logfile;
    private static Timer timer;

    //TODO once commands are implemented, have one to toggle this value.
    //private boolean aging = true;
    private static boolean record_unknown = false;
    //t is the number of swing timer ticks
    //the other 3 are self explainatory
    private static int  t         = 0,
                        game_mins = 10,
                        game_secs = 0,
                        quarter   = 1;
    private static String gametime = "Q" + quarter + " " + game_mins + ":" + game_secs + "0";

    /*
       Parameters are: width of the window to draw
       path to the file to log events to
       CourtConsole used to interact with the user
     */
    public CourtPanel(int width, String logPath, CourtConsole console) {
        logfile = new File(logPath);
        initializeLogFile();

        plays = new ArrayList<>();
        cc = console;

        Image court_image = null;
        try {
            //uses the classpath as reference to find the court file. so keep the file in CourtRecord/src
            court_image = ImageIO.read(getClass().getResource("/ball_court_full.png"));
            this.bg = ((BufferedImage)court_image);
        } catch (IOException ioe) {
            System.out.println("Error reading picture from file. Program will exit.");
            ioe.printStackTrace();
            System.exit(2);
        } catch (IllegalArgumentException iae) {
            System.out.println("Picture file not found! Program will exit.");
            iae.printStackTrace();
            System.exit(2);
        }
        width -= cc.getHorizontalSize();
        final int height= (int) (width * ((double) bg.getHeight() / (double) bg.getWidth()));

        court_image = court_image.getScaledInstance(width, height, 2);
        this.bg = new BufferedImage(width, height, 4);
        Graphics2D bgr = this.bg.createGraphics();
        bgr.drawImage(court_image, 0, 0, null);
        bgr.dispose();

        setPreferredSize(new Dimension(width, height));

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Play p = new Play(e.getX(), e.getY());
                plays.add(p);
                parseInput(p);
            }
        });
        //number of times the program's clock has ticked
        timer = new Timer(100, e -> {
            t++;
            if(t % 10 == 0) {
                if(game_secs > 0)
                    game_secs--;
                else if(game_mins > 0) {
                    game_secs = 59;
                    game_mins--;
                }
                else{
                    if(quarter < 4) {
                        quarter++;
                        game_secs = 0;
                        game_mins = 10;
                        stopClock();
                    }
                    else {
                        cc.output("Game over!!");
                        timer.stop();
                    }
                }
            }

            updateGameTime();

            CourtConsole.TimerPanel.timelabel.setText(gametime);

            repaint();
        });

        Timer clickTimer = new Timer(200, e -> {
            for (int i = 0; i < plays.size(); i++) {
                if (plays.get(i).getAge() > 3)
                    plays.remove(i);
                else
                    plays.get(i).incrementAge();
            }
            repaint();
        });

        clickTimer.start();
    }

    private static void updateGameTime() {
        if(game_secs < 10)
            gametime = "Q" + quarter + " " + game_mins + ":" + "0" + game_secs;
        else
            gametime = "Q" + quarter + " " + game_mins + ":" + game_secs;
    }

    private static void stopClock() {
        timer.stop();
        CourtConsole.ButtonPanel.start_stopb.setText("Start");
    }

    private void initializeLogFile()
    {
        final String FIRST_LINE = "Time, Game Time, X, Y, Play Type, Offensive Team, Offense, Defense";

        PrintWriter pw = null;
        BufferedReader br = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
            br = new BufferedReader(new FileReader(logfile));

            if (FIRST_LINE.equals(br.readLine())) {
                return;
            }
            pw = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
        }
        catch (IOException ioe) {
            System.out.println("Error opening logfile for writing at initialization step. Please check path, and ensure no other process is accessing " + logfile.getName() + ". \n" +
                    "Program will now exit.");
            ioe.printStackTrace();
            System.exit(3);
        }
        finally {
            if (br != null)
                try { br.close(); } catch (IOException ioe) {
                    System.err.println("Error closing BufferedReader: ");
                    ioe.printStackTrace();
                }
            else
                System.err.println("BufferedReader for logfile could not be opened, please check file path.");
        }
        pw.println(FIRST_LINE);
        pw.flush();
        pw.close();
    }

    private static void storePlay(Play p)
    {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
        } catch (IOException ioe) {
            System.out.println("Error opening logfile for writing. Please check path. Program will now exit.");
            ioe.printStackTrace();
            System.exit(3);
        }

        DateFormat tf = new SimpleDateFormat("HH:mm:ss");
        //DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();

        if(p.getX() == -1)
            pw.printf(" %s, %s, , , %s, %s, %s, %s\n", gametime, tf.format(cal.getTime()), p.getType(), p.getOTeam(), p.getOPlayer(), p.getDPlayer());
        else
            pw.printf(" %s, %s, %d, %d, %s, %s, %s, %s\n", gametime, tf.format(cal.getTime()), p.getX(), p.getY(), p.getType(), p.getOTeam(), p.getOPlayer(), p.getDPlayer());

        pw.flush();
        pw.close();
    }

    /*
    Divides input into sections and calls parsePlay on each of them
     */
    private static void parseInput(Play p) {
        //the list of plays input this time
        ArrayList<String> input_plays = new ArrayList<>();
        String in = cc.getInput().toLowerCase();
        int start = 0;

        for(int i = 0; i < in.length(); i++) {
            if(in.charAt(i) == ' ') {
                start++;
                continue;
            }
            if(in.charAt(i) == ';' || i == in.length() - 1) {
                input_plays.add(in.substring(start, i+1));
                start = i+1;
            }
        }

        for(String playString : input_plays) {
            Play returned = parsePlay(p, playString);
            if (returned != null)
                storePlay(returned);
        }
    }

    /*
    Returns a play with the correct play type as parsed from input.
    Returns null if input is bad, null plays will not be recorded.
     */
    private static Play parsePlay(Play p, String in) {

        int split = in.indexOf('/');

        char ch = in.charAt(0);
        String  out = "",           //value to be output to user
                onum, dnum;

        onum = parseNumber(in, 0);

        //BEGIN PARSING - first the offensive stuff & play type
        int start_index = 2;
        //if it's a 2 digit number we should start at 3 not 2
        if(!onum.equals("") && (Integer.parseInt(onum)) > 9)
            start_index++;
        //first and second after the number
        char first, second;

        try {
            first = in.charAt(start_index);
        }
        catch (StringIndexOutOfBoundsException e) {
            first = ' ';
        }

        try {
            second = in.charAt(++start_index);
        }
        catch (StringIndexOutOfBoundsException e) {
            second = ' ';
        }

        p.setOPlayer(onum);

        switch (ch) {
            //tipoff

            // offensive team
            case 'q':
                //if(onum.equals("")) {
                    if(first == 'g') {
                        out = out + "Guelph:";
                        p.setOTeam("Guelph");
                    }
                    else if (first == 'o') {
                        out = out + "Opp:";
                        p.setOTeam("Opp");
                    }
                /*
                else{
                    cc.output("Error: Offensive Player # should excluded when inputting offensive team");
                }
                */
                break;

            case 'i':
                if(first == 'l') {
                    out = out + onum + " lost the tipoff";
                    p.setType("Tipoff Loss");
                }
                else if (first == 'w') {
                    out = out + onum + " won the tipoff";
                    p.setType("Tipoff Win");
                }
                else{
                    cc.output("Input error for tipoff, will be recorded as a win");
                    out = out + onum + " won the tipoff";
                    p.setType("Tipoff Win");
                }
                break;
            //shot
            case 's':
                if(!onum.equals(""))
                    out = out + "Offensive Player #" + onum;
                else
                    out = out + "Offense";

                if(in.substring(start_index-1).contains("o")) {
                    out = out + " makes";
                    p.setType("Made shot");
                }
                else if(in.substring(start_index-1).contains("x")) {
                    out = out + " misses";
                    p.setType("Missed shot");
                }
                else {
                    cc.output("Error: " + second + " must be o or x for shots. Event not recorded.");
                    return null;
                }
                /*
                if(first == 'u' ) {
                    out = out + " an uncontested shot";
                }
                else if(first == 'c' ) {
                    out = out + " a contested shot";
                }
                else if(first == 'b' ) {
                    out = out + " was blocked";
                }*/

                if(in.substring(start_index-2).contains("u")) {
                    out = out + " an uncontested shot";
                }
                else if(in.substring(start_index-2).contains("c")) {
                    out = out + " a contested shot";
                }
                else if(in.substring(start_index-2).contains("b")) {
                    out = out + " was blocked";
                }

                break;
            //turnover
            case 't':
                out = out + "#" + onum + " ";
                if(first == 'u') {
                    if(in.substring(start_index-1).contains("o")){
                        out = out + "committed an unforced out-of-bounds";
                        p.setType("Unforced OOB");
                    }
                    else {
                        out = out + "committed an unforced turnover ";
                        p.setType("Unforced turnover");
                    }
                }
                else if(first == 'f') {
                    if(in.substring(start_index-1).contains("o")) {
                        out = out + "was forced out-of-bounds";
                        p.setType("Forced OOB");
                    }
                    else {
                            out = out + "committed a forced turnover";
                            p.setType("Forced turnover");
                    }
                }
                else if(in.substring(start_index-1).contains("o")) {
                    out = out + "went out of bounds";
                    p.setType("Unforced OOB");
                }
                else {
                    out = out + "committed a turnover";
                    p.setType("Turnover");
                }

                //add out of bounds and true steals when you find out where that happens
                break;
            //d-rebound
            case 'd':
                if(!onum.equals("") )
                    out = out + "Player " + onum + " won a defensive rebound";
                p.setType("Defensive rebound");
                //add case for an unknown player winning a rebound
                break;
            //o-rebound
            case 'o':
                if(!onum.equals(""))
                    out = out + "Player " + onum + " won an offensive rebound";
                p.setType("Offensive rebound");
                //same as above
                break;
            //the default case could be switched to record things.
            case 'p':
                break;
            default:
                if(!record_unknown) {
                    out = "Play " + in.substring(0, in.length() - 1) + " not recognized and not recorded.";
                }
                else out = "Play " + in.substring(0, in.length() - 1) + " not recognized and not recorded.";
        }
        //if there is a defensive aspect to this play, we process it here
        if(split != -1 && !out.equals("Play " + in.substring(0, in.length() - 1) + " not recognized and not recorded.")) {
            dnum = parseNumber(in, split);
            p.setDPlayer(dnum);
            //2 players
            //shot that was blocked or contested or not contested
            if (ch == 's') {
                if(first == 'u')
                    out = out + "\n" + "Shot uncontested by Defensive Player # " + dnum;
                else if(first == 'c')
                    out = out + "\n" + "Shot contested by Defensive Player # " + dnum;
                else if(first == 'b')
                    out = out + "\n" + "Shot blocked by Defensive Player # " + dnum;
            }
            //turnover that was forced ONLY
            //HERE WE ASSUME that if there is a defensive player, the turnover WAS FORCED REGARDLESS OF IF F OR U IS PRESENT
            else if (ch == 't' ) {
                if(in.substring(split+1).contains("s")) {
                    out = out + "\n" + "Steal by #" + dnum;
                    p.setType("Forced turnover: Steal");
                }
                else {
                    if(in.substring(split+1).contains("o")) {
                        out = out + "\n" + "Out-of-bounds forced by #" + dnum;
                        p.setType("Forced OOB");
                    }
                    else
                        out = out + "Turnover forced by #" + dnum;
                }
            }
            //foul - player BEFORE / WAS FOULED, player AFTER / WAS THE FOULER
            else if (ch == 'p') {
                if(!onum.equals(dnum))
                    out = onum + " was fouled by #" + dnum;
                else {
                    p.setOPlayer("");
                    out = "# " + dnum + " committed a foul";
                }
                stopClock();
                p.setType("Personal foul");
            }
            else if (ch == 'i') {
                out = out + " against # " + dnum;
            }
            else {
                if(!record_unknown) {
                    out = "Play " + in.substring(0, in.length() - 1) + " not recognized and not recorded.";
                }
                else out = "Play " + in.substring(0, in.length() - 1) + " not recognized and not recorded.";
            }
        }

        if(p.getType().equals(""))
            p.setType("Unknown");

        cc.output(out);

        return p;
    }

    /*
    Receives a string as input
    Checks the indices i and i+1 for numbers and returns the number as one number
    There should be one character before the player number in any play string
     */
    private static String parseNumber(String in, int i) {

        String num = "";
        char ch;
        try {
            ch = in.charAt(++i);
            if (ch >= 48 && ch <= 57) {
                num = num + ch;
            }
        }
        catch(StringIndexOutOfBoundsException se) {
            return "";
        }
        try {
            ch = in.charAt(i + 1);
            if (ch >= 48 && ch <= 57) {
                num = num + ch;
            }
        }
        catch(StringIndexOutOfBoundsException se) {
            return num;
        }

        //cc.output("num parsed: " + num);
        if(!num.equals("")) return num;
        else return "";
    }
    
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(this.bg, 0, 0, null);
        final int LINE_LENGTH = 8;
        for (Play p : plays) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.draw(new Line2D.Double(p.getX() - LINE_LENGTH, p.getY(), p.getX() + LINE_LENGTH, p.getY()));
            g2.draw(new Line2D.Double(p.getX(), p.getY() - LINE_LENGTH, p.getX(), p.getY() + LINE_LENGTH));
        }
    }

    /*
        @author Tim Etchells
        Represents a JComponent which is composed of four inner components - a scroll pane to show output and a text field,
        also a label to display time
        and a panel to display a series of buttons
        for the user to type in. Clicking on the picture will grab the input from the text field.
     */
    protected static class CourtConsole extends JPanel {

        private final JTextArea jta;
        private final JTextField jtf;
        private final int HORIZONTAL_SIZE = 40;
        private final File INPUT_LOG = new File("user_input.txt");

        //TODO width is still unused here, should use it to scale things a little better
        public CourtConsole(int width) {
            final int FACTOR = 14;                  //scaling numbers, determined experimentally (these are sketchy)

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            jta = new JTextArea(FACTOR, HORIZONTAL_SIZE-15);

            JScrollPane jsp = new JScrollPane(jta);

            //these two lines cause the scrollpane to scroll down automatically
            DefaultCaret caret = (DefaultCaret) jta.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

            jtf = new JTextField(HORIZONTAL_SIZE);

            output("Enter plays in the box below. \n" +
                    "Click on the picture or press RETURN to process the input.");

            //timelabel.setFont(new Font("Arial", Font.BOLD, 20));

            add(new TimerPanel());
            add(jsp);
            add(jtf);
            add(new ButtonPanel(this));

            jtf.addActionListener( e -> {
                //TODO this will make ALL plays in the current input locationless
                Play p = new Play(-1, -1);
                plays.add(p);
                parseInput(p);
            });

            jtf.grabFocus();
            jta.setEditable(false);

        }
        public String getInput() {
            String in = jtf.getText();
            if(!in.equals(""))
                storePlainText(in);
            jtf.setText("");

            jta.append(">>  " + in + "\n");

            return in;
        }

        private void storePlainText(String s) {
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new BufferedWriter(new FileWriter(INPUT_LOG, true)));
            } catch (IOException ioe) {
                System.out.println("Error opening input log for writing. Program will now exit.");
                ioe.printStackTrace();
                System.exit(3);
            }
            pw.println(s);

            pw.flush();
            pw.close();
        }

        public void output(String s) {
            jta.append(s + "\n");
        }

        public int getHorizontalSize() {
            return 12*HORIZONTAL_SIZE;
        }

        private static class TimerPanel extends JComponent {
            private final static JLabel timelabel = new JLabel(gametime);

            public TimerPanel() {
                timelabel.setFont(new Font("Arial", Font.BOLD, 20));

                setLayout(new FlowLayout(FlowLayout.LEFT));
                add(timelabel);
            }
        }

        private static class ButtonPanel extends JComponent {
            private final static JButton start_stopb = new JButton("Start Time");
            private final static JButton update_timeb = new JButton("Update Time");

            public ButtonPanel(CourtConsole cc) {
                start_stopb.addActionListener(e -> {
                    if(timer.isRunning()) {
                        timer.stop();
                        start_stopb.setText("Start Time");
                    }
                    else {
                        timer.start();
                        start_stopb.setText("Stop Time");
                    }
                });

                update_timeb.addActionListener(e -> {
                    timer.stop();
                    start_stopb.setText("Start Time");

                    JOptionPane command_pane = new JOptionPane();
                    command_pane.setPreferredSize(new Dimension(160, 90));

                    JTextField inputField = new JTextField();
                    inputField.requestFocus();
                    inputField.setText("Q/MM:SS");

                    //note that the / (setting quarter) is optional, but the : is not.
                    JOptionPane.showMessageDialog(null, inputField, "Enter game time", JOptionPane.PLAIN_MESSAGE);

                    String newtime = inputField.getText();
                    int colon = newtime.indexOf(":");
                    int slash = newtime.indexOf("/");
                    int newmins, newsecs, newq = 1;
                    try {
                        if(slash != -1) {
                            newq = Integer.parseInt(newtime.substring(0,slash));

                            newmins = Integer.parseInt(newtime.substring(slash+1, colon));
                        }
                        else
                            newmins = Integer.parseInt(newtime.substring(0, colon));

                        newsecs = Integer.parseInt(newtime.substring(colon+1));

                        if(newq > 4 || newq < 1 || newmins > 10 || newmins < 0 || newsecs > 59 || newsecs < 0)
                            throw new NumberFormatException();

                        quarter = newq;
                        game_mins = newmins;
                        game_secs = newsecs;

                        updateGameTime();
                        CourtConsole.TimerPanel.timelabel.setText(gametime);
                    } catch ( NumberFormatException | StringIndexOutOfBoundsException ex ) {
                        cc.output("Invalid time entered");
                    }
                });

                setLayout(new FlowLayout(FlowLayout.LEFT));
                add(start_stopb);
                add(update_timeb);
            }
        }
    }
}
