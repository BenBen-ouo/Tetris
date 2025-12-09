package view;

import javax.swing.*;

public class Tetris extends JFrame {
    public final int WIDTH = 700, HEIGHT = 800;
    public Tetris() {
        this.setTitle("Tetris");
        this.setSize(WIDTH, HEIGHT);
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TetrisPanel panel = new TetrisPanel();
        panel.setBounds(0, 0, 700, 700);
        add(panel);
        addKeyListener(panel);
    }
    public static void main(String[] args) {
        Tetris gui = new Tetris();
        gui.setVisible(true);
    }
}