import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;

public class HuntTheWumpus extends JPanel implements KeyListener {
    //===============================================================================
    //                       Constants area                                         =
    //===============================================================================
    
    // Set up the display
    private static final int SCREEN_WIDTH = 870;
    private static final int SCREEN_HEIGHT = 760;
    
    // Constants for directions
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    
    // Colour definitions
    private static final Color BROWN = new Color(82, 64, 51);
    private static final Color BLACK = Color.BLACK;
    private static final Color RED = new Color(138, 7, 7);
    private static final Color WHITE = Color.WHITE;
    
    // Mechanic settings
    private static final int NUM_BATS = 2;
    private static final int NUM_PITS = 1;
    private static final int NUM_ARROWS = 5;
    
    //===============================================================================
    //                       Instance Variables                                     =
    //===============================================================================
    
    // Game state variables
    private int player_pos;
    private int wumpus_pos;
    private int num_arrows = 3;
    private final boolean mobile_wumpus = true;
    private final int wumpus_move_chance = 1;
    private boolean hit = false;
    private boolean running = true;
    
    // Timer variables
    private final int countdown_time = 120;
    private final long start_ticks;
    
    // Collections
    private final List<Integer> bats_list = new ArrayList<>();
    private final List<Integer> pits_list = new ArrayList<>();
    private final List<Integer> arrows_list = new ArrayList<>();
    
    // Cave mapping
    private final Map<Integer, int[]> cave = new HashMap<>();
    
    // Images
    private BufferedImage bat_img;
    private BufferedImage player_img;
    private BufferedImage wumpus_img;
    private BufferedImage dead_wumpus_image;
    
    // Sounds
    private Clip footstep_sound;
    private Clip jumpscare_sound;
    private Clip ambient_sound;
    private Clip breathing_sound;
    private Clip shoot_sound;
    private Clip wumpus_death_sound;
    private Clip blinded_sound;
    private Clip wumpus_move_sound;
    
    // Font
    private final Font font;
    
    // Main frame
    private JFrame frame;
    private Timer gameTimer;
    
    public HuntTheWumpus() {
        initializeCave();
        loadImages();
        loadSounds();
        setupUI();
        printInstructions();
        
        // Wait for user input
        JOptionPane.showMessageDialog(null, "Press OK to begin.");
        
        start_ticks = System.currentTimeMillis();
        font = new Font("Arial", Font.PLAIN, 35);
        resetGame();
        startGameLoop();
    }
    
    //===============================================================================
    //                       Functions Area                                         =
    //===============================================================================
    
    private boolean checkNeighborRooms(int pos, List<Integer> item_list) {
        int[] exits = cave.get(pos);
        for (int exit : exits) {
            if (item_list.contains(exit)) {
                return true;
            }
        }
        return false;
    }
    
    /** Draws the room in the back buffer
     */
    private void drawRoom(int pos, Graphics2D g2d) {
        int[] exits = cave.get(pos);
        if (exits == null) {
            System.out.println("No exits found for position: " + pos);
            return;
        }
        
        // Clear screen
        g2d.setColor(BLACK);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // Draw the room circle in brown
        int circle_radius = (int)((SCREEN_WIDTH / 2) * 0.65);
        g2d.setColor(BROWN);
        g2d.fillOval(SCREEN_WIDTH/2 - circle_radius, SCREEN_HEIGHT/2 - circle_radius, 
                     circle_radius * 2, circle_radius * 2);
        
        // Next draw all exits from the room
        if (exits[LEFT] > 0) {
            // Draw left exit
            int left = 0;
            int top = SCREEN_HEIGHT/2 - 40;
            g2d.fillRect(left, top, SCREEN_WIDTH/4, 80);
        }
        if (exits[RIGHT] > 0) {
            // Draw right exit
            int left = SCREEN_WIDTH - (SCREEN_WIDTH/4);
            int top = SCREEN_HEIGHT/2 - 40;
            g2d.fillRect(left, top, SCREEN_WIDTH/4, 80);
        }
        if (exits[UP] > 0) {
            // Draw top exit
            int left = SCREEN_WIDTH/2 - 40;
            int top = 0;
            g2d.fillRect(left, top, 80, SCREEN_HEIGHT/4);
        }
        if (exits[DOWN] > 0) {
            // Draw bottom exit
            int left = SCREEN_WIDTH/2 - 40;
            int top = SCREEN_HEIGHT - (SCREEN_WIDTH/4);
            g2d.fillRect(left, top, 80, SCREEN_HEIGHT/4);
        }
        
        // Find out if bats, pits or a Wumpus is near
        boolean bats_near = checkNeighborRooms(player_pos, bats_list);
        boolean pit_near = checkNeighborRooms(player_pos, pits_list);
        List<Integer> wumpus_check = Arrays.asList(wumpus_pos, -1);
        boolean wumpus_near = checkNeighborRooms(player_pos, wumpus_check);
        
        // Draw the pit in black if it is present
        if (pits_list.contains(player_pos)) {
            circle_radius = (int)((SCREEN_WIDTH / 2) * 0.5);
            g2d.setColor(BLACK);
            g2d.fillOval(SCREEN_WIDTH/2 - circle_radius, SCREEN_HEIGHT/2 - circle_radius, 
                         circle_radius * 2, circle_radius * 2);
        }
        
        // Draw the player
        g2d.drawImage(player_img, SCREEN_WIDTH/2 - player_img.getWidth()/2, 
                      SCREEN_HEIGHT/2 - player_img.getHeight()/2, null);
        
        // Draw the bat image
        if (bats_list.contains(player_pos)) {
            g2d.drawImage(bat_img, SCREEN_WIDTH/2 - bat_img.getWidth()/2, 
                          SCREEN_HEIGHT/2 - bat_img.getHeight()/2, null);
        }
        
        // Draw the Wumpus
        if (player_pos == wumpus_pos) {
            g2d.drawImage(wumpus_img, SCREEN_WIDTH/2 - wumpus_img.getWidth()/2, 
                          SCREEN_HEIGHT/2 - wumpus_img.getHeight()/2, null);
        }
        
        // Draw text
        g2d.setFont(font);
        g2d.setColor(new Color(0, 255, 64));
        int y_text_pos = 0;
        if (y_text_pos == 0) {
            System.out.println("y_text_pos is zero");
        }
        
        String pos_text = "POS:" + player_pos;
        g2d.drawString(pos_text, 0, font.getSize());
        
        String arrow_text = "Battery: " + num_arrows;
        y_text_pos = font.getSize() + 10;
        g2d.drawString(arrow_text, 0, y_text_pos + font.getSize());
        
        if (bats_near) {
            String bat_text = "You feel a strange wind";
            y_text_pos += font.getSize() + 10;
            g2d.drawString(bat_text, 0, y_text_pos + font.getSize());
        }
        
        if (pit_near) {
            String pit_text = "You feel the ground shaking";
            y_text_pos += font.getSize() + 10;
            g2d.drawString(pit_text, 0, y_text_pos + font.getSize());
        }
        
        // Timer display
        long seconds = (System.currentTimeMillis() - start_ticks) / 1000;
        long remaining_time = countdown_time - seconds;
        String timer_text = "Time Left: " + Math.max(0, remaining_time);
        FontMetrics fm = g2d.getFontMetrics();
        int x_position = SCREEN_WIDTH - fm.stringWidth(timer_text) - 10;
        int y_position = font.getSize() + 10;
        g2d.drawString(timer_text, x_position, y_position);
        
        if (bats_list.contains(player_pos)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void populateCave() {
        // Place the player
        player_pos = ThreadLocalRandom.current().nextInt(1, 21);
        
        // Place the wumpus
        placeWumpus();
        
        // Place the bats
        for (int bat = 0; bat < NUM_BATS; bat++) {
            placeBat();
        }
        
        // Place the pits
        for (int pit = 0; pit < NUM_PITS; pit++) {
            placePit();
        }
        
        // Place the arrows
        for (int flashlight = 0; flashlight < NUM_ARROWS; flashlight++) {
            placeArrow();
        }
        
        System.out.println("Player at: " + player_pos);
    }
    
    private void placeWumpus() {
        wumpus_pos = player_pos;
        while (wumpus_pos == player_pos) {
            wumpus_pos = ThreadLocalRandom.current().nextInt(1, 21);
        }
    }
    
    private void placeBat() {
        int bat_pos = player_pos;
        while (bat_pos == player_pos || bats_list.contains(bat_pos) || 
               bat_pos == wumpus_pos || pits_list.contains(bat_pos)) {
            bat_pos = ThreadLocalRandom.current().nextInt(1, 21);
        }
        bats_list.add(bat_pos);
    }
    
    private void placePit() {
        int pit_pos = player_pos;
        while (pit_pos == player_pos || bats_list.contains(pit_pos) || 
               pit_pos == wumpus_pos || pits_list.contains(pit_pos)) {
            pit_pos = ThreadLocalRandom.current().nextInt(1, 21);
        }
        pits_list.add(pit_pos);
    }
    
    private void placeArrow() {
        int arrow_pos = player_pos;
        while (arrow_pos == player_pos || bats_list.contains(arrow_pos) || 
               arrow_pos == wumpus_pos || pits_list.contains(arrow_pos)) {
            arrow_pos = ThreadLocalRandom.current().nextInt(1, 21);
        }
        arrows_list.add(arrow_pos);
    }
    
    private void checkRoom(int pos) {
        // Is there a Wumpus in the room?
        if (player_pos == wumpus_pos) {
            stopSound(breathing_sound);
            playSound(jumpscare_sound);
            gameOver("You died!!!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Is there a pit?
        if (pits_list.contains(player_pos)) {
            gameOver("You fell into a bottomless pit!!");
        }
        
        // Is there bats in the room? If so move the player and the bats
        if (bats_list.contains(player_pos)) {
            playSound(blinded_sound);
            System.out.println("You have been blinded!");
            
            Graphics2D g2d = (Graphics2D) getGraphics();
            g2d.setColor(BLACK);
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            
            g2d.setFont(font);
            g2d.setColor(new Color(0, 255, 64));
            String bat_text = "You have been blinded";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (SCREEN_WIDTH - fm.stringWidth(bat_text)) / 2;
            int y = SCREEN_HEIGHT / 2;
            g2d.drawString(bat_text, x, y);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Move the bats
            int new_pos = player_pos;
            while (new_pos == player_pos || bats_list.contains(new_pos) || 
                   new_pos == wumpus_pos || pits_list.contains(new_pos)) {
                new_pos = ThreadLocalRandom.current().nextInt(1, 21);
            }
            bats_list.remove(Integer.valueOf(player_pos));
            bats_list.add(new_pos);
            System.out.println("bat at: " + new_pos);
            
            // Now move the player
            new_pos = player_pos;
            while (new_pos == player_pos || bats_list.contains(new_pos) || 
                   new_pos == wumpus_pos || pits_list.contains(new_pos)) {
                new_pos = ThreadLocalRandom.current().nextInt(1, 21);
            }
            player_pos = new_pos;
            System.out.println("player at:" + player_pos);
        }
        
        // Is there a flashlight in the room?
        if (arrows_list.contains(player_pos)) {
            Graphics2D g2d = (Graphics2D) getGraphics();
            g2d.setColor(BLACK);
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            
            g2d.setFont(font);
            g2d.setColor(new Color(0, 20, 255));
            String text = "You have found a battery";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
            int y = SCREEN_HEIGHT / 2;
            g2d.drawString(text, x, y);
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            num_arrows++;
            arrows_list.remove(Integer.valueOf(player_pos));
        }
    }
    
    private void resetGame() {
        num_arrows = 2;
        bats_list.clear();
        pits_list.clear();
        arrows_list.clear();
        populateCave();
    }
    
    private void gameOver(String message) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Graphics2D g2d = (Graphics2D) getGraphics();
        g2d.setColor(RED);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        g2d.setFont(font);
        g2d.setColor(WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (SCREEN_WIDTH - fm.stringWidth(message)) / 2;
        int y = SCREEN_HEIGHT / 2;
        g2d.drawString(message, x, y);
        
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println(message);
        System.exit(0);
    }
    
    private void moveWumpus() {
        if (mobile_wumpus || ThreadLocalRandom.current().nextInt(1, 11) <= wumpus_move_chance) {
            return;
        }
        
        int[] exits = cave.get(wumpus_pos);
        for (int new_room : exits) {
            if (new_room == 0) {
                continue;
            } else if (new_room == player_pos) {
                continue;
            } else if (bats_list.contains(new_room)) {
                continue;
            } else if (pits_list.contains(new_room)) {
                continue;
            } else {
                wumpus_pos = new_room;
                break;
            }
        }
        
        playSound(wumpus_move_sound);
    }
    
    private void shootArrow(int direction) {
        playSound(shoot_sound);
        hit = false;
        
        if (num_arrows == 0) {
            return;
        }
        
        num_arrows--;
        
        if (wumpus_pos == cave.get(player_pos)[direction]) {
            hit = true;
        }
        
        if (hit) {
            stopSound(breathing_sound);
            Graphics2D g2d = (Graphics2D) getGraphics();
            int x = (SCREEN_WIDTH - dead_wumpus_image.getWidth()) / 2;
            int y = (SCREEN_HEIGHT - dead_wumpus_image.getHeight()) / 2;
            displayDeadWumpus(g2d, x, y);
            playSound(wumpus_death_sound);
            
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            gameOver("You Win!");
            System.exit(0);
        } else {
            placeWumpus();
        }
        
        if (num_arrows == 0) {
            gameOver("You are out of battery!");
            System.exit(0);
        }
    }
    
    private void displayDeadWumpus(Graphics2D g2d, int x, int y) {
        g2d.drawImage(dead_wumpus_image, x, y, null);
    }
    
    private void checkWumpusDistance(int player_pos, int wumpus_pos) {
        int distance = Math.abs(player_pos - wumpus_pos);
        if (distance < 2) {
            playSound(breathing_sound, true);
        } else {
            stopSound(breathing_sound);
        }
    }
    
    private void printInstructions() {
        System.out.println("""
                                        Game Instructions

    Mechanics:
You have been put into a cave where your objective is to kill the Wumpus using your flashlight.
There are multiple things wandering around the cave. This include:

Wumpus - A monster hungry to eat anything it sees. 
         Avoid running into it by listening for its noises and try using your flashlight to kill it.
         **NOTE** Sometimes the Wumpus will choose to be silent and not making any noises when you are near.
                  To be cautious, trying using your flashlight if you are unsure.
Blinding fogs - If you walk into it, it will blind you and put you into a random room in the cave.
                Try to look for clues to avoid them.
Bottomless pits - If you fell into these pits, it is game over. 
                  Keep an eye out for clues to avoid these.
Batteries - These are used to power your flashlight. 
            Try collecting these to use your flashlight against the Wumpus.
            If you run out of batteries, you will lose.

There is also a timer that you also need to keep an eye out for. 
If the timer goes down to zero before you kill the Wumpus, you will lose.

    Controls:
Use the arrow keys to move between caves.  
Press the <SHIFT> key and an arrow key to use your flashlight at the Wumpus.
        """);
    }
    
    //===============================================================================
    //                       Initialization Methods                                 =
    //===============================================================================
    
    private void initializeCave() {
        cave.put(1, new int[]{0, 8, 2, 5});
        cave.put(2, new int[]{0, 10, 3, 1});
        cave.put(3, new int[]{0, 12, 4, 2});
        cave.put(4, new int[]{0, 14, 5, 3});
        cave.put(5, new int[]{0, 6, 1, 4});
        cave.put(6, new int[]{5, 0, 7, 15});
        cave.put(7, new int[]{0, 17, 8, 6});
        cave.put(8, new int[]{1, 0, 9, 7});
        cave.put(9, new int[]{0, 18, 10, 8});
        cave.put(10, new int[]{2, 0, 11, 9});
        cave.put(11, new int[]{0, 19, 12, 10});
        cave.put(12, new int[]{3, 0, 13, 11});
        cave.put(13, new int[]{0, 20, 14, 12});
        cave.put(14, new int[]{4, 0, 15, 13});
        cave.put(15, new int[]{0, 16, 6, 14});
        cave.put(16, new int[]{15, 0, 17, 20});
        cave.put(17, new int[]{7, 0, 18, 16});
        cave.put(18, new int[]{9, 0, 19, 17});
        cave.put(19, new int[]{11, 0, 20, 18});
        cave.put(20, new int[]{13, 0, 16, 19});
    }
    
    private void loadImages() {
        try {
            bat_img = ImageIO.read(new File("images/bat.png"));
            player_img = ImageIO.read(new File("images/player.png"));
            wumpus_img = ImageIO.read(new File("images/wumpus.png"));
            dead_wumpus_image = ImageIO.read(new File("dead_wumpus.png"));
        } catch (IOException e) {
            System.err.println("Error loading images: " + e.getMessage());
            // Create placeholder images if files don't exist
            bat_img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
            player_img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
            wumpus_img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
            dead_wumpus_image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        }
    }
    
    private void loadSounds() {
        try {
            footstep_sound = loadSound("footsteps.mp3");
            jumpscare_sound = loadSound("jumpscare.mp3");
            ambient_sound = loadSound("ambient.mp3");
            breathing_sound = loadSound("breathing.mp3");
            shoot_sound = loadSound("shoot.mp3");
            wumpus_death_sound = loadSound("wumpus_death.mp3");
            blinded_sound = loadSound("blinded.mp3");
            wumpus_move_sound = loadSound("wumpus_move.mp3");
            
            // Set volume levels
            setVolume(wumpus_death_sound, 0.8f);
            setVolume(wumpus_move_sound, 1.5f);
            setVolume(jumpscare_sound, 1.0f);
            
            // Play ambient sound
            playSound(ambient_sound, true);
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }
    
    private Clip loadSound(String filename) {
        try {
            File soundFile = new File(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            System.err.println("Error loading sound " + filename + ": " + e.getMessage());
            return null;
        }
    }
    
    private void playSound(Clip clip) {
        playSound(clip, false);
    }
    
    private void playSound(Clip clip, boolean loop) {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        }
    }
    
    private void stopSound(Clip clip) {
        if (clip != null) {
            clip.stop();
        }
    }
    
    private void setVolume(Clip clip, float volume) {
        if (clip != null) {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            volumeControl.setValue(dB);
        }
    }
    
    private void setupUI() {
        frame = new JFrame("Hunt the Wumpus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setResizable(false);
        frame.add(this);
        
        setFocusable(true);
        addKeyListener(this);
        
        frame.setVisible(true);
    }
    
    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            // Check if time is up
            long seconds = (System.currentTimeMillis() - start_ticks) / 1000;
            long remaining_time = countdown_time - seconds;
            while (running) {
            if (remaining_time <= 0) {
                System.out.println("You ran out of time!");
                running = false;
            }
                gameOver("Time's up!");
            }
            
            checkRoom(player_pos);
            checkWumpusDistance(player_pos, wumpus_pos);
            repaint();
        });
        gameTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawRoom(player_pos, g2d);
    }
    
    //===============================================================================
    //                       Key Event Handling                                     =
    //===============================================================================
    
    @Override
    public void keyPressed(KeyEvent e) {
        boolean shiftPressed = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
            case KeyEvent.VK_LEFT:
                if (shiftPressed) {
                    shootArrow(LEFT);
                } else if (cave.get(player_pos)[LEFT] > 0) {
                    player_pos = cave.get(player_pos)[LEFT];
                    moveWumpus();
                    playSound(footstep_sound);
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (shiftPressed) {
                    shootArrow(RIGHT);
                } else if (cave.get(player_pos)[RIGHT] > 0) {
                    player_pos = cave.get(player_pos)[RIGHT];
                    moveWumpus();
                    playSound(footstep_sound);
                }
                break;
            case KeyEvent.VK_UP:
                if (shiftPressed) {
                    shootArrow(UP);
                } else if (cave.get(player_pos)[UP] > 0) {
                    player_pos = cave.get(player_pos)[UP];
                    moveWumpus();
                    playSound(footstep_sound);
                }
                break;
            case KeyEvent.VK_DOWN:
                if (shiftPressed) {
                    shootArrow(DOWN);
                } else if (cave.get(player_pos)[DOWN] > 0) {
                    player_pos = cave.get(player_pos)[DOWN];
                    moveWumpus();
                    playSound(footstep_sound);
                }
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    //===============================================================================
    //                       Main Method                                            =
    //===============================================================================
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HuntTheWumpus());
    }
}

