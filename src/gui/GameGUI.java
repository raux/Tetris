package gui;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import gameBoard.GameBoard;

public class GameGUI{
	
	private FieldGUI field;
	private InfoGUI info;
	private JFrame root;
	
	public GameGUI(GameBoard g) {
		super();
		root = new JFrame("Tetris");
		field = new FieldGUI(g);
		info = new InfoGUI(g, field.getSquareSize(), field.getXPadding(), field.getYPadding());
		root.setLayout(new BoxLayout(root.getContentPane(), BoxLayout.X_AXIS));;
		root.add(info);
		root.add(field);
		root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void update() {
		info.repaint();
		field.repaint();
		root.repaint();
	}
	
	public JFrame getRoot() {
		return root;
	}
	

}
