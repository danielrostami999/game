import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class LudoGameGUI extends JFrame {

    private BoardPanel boardPanel;
    private DicePanel dicePanel;
    private GameController gameController;

    public LudoGameGUI() {
        setTitle("Ludo Game - Phase 5");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 800);
        setLocationRelativeTo(null);

        gameController = new GameController();

        boardPanel = new BoardPanel(gameController);
        dicePanel = new DicePanel(gameController, boardPanel);

        add(boardPanel, BorderLayout.CENTER);
        add(dicePanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LudoGameGUI::new);
    }
}

enum PieceState {
    HOME,
    ACTIVE,
    FINAL_PATH,
    FINISHED
}

class Piece {
    private final int playerIndex;
    private PieceState state;
    private int position; // -1: home, 0-39: active path, 0-3: final path, -2: finished

    public Piece(int playerIndex) {
        this.playerIndex = playerIndex;
        this.state = PieceState.HOME;
        this.position = -1;
    }

    public int getPlayerIndex() { return playerIndex; }
    public PieceState getState() { return state; }
    public int getPosition() { return position; }

    public void setState(PieceState state) { this.state = state; }
    public void setPosition(int pos) { this.position = pos; }
}

class Player {
    private final int playerIndex;
    private final Piece[] pieces;

    public Player(int playerIndex) {
        this.playerIndex = playerIndex;
        pieces = new Piece[4];
        for (int i=0; i<4; i++) {
            pieces[i] = new Piece(playerIndex);
        }
    }

    public Piece[] getPieces() { return pieces; }
}

class GameController {
    private int currentPlayer = 0;
    private boolean hasRolled = false;
    private boolean hasMoved = false;

    private int dice1 = 0, dice2 = 0;
    private boolean bonusRoll = false;

    private final Color[] playerColors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
    private final String[] playerNames = {"Red", "Blue", "Green", "Yellow"};

    private final Player[] players;

    private Piece selectedPiece = null;

    public GameController() {
        players = new Player[4];
        for (int i=0; i<4; i++) {
            players[i] = new Player(i);
        }
    }

    public Player[] getPlayers() { return players; }
    public Color[] getPlayerColors() { return playerColors; }
    public int getCurrentPlayer() { return currentPlayer; }
    public Color getCurrentPlayerColor() { return playerColors[currentPlayer]; }
    public String getCurrentPlayerName() { return playerNames[currentPlayer]; }
    public int getDice1() { return dice1; }
    public int getDice2() { return dice2; }
    public boolean hasBonusRoll() { return bonusRoll; }
    public boolean hasRolled() { return hasRolled; }
    public boolean hasMoved() { return hasMoved; }
    public Piece getSelectedPiece() { return selectedPiece; }

    public void selectPiece(Piece piece) {
        if(piece.getPlayerIndex() == currentPlayer)
            selectedPiece = piece;
    }

    public void deselectPiece() {
        selectedPiece = null;
    }

    public boolean rollDice() {
        if (hasRolled) return false;
        Random rnd = new Random();
        dice1 = rnd.nextInt(6) + 1;
        dice2 = rnd.nextInt(6) + 1;
        hasRolled = true;
        bonusRoll = (dice1 == dice2);
        hasMoved = false;
        return true;
    }

    public int getSteps() {
        return dice1 + dice2;
    }

    private int startIndexForPlayer(int playerIndex) {
        switch(playerIndex) {
            case 0: return 1;
            case 1: return 11;
            case 2: return 21;
            case 3: return 31;
            default: return 0;
        }
    }

    public boolean moveSelectedPiece() {
        if(selectedPiece == null) return false;
        if(!hasRolled || hasMoved) return false;

        int steps = dice1 + dice2;

        if(selectedPiece.getState() == PieceState.HOME) {
            // فقط وقتی تاس 6 اومده میشه وارد مسیر شد
            if(dice1 == 6 || dice2 == 6) {
                selectedPiece.setState(PieceState.ACTIVE);
                selectedPiece.setPosition(startIndexForPlayer(currentPlayer));
                hasMoved = true;
                return true;
            } else {
                return false;
            }
        } else if(selectedPiece.getState() == PieceState.ACTIVE) {
            int pos = selectedPiece.getPosition();
            int finalEntry = (startIndexForPlayer(currentPlayer) + 39) % 40;

            if(willEnterFinalPath(selectedPiece, steps)) {
                int stepsInFinal = steps - (finalEntry - pos + 1);
                if(stepsInFinal < 4) {
                    selectedPiece.setState(PieceState.FINAL_PATH);
                    selectedPiece.setPosition(stepsInFinal);
                    hasMoved = true;
                    return true;
                } else if(stepsInFinal == 4) {
                    selectedPiece.setState(PieceState.FINISHED);
                    selectedPiece.setPosition(-2);
                    hasMoved = true;
                    return true;
                } else {
                    return false; // نمی‌تونه بیشتر از پارکینگ بره
                }
            } else {
                int newPos = (pos + steps) % 40;
                selectedPiece.setPosition(newPos);
                hasMoved = true;
                return true;
            }
        } else if(selectedPiece.getState() == PieceState.FINAL_PATH) {
            int newPos = selectedPiece.getPosition() + steps;
            if(newPos < 4) {
                selectedPiece.setPosition(newPos);
                hasMoved = true;
                return true;
            } else if(newPos == 4) {
                selectedPiece.setState(PieceState.FINISHED);
                selectedPiece.setPosition(-2);
                hasMoved = true;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean willEnterFinalPath(Piece piece, int steps) {
        int pos = piece.getPosition();
        int playerIndex = piece.getPlayerIndex();
        int finalEntry = (startIndexForPlayer(playerIndex) + 39) % 40;

        if(pos <= finalEntry)
            return (pos + steps) > finalEntry;
        else
            return ((pos + steps) % 40) > finalEntry;
    }

    // حذف مهره حریف اگر در خانه مهره فعلی بود
    public void checkAndRemoveOpponentPiece() {
        if(selectedPiece == null) return;
        if(selectedPiece.getState() == PieceState.ACTIVE) {
            int pos = selectedPiece.getPosition();
            int playerIndex = selectedPiece.getPlayerIndex();

            for(Player p : players) {
                if(p == players[playerIndex]) continue; // نمی‌زنه مهره خودش

                for(Piece piece : p.getPieces()) {
                    if(piece.getState() == PieceState.ACTIVE && piece.getPosition() == pos) {
                        // حذف مهره حریف
                        piece.setState(PieceState.HOME);
                        piece.setPosition(-1);
                    }
                }
            }
        }
    }

    public void endTurn() {
        if(!hasMoved) return;
        if(bonusRoll) {
            // جایزه تاس دوباره، فقط پرتاب مجدد
            hasRolled = false;
            hasMoved = false;
            selectedPiece = null;
            // نوبت نمی‌رود
        } else {
            // نوبت به بازیکن بعدی
            currentPlayer = (currentPlayer + 1) % 4;
            hasRolled = false;
            hasMoved = false;
            selectedPiece = null;
        }
    }
}

class DicePanel extends JPanel {
    private JLabel lblDice1, lblDice2, lblStatus;
    private JButton btnRoll, btnMove, btnEndTurn;

    private GameController controller;
    private BoardPanel board;

    public DicePanel(GameController controller, BoardPanel board) {
        this.controller = controller;
        this.board = board;

        setPreferredSize(new Dimension(640, 120));
        setLayout(new FlowLayout());

        lblDice1 = new JLabel("Dice 1: -");
        lblDice2 = new JLabel("Dice 2: -");
        lblStatus = new JLabel("Waiting for " + controller.getCurrentPlayerName() + "'s turn");

        btnRoll = new JButton("Roll Dice");
        btnMove = new JButton("Move Selected Piece");
        btnEndTurn = new JButton("End Turn");

        btnMove.setEnabled(false);
        btnEndTurn.setEnabled(false);

        add(lblDice1);
        add(lblDice2);
        add(lblStatus);
        add(btnRoll);
        add(btnMove);
        add(btnEndTurn);

        updateButtons();

        btnRoll.addActionListener(e -> {
            if(controller.rollDice()) {
                lblDice1.setText("Dice 1: " + controller.getDice1());
                lblDice2.setText("Dice 2: " + controller.getDice2());
                lblStatus.setText(controller.getCurrentPlayerName() + " rolled. Select a piece and move.");
                btnRoll.setEnabled(false);
                btnMove.setEnabled(true);
                btnEndTurn.setEnabled(false);
                updateButtons();
                board.repaint();
            }
        });

        btnMove.addActionListener(e -> {
            if(controller.moveSelectedPiece()) {
                controller.checkAndRemoveOpponentPiece();
                lblStatus.setText(controller.getCurrentPlayerName() + " moved piece.");
                btnMove.setEnabled(false);
                btnEndTurn.setEnabled(true);
                board.repaint();
            } else {
                lblStatus.setText("Invalid move! Select another piece or roll again.");
            }
        });

        btnEndTurn.addActionListener(e -> {
            controller.endTurn();
            lblStatus.setText(controller.getCurrentPlayerName() + "'s turn. Roll the dice.");
            lblDice1.setText("Dice 1: -");
            lblDice2.setText("Dice 2: -");
            btnRoll.setEnabled(true);
            btnMove.setEnabled(false);
            btnEndTurn.setEnabled(false);
            board.repaint();
        });
    }

    private void updateButtons() {
        btnRoll.setBackground(controller.getCurrentPlayerColor());
        btnRoll.setForeground(Color.WHITE);

        btnMove.setBackground(Color.GRAY);
        btnMove.setForeground(Color.WHITE);

        btnEndTurn.setBackground(Color.DARK_GRAY);
        btnEndTurn.setForeground(Color.WHITE);
    }
}

class BoardPanel extends JPanel {

    private final int CENTER_X = 300;
    private final int CENTER_Y = 300;
    private final int RADIUS = 200;
    private final int CELL_RADIUS = 20;
    private final int FINAL_PATH_LENGTH = 4;

    private final List<Cell> cells = new ArrayList<>();
    private GameController controller;

    public BoardPanel(GameController controller) {
        this.controller = controller;
        setBackground(Color.WHITE);
        generateMainPath();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point clicked = e.getPoint();
                for(Player player : controller.getPlayers()) {
                    for(Piece piece : player.getPieces()) {
                        Point pPos = getPiecePosition(piece);
                        double dist = clicked.distance(pPos);
                        if(dist < CELL_RADIUS) {
                            if(piece.getPlayerIndex() == controller.getCurrentPlayer()) {
                                controller.selectPiece(piece);
                                repaint();
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    private void generateMainPath() {
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians((360.0 / 40) * i);
            int x = (int) (CENTER_X + RADIUS * Math.cos(angle));
            int y = (int) (CENTER_Y + RADIUS * Math.sin(angle));
            cells.add(new Cell(x, y, i));
        }
    }

    private void drawFinalPath(Graphics2D g, int baseIndex, Color color) {
        Cell start = cells.get(baseIndex);
        int dx = CENTER_X - start.x;
        int dy = CENTER_Y - start.y;

        for (int i = 1; i <= FINAL_PATH_LENGTH; i++) {
            int x = start.x + (dx * i) / (FINAL_PATH_LENGTH + 1);
            int y = start.y + (dy * i) / (FINAL_PATH_LENGTH + 1);
            g.setColor(color);
            g.fillOval(x - CELL_RADIUS / 2, y - CELL_RADIUS / 2, CELL_RADIUS, CELL_RADIUS);
            g.setColor(Color.BLACK);
            g.drawOval(x - CELL_RADIUS / 2, y - CELL_RADIUS / 2, CELL_RADIUS, CELL_RADIUS);
        }
    }

    private Point getCellCenter(int index) {
        Cell c = cells.get(index);
        return new Point(c.x, c.y);
    }

    private Point getHomePosition(int playerIndex, Piece piece) {
        int xBase = 10 + playerIndex * 50;
        int yBase = 500;
        int pieceIndex = Arrays.asList(controller.getPlayers()[playerIndex].getPieces()).indexOf(piece);
        return new Point(xBase, yBase + pieceIndex * (CELL_RADIUS + 5));
    }

    private Point getFinalPathPosition(int playerIndex, int finalPos) {
        Cell start = cells.get(startIndexForPlayer(playerIndex));
        int dx = CENTER_X - start.x;
        int dy = CENTER_Y - start.y;
        int x = start.x + (dx * (finalPos+1)) / (FINAL_PATH_LENGTH + 1);
        int y = start.y + (dy * (finalPos+1)) / (FINAL_PATH_LENGTH + 1);
        return new Point(x, y);
    }

    private int startIndexForPlayer(int playerIndex) {
        switch(playerIndex) {
            case 0: return 1;
            case 1: return 11;
            case 2: return 21;
            case 3: return 31;
            default: return 0;
        }
    }

    private Point getPiecePosition(Piece piece) {
        switch(piece.getState()) {
            case HOME:
                return getHomePosition(piece.getPlayerIndex(), piece);
            case ACTIVE:
                return getCellCenter(piece.getPosition());
            case FINAL_PATH:
                return getFinalPathPosition(piece.getPlayerIndex(), piece.getPosition());
            case FINISHED:
                return new Point(-100, -100); // خارج صفحه
            default:
                return new Point(-100, -100);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // رسم مسیر اصلی
        for (Cell cell : cells) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillOval(cell.x - CELL_RADIUS, cell.y - CELL_RADIUS, CELL_RADIUS * 2, CELL_RADIUS * 2);
            g2.setColor(Color.BLACK);
            g2.drawOval(cell.x - CELL_RADIUS, cell.y - CELL_RADIUS, CELL_RADIUS * 2, CELL_RADIUS * 2);
            g2.drawString(String.valueOf(cell.index), cell.x - 6, cell.y + 4);
        }

        // رسم خانه های شروع (home) با رنگ و یک خانه جلوتر
        g2.setColor(Color.RED);
        g2.fillOval(cells.get(1).x - 10, cells.get(1).y - 10, 20, 20);

        g2.setColor(Color.BLUE);
        g2.fillOval(cells.get(11).x - 10, cells.get(11).y - 10, 20, 20);

        g2.setColor(Color.GREEN);
        g2.fillOval(cells.get(21).x - 10, cells.get(21).y - 10, 20, 20);

        g2.setColor(Color.YELLOW);
        g2.fillOval(cells.get(31).x - 10, cells.get(31).y - 10, 20, 20);

        // رسم مسیر نهایی (پارکینگ)
        drawFinalPath(g2, 0, Color.RED);
        drawFinalPath(g2, 10, Color.BLUE);
        drawFinalPath(g2, 20, Color.GREEN);
        drawFinalPath(g2, 30, Color.YELLOW);

        // رسم مهره‌ها
        for(Player player : controller.getPlayers()) {
            for(Piece piece : player.getPieces()) {
                if(piece == controller.getSelectedPiece()) {
                    // هایلایت مهره انتخاب شده
                    g2.setColor(Color.MAGENTA);
                    Point pos = getPiecePosition(piece);
                    g2.fillOval(pos.x - CELL_RADIUS / 2 - 4, pos.y - CELL_RADIUS / 2 - 4, CELL_RADIUS + 8, CELL_RADIUS + 8);
                }
            }
        }

        for(Player player : controller.getPlayers()) {
            for(Piece piece : player.getPieces()) {
                drawPiece(g2, piece);
            }
        }
    }

    private void drawPiece(Graphics2D g2, Piece piece) {
        Color color = controller.getPlayerColors()[piece.getPlayerIndex()];
        g2.setColor(color);
        Point pos = getPiecePosition(piece);
        g2.fillOval(pos.x - CELL_RADIUS / 2, pos.y - CELL_RADIUS / 2, CELL_RADIUS, CELL_RADIUS);
        g2.setColor(Color.BLACK);
        g2.drawOval(pos.x - CELL_RADIUS / 2, pos.y - CELL_RADIUS / 2, CELL_RADIUS, CELL_RADIUS);
    }

    static class Cell {
        int x, y, index;
        public Cell(int x, int y, int index) {
            this.x = x;
            this.y = y;
            this.index = index;
        }
    }
}
