package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Timer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;

import gameBoard.GameBoard;
import keyEvents.UserInput;
import tetrads.Tetrad;

public class GameGUI{
	
	private FieldGUI field;
	private InfoGUI info;
	private JFrame root;
	private Tetrad lastQueue;
	private Tetrad lastHold;
	private GameBoard game;
	
	public GameGUI(GameBoard g, Timer t) {
		game = g;
		root = new JFrame("Tetris");
		
		field = new FieldGUI(g);
		info = new InfoGUI(g, field, field.getXPadding(), field.getYPadding());
		
		root.setLayout(new BoxLayout(root.getContentPane(), BoxLayout.X_AXIS));
		info.setPreferredSize(new Dimension((int) Math.round(800 * 0.3), 600));
		info.setBorder(BorderFactory.createLineBorder(Color.black));
		info.setMaximumSize(info.getPreferredSize());
		root.add(info);
		root.add(field);
		lastQueue = g.getQueue();
		lastHold = g.getHolding();
		
		root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		UserInput keyListener = new UserInput(g, t);
		root.addKeyListener(keyListener);
	}
	
	public void update() {
		if (lastQueue != game.getQueue() || lastHold != game.getHolding()) {
			info.repaint();
			lastQueue = game.getQueue();
			lastHold = game.getHolding();
		}
		field.repaint();
	}
	
	public JFrame getRoot() {
		return root;
	}
	
	public void setAppend(String append) {
		info.setAppend(append);
	}
	

}
