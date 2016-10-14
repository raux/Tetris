package ai.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import ai.generation.Generation;
import ai.gui.terminalComand.ClearTerminalCommand;
import ai.gui.terminalComand.CreateNewGenerationTerminalCommand;
import ai.gui.terminalComand.GetGenerationTerminalCommand;
import ai.gui.terminalComand.GetTerminalCommand;
import ai.gui.terminalComand.HelpTerminalCommand;
import ai.gui.terminalComand.PauseTerminalCommand;
import ai.gui.terminalComand.QuitTerminalCommand;
import ai.gui.terminalComand.ResumeTerminalCommand;
import ai.gui.terminalComand.RunSimulationTerminalCommand;
import ai.gui.terminalComand.SetTerminalCommand;
import ai.gui.terminalComand.ShowGraphicsTerminalCommand;
import ai.gui.terminalComand.TerminalCommand;
import ai.gui.terminalComand.exceptions.TerminalCommandNotFoundException;
import ai.gui.terminalComand.exceptions.TerminalException;

public class Terminal extends JPanel {

	private static final long serialVersionUID = -2607615191596287587L;
	private static final Dimension STANDARD_TEXT_PANE_DIM = new Dimension(480, 336);
	private static final Dimension MAX_INPUT_DIM = new Dimension(Integer.MAX_VALUE, 21);
	
	public static final int STANDARD_WINDOW_WIDTH = 700;
	public static final int STANDARD_WINDOW_HEIGHT = 400;
	
	public static enum Mode {TYPING, COMPLEATION};
	
	private JTextPane text;
	private JTextField input;
	private ArrayList<String> lastInput;
	private int prevInput;
	private Mode mode = Mode.TYPING;
	private Font font;
	private Timer timer;
	
	private HashMap<String, TerminalCommand> actions;
	private HashMap<String, TerminalCommand> specialActions;
	private Generation generation;
	
	private static Queue<String> appendRequests = new LinkedList<String>();
	private static Terminal currentTerm;
	
	public Terminal() {
		font = null;
		text = new JTextPane();
		input = new JTextField();
		lastInput = new ArrayList<String>();
		actions = new HashMap<String, TerminalCommand>();
		specialActions = new HashMap<String, TerminalCommand>();
		prevInput = 0;
		generation = null;
		timer = new Timer();
		currentTerm = this;
		
		createActionMap();
		createSpecialMap();
		
		ArrayList<String> words = new ArrayList<String>();
		words.addAll(actions.keySet());
		words.addAll(Generation.VALUE_LABELS.values());
		Collections.sort(words);
		input.getDocument().addDocumentListener(new AutoCompleat(input, this, words));
		
		setPreferredSize(new Dimension(STANDARD_WINDOW_WIDTH, STANDARD_WINDOW_HEIGHT));
		text.setPreferredSize(STANDARD_TEXT_PANE_DIM);
		input.setMaximumSize(MAX_INPUT_DIM);
		input.setMinimumSize(MAX_INPUT_DIM);
		input.setPreferredSize(MAX_INPUT_DIM);
		input.setFocusTraversalKeysEnabled(false);
		
		text.setEditable(false);
		text.setContentType("text/html");
		
		makeKeyBindings();
		
		try {
			InputStream in = Terminal.class.getResourceAsStream("/fonts/terminal.ttf");
			font = Font.createFont(Font.TRUETYPE_FONT, in);
			font = font.deriveFont(10f);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
		}
		
		
		if (font != null) {
			text.setFont(font);
			input.setFont(font);
		}
		JScrollPane scrollPane = new JScrollPane(text);
		scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		input.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		add(scrollPane);
		add(input);
		
		String fontFamily = font.getFamily();
		
		text.setText(String.format("<html><style>"+
		"body {font-family: \"%s\"; font-size: \"%2$d\"}" +
		 "</style></html>", fontFamily, font.getSize()));
		
	}
	
	public static void makeAppendRequest(String toAppend) {
		appendRequests.offer(toAppend);
		if (currentTerm != null) {
			currentTerm.append("");
		}
	}
	
	private void makeKeyBindings() {
		input.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					lastInput.add(input.getText());
					append("> " + input.getText() + "\n");
					prevInput = lastInput.size() - 1;
					inputHandler(input.getText());
					input.setText("");
					mode = Mode.TYPING;
			}
		});
		
		input.getInputMap().put(KeyStroke.getKeyStroke("UP"), "recall");
		input.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "derecall");
		input.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "compleate");
		input.getActionMap().put("recall", new AbstractAction() {
			private static final long serialVersionUID = -7466542331748301130L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (prevInput < lastInput.size() && prevInput >= 0) {
					input.setText(lastInput.get(prevInput));
					prevInput--;
				}
			}
		});
		input.getActionMap().put("derecall", new AbstractAction() {
			private static final long serialVersionUID = 594827267666791523L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (prevInput + 2 < lastInput.size()) {
					input.setText(lastInput.get(prevInput + 2));
					prevInput++;
				} else if (prevInput + 1 < lastInput.size()) {
					input.setText("");
					prevInput = lastInput.size() - 1;
				}
			}
		});
		input.getActionMap().put("compleate", new AbstractAction() {
			private static final long serialVersionUID = -109397958925344323L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (mode == Mode.COMPLEATION) {
					mode = Mode.TYPING;
					input.setCaretPosition(input.getText().length());
				}
			}
		});
	}
	
	public void setGeneration(Generation newGen) {
		generation = newGen;
	}
	
	public Generation getGeneration() {
		return generation;
	}
	
	public Timer getTimer() {
		return timer;
	}
	
	private void createSpecialMap() {
		specialActions.put("(%(\\w)+%(\\s)*=(\\s)*(\\w|-)+)", new SetTerminalCommand(this));
		specialActions.put("(%(\\w)+%)", new GetTerminalCommand(this));
	}

	private void createActionMap() {
		actions.put("help", new HelpTerminalCommand(this, actions));
		actions.put("cls", new ClearTerminalCommand(this, "cls"));
		actions.put("clear", new ClearTerminalCommand(this, "clear"));
		actions.put("quit", new QuitTerminalCommand(this));
		actions.put("get", new GetTerminalCommand(this));
		actions.put("set", new SetTerminalCommand(this));
		actions.put("new", new CreateNewGenerationTerminalCommand(this));
		actions.put("show", new ShowGraphicsTerminalCommand(this));
		actions.put("run", new RunSimulationTerminalCommand(this));
		actions.put("pause", new PauseTerminalCommand(this));
		actions.put("resume", new ResumeTerminalCommand(this));
		actions.put("generation", new GetGenerationTerminalCommand(this));
	}
	
	public Dimension getTextDim() {
		return text.getSize();
	}
	
	public Dimension getInputDim() {
		return input.getSize();
	}
	
	public void append(String toAppend) {
		StringBuilder toInsert = new StringBuilder();
		if (!appendRequests.isEmpty()) {
			int size = appendRequests.size();
			for (int i = 0; i < size; i++) {
				toInsert.append(appendRequests.poll());
			}
		}
		toInsert.append(toAppend.replaceAll("\n", "<br>").replaceAll("\t", getTab()));
		HTMLDocument doc = (HTMLDocument) text.getStyledDocument();
		try {
			doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), toInsert.toString());
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		text.selectAll();
	}
	
	public void printStackTrace(String trace) {
		append("<font color=\"red\">" + trace + "</font>");
	}
	
	public static String getTab() {
		return "&nbsp;&nbsp;&nbsp;&nbsp;";
	}
	
	public static String getSpace() {
		return "&nbsp;";
	}
	
	private void inputHandler(String input) {
		try {
			TerminalCommand t = actions.get(input.split(" ")[0]);
			if (t != null) {
				t.excecuteCommand(input);
			} else {
				Set<String> regexes = specialActions.keySet();
				for (String s : regexes) {
					if (Pattern.matches(s, input)) {
						specialActions.get(s).excecuteCommand(input);
						return;
					}
				}
				throw new TerminalCommandNotFoundException("No Command Named: " + input.split(" ")[0], input);
			}
		} catch (TerminalException e) {
			printStackTrace(e.getLocalizedMessage() + "<br>");
		}
	}
	
	public CompleationTask getNewTask(String com, int p) {
		return new CompleationTask(com, p);
	}
	
	public void setMode(Mode newMode) {
		mode = newMode;
	}
	
	public void clear() {
		String fontFamily = font.getFamily();
		text.setText(String.format("<html><style>"+
		"body {font-family: \"%s\"; font-size: \"%2$d\"}" +
		 "</style></html>", fontFamily, font.getSize()));
	}
	
	public void quit() {
		JFrame root = (JFrame) SwingUtilities.getWindowAncestor(this);
		root.dispatchEvent(new WindowEvent(root, WindowEvent.WINDOW_CLOSING));
	}
	
	public class CompleationTask implements Runnable {
		private String compleation;
		private int pos;
		
		public CompleationTask(String com, int p) {
			compleation = com;
			pos = p;
		}

		@Override
		public void run() {
			StringBuilder newText = new StringBuilder(input.getText());
			newText.insert(pos, compleation);
			input.setText(newText.toString());
			input.setCaretPosition(pos + compleation.length());
			input.moveCaretPosition(pos);
			mode = Mode.COMPLEATION;
		}
	}
}
