package com.salifm.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import  javax.swing.UIManager;

import com.salifm.tetris.Tetromino.Tetrominoes;

public class GameBoardPanel extends JPanel implements ActionListener {
    private static final int BoardWidth = 10;    // game board x size
    private static final int BoardHeight = 22;    // game board y size
    private static final int MIN_TIMER_RESOLUTION = 100;
    private static final int MAX_TIMER_RESOLUTION = 370;

    // game status & timer
    private Timer timer;
    private boolean isFallingDone = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private int currentScore = 0; // removed lines == score

    // position of current block
    private int curX = 0;
    private int curY = 0;

    // current tetromino
    private Tetromino curBlock;

    // logical game block
    private Tetrominoes[] gameBoard;
    private Color[] colorTable;

    // adjusting game status
    private String currentStatus;
    private String currentLevel;
    private int currentTimerResolution;

    private GameWindow tetrisFrameD;


    public GameBoardPanel(GameWindow tetrisFrame, int timerResolution) {
        initializeAttributes(tetrisFrame, timerResolution);
        setFocusable(true);
        setBackground(new Color(0, 30, 30));
        addKeyListener(createKeyListener());
        tetrisFrameD = tetrisFrame;
        initBoard();
    }

    private void initializeAttributes(GameWindow tetrisFrame, int timerResolution) {
        curBlock = new Tetromino();
        timer = new Timer(timerResolution, this);
        timer.start();
        currentTimerResolution = timerResolution;
        gameBoard = new Tetrominoes[BoardWidth * BoardHeight];
        colorTable = initializeColorTable();
    }

    private Color[] initializeColorTable() {
        return new Color[]{
                new Color(0, 0, 0), new Color(164, 135, 255),
                new Color(255, 128, 0), new Color(255, 0, 0),
                new Color(32, 128, 255), new Color(255, 0, 255),
                new Color(255, 255, 0), new Color(0, 255, 0)
        };
    }

    private KeyAdapter createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
        };
    }

    private void handleKeyPressed(KeyEvent e) {
        if (!isStarted || curBlock.getShape() == Tetrominoes.NO_BLOCK) {
            return;
        }

        int keycode = e.getKeyCode();

        if (keycode == 'p' || keycode == 'P') {
            pause();
            return;
        }

        if (isPaused) {
            return;
        }

        handleKeyActions(e);
    }

    private void handleKeyActions(KeyEvent e) {
        if (!isStarted || curBlock.getShape() == Tetrominoes.NO_BLOCK) {
            return;
        }

        int keycode = e.getKeyCode();

        if (keycode == 'p' || keycode == 'P') {
            pause();
            return;
        }

        if (isPaused) {
            return;
        }

        switch (keycode) {
            case 'a':
            case 'A':
            case KeyEvent.VK_LEFT:
                atomIsMovable(curBlock, curX - 1, curY, true);
                break;
            case 'd':
            case 'D':
            case KeyEvent.VK_RIGHT:
                atomIsMovable(curBlock, curX + 1, curY, true);
                break;
            case 'w':
            case 'W':
            case KeyEvent.VK_UP:
                atomIsMovable(curBlock.rotateRight(), curX, curY, true);
                break;
            case 's':
            case 'S':
            case KeyEvent.VK_DOWN:
                advanceOneLine();
                break;
            case KeyEvent.VK_SPACE:
                advanceToEnd();
                break;
            case 'p':
            case 'P':
                pause();
                break;
        }
    }

    // adjusting game level
    private void setResolution() {
        int level = currentScore / 10;
        currentTimerResolution = Math.max(MIN_TIMER_RESOLUTION, MAX_TIMER_RESOLUTION - (level * 30));
        timer.setDelay(currentTimerResolution);
    }

    // initialize game board
    private void initBoard() {
        for (int i = 0; i < BoardWidth * BoardHeight; i++) {
            gameBoard[i] = Tetrominoes.NO_BLOCK;
        }
    }

    // timer callback
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isFallingDone) {
            isFallingDone = !isFallingDone; // toggle status
            newTetromino();
        } else {
            advanceOneLine();
        }
    }

    public void start() {
        if (isPaused) {
            return;
        }

        isStarted = true;
        isFallingDone = false;
        currentScore = 0;
        initBoard();

        newTetromino();
        timer.start();
    }

    public void pause() {
        if (!isStarted) {
            return;
        }

        isPaused = !isPaused;
        if (isPaused) {
            timer.stop();
        } else {
            timer.start();
        }

        repaint();
    }

    // calculates actual size of tetromino on screen
    private int blockWidth() {
        return (int) getSize().getWidth() / BoardWidth;
    }

    private int blockHeight() {
        return (int) getSize().getHeight() / BoardHeight;
    }

    // current tetromino position in array (atom)
    Tetrominoes curTetrominoPos(int x, int y) {
        return gameBoard[(y * BoardWidth) + x];
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        drawGameInfo(g);
        drawShadow(g);
        drawGameBoard(g);
        drawCurrentTetromino(g);
    }

    private void drawGameInfo(Graphics g) {
        if (!isPaused) {
            currentStatus = "Score: " + currentScore;
            currentLevel = "Level: " + (currentScore / 10 + 1);
        } else {
            currentStatus = "PAUSED";
            currentLevel = "";
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 28));
        g.drawString(currentStatus, 15, 35);
        g.drawString(currentLevel, 15, 70);
    }

    private void drawShadow(Graphics g) {
        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BoardHeight * blockHeight();

        int tempY = curY;
        while (tempY > 0) {
            if (!atomIsMovable(curBlock, curX, tempY - 1, false))
                break;
            tempY--;
        }
        for (int i = 0; i < 4; i++) {
            int x = curX + curBlock.getX(i);
            int y = tempY - curBlock.getY(i);
            drawTetromino(g, 0 + x * blockWidth(), boardTop + (BoardHeight - y - 1) * blockHeight(),
                    curBlock.getShape(), true);
        }
    }

    private void drawGameBoard(Graphics g) {
        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BoardHeight * blockHeight();

        for (int i = 0; i < BoardHeight; i++) {
            for (int j = 0; j < BoardWidth; j++) {
                Tetrominoes shape = curTetrominoPos(j, BoardHeight - i - 1);
                if (shape != Tetrominoes.NO_BLOCK)
                    drawTetromino(g, 0 + j * blockWidth(), boardTop + i * blockHeight(), shape, false);
            }
        }
    }

    private void drawCurrentTetromino(Graphics g) {
        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BoardHeight * blockHeight();

        if (curBlock.getShape() != Tetrominoes.NO_BLOCK) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curBlock.getX(i);
                int y = curY - curBlock.getY(i);
                drawTetromino(g, 0 + x * blockWidth(), boardTop + (BoardHeight - y - 1) * blockHeight(),
                        curBlock.getShape(), false);
            }
        }
    }

    private void drawTetromino(Graphics g, int x, int y, Tetrominoes bs, boolean isShadow) {
        Color curColor = colorTable[bs.ordinal()];

        if (!isShadow) {
            g.setColor(curColor);
            g.fillRect(x + 1, y + 1, blockWidth() - 2, blockHeight() - 2);
        } else {
            g.setColor(curColor.darker().darker());
            g.fillRect(x + 1, y + 1, blockWidth() - 2, blockHeight() - 2);
        }
    }

    private void removeFullLines() {
        int fullLines = 0;

        for (int i = BoardHeight - 1; i >= 0; i--) {
            boolean isFull = true;

            for (int j = 0; j < BoardWidth; j++) {
                if (curTetrominoPos(j, i) == Tetrominoes.NO_BLOCK) {
                    isFull = false;
                    break;
                }
            }

            if (isFull) {
                ++fullLines;
                for (int k = i; k < BoardHeight - 1; k++) {
                    for (int l = 0; l < BoardWidth; ++l)
                        gameBoard[(k * BoardWidth) + l] = curTetrominoPos(l, k + 1);
                }
            }
        }

        if (fullLines > 0) {
            currentScore += fullLines;
            isFallingDone = true;
            curBlock.setShape(Tetrominoes.NO_BLOCK);
            setResolution();
            repaint();
        }

    }

    // true - actual tetromino pos
    // flase - shadow pos
    private boolean atomIsMovable(Tetromino chkBlock, int chkX, int chkY, boolean flag) {
        for (int i = 0; i < 4; i++) {
            int x = chkX + chkBlock.getX(i);
            int y = chkY - chkBlock.getY(i);
            if (x < 0 || x >= BoardWidth || y < 0 || y >= BoardHeight)
                return false;
            if (curTetrominoPos(x, y) != Tetrominoes.NO_BLOCK) {
                return false;
            }
        }

        if (flag) {
            curBlock = chkBlock;
            curX = chkX;
            curY = chkY;
            repaint();
        }

        return true;
    }

    private void newTetromino() {
        curBlock.setRandomShape();
        curX = BoardWidth / 2 + 1;
        curY = BoardHeight - 1 + curBlock.minY();

        if (!atomIsMovable(curBlock, curX, curY, true)) {
            curBlock.setShape(Tetrominoes.NO_BLOCK);
            timer.stop();
            isStarted = false;
            GameOver(currentScore);
        }
    }

    private void tetrominoFixed() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curBlock.getX(i);
            int y = curY - curBlock.getY(i);
            gameBoard[(y * BoardWidth) + x] = curBlock.getShape();
        }

        removeFullLines();

        if (!isFallingDone) {
            newTetromino();
        }
    }

    private void advanceOneLine() {
        if (!atomIsMovable(curBlock, curX, curY - 1, true)) {
            tetrominoFixed();
        }
    }

    private void advanceToEnd() {
        int tempY = curY;
        while (tempY > 0) {
            if (!atomIsMovable(curBlock, curX, tempY - 1, true))
                break;
            --tempY;
        }
        tetrominoFixed();
    }

    private void GameOver(int dbScore) {
        int maxScore = readDB();
        String showD = "";
        if (dbScore > maxScore) {
            writeDB(dbScore);
            showD = String.format("%nCongratulations! %nNew max score: %d", dbScore);
        } else {
            showD = String.format("Score: %d %nMax score: %d", dbScore, maxScore);
        }
        UIManager.put("OptionPane.okButtonText", "new game");
        JOptionPane.showMessageDialog(null, showD, "Game Over!", JOptionPane.OK_OPTION);
        setResolution();
        start();
    }

    private int readDB() {
        try {
            BufferedReader inputStream = new BufferedReader(new FileReader("Tetris.score"));
            String dbMaxScore = inputStream.readLine();
            inputStream.close();
            return Integer.parseInt(dbMaxScore);
        } catch (IOException e) {
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void writeDB(int dbScore) {
        try {
            File UIFile = new File("Tetris.score");
            if (!UIFile.exists()) {
                UIFile.createNewFile();
            }
            FileWriter filewriter = new FileWriter(UIFile.getAbsoluteFile());
            BufferedWriter outputStream = new BufferedWriter(filewriter);
            outputStream.write(String.valueOf(dbScore));
            outputStream.newLine();
            outputStream.write("This is database for Tetris game - https://github.com/salifm/Tetris");
            outputStream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
