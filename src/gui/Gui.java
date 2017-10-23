package gui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import ring.Ring;
import rmiServer.RMIServer;
import table.Tabella;
import thread.MsgThread;
import utils.Utils.Types;
public class Gui extends JFrame {
	JButton buttons[] = null;
	JButton winButton = null;
	JButton drawButton = null;
	JTextArea lastNumberView;
	JTextArea drawnNumbersView;
    JPanel panel = null;
    JPanel panel2 = null;
	Tabella tabella;
	RMIServer rmiServer;
	Ring ring;
	String buttonsString[] = null;
	boolean activeButtons[][] = null;
	public Gui(RMIServer rmiServer) {
		this.rmiServer = rmiServer;
		this.rmiServer.setGui(this);
		this.tabella = rmiServer.getTabella();
		this.ring = rmiServer.getRing();
        initUI();
    }
	private void initUI() {
        this.panel = new JPanel();
        this.panel2 = new JPanel();
        lastNumberView =  new JTextArea();
        lastNumberView.setEditable(false);
        drawnNumbersView = new JTextArea(20,20);
        drawnNumbersView.setEditable(false);
        buttonsString = new String[27];
        buttons = new JButton[27];
        winButton = new JButton("Call Bingo");
        drawButton = new JButton("Drawn A Number");
        disable_drawButton();
        activeButtons = new boolean[3][9];
        //listener for win Button
        winButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	tabella.setWin(true);
            }
        });
        //listener for draw Button
        drawButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){

            	int n = tabella.draw();
				tabella.setMyLastDrawnNumber(n);
				ring.sendDrawnNumber(Types.DRAWN_NUMBER, n);
				disable_drawButton();
            }
        });
        initializeButtons();
        setLayout(new BorderLayout());
        drawnNumbersView.setFont(drawnNumbersView.getFont().deriveFont(12f));
        lastNumberView.setFont(lastNumberView.getFont().deriveFont(40f));
        panel2.setLayout(new BorderLayout());
        panel2.add(winButton, BorderLayout.EAST);
        panel2.add(lastNumberView, BorderLayout.NORTH);
        panel2.add(drawnNumbersView, BorderLayout.SOUTH);
        panel2.add(drawButton, BorderLayout.WEST);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setLayout(new GridLayout(3, 9, 5, 5));
        for (int i = 0; i < buttonsString.length; i++) {
            if (buttonsString[i].equals("-")) {
            	JButton tmpButton = new JButton("");
            	tmpButton.setEnabled(false);
                panel.add(tmpButton);
                buttons[i] = tmpButton;
            } else {
               	final JButton tmpButton = new JButton(buttonsString[i]);
                panel.add(tmpButton);
                buttons[i] = tmpButton;
                tmpButton.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e){
                	  int index = Arrays.asList(buttons).indexOf(tmpButton);
                	  int row = -1;
                	  int col = -1;
                	  Integer[] tmp = null;
                	  tmp = returnMatrixIndex(index);
                	  row = tmp[0];
                	  col = tmp[1];
                	  if(!activeButtons[row][col]) {
                    	  tabella.removeLeftNumber(Integer.parseInt(tmpButton.getText()));
                    	  tmpButton.setBackground(Color.GREEN);
                    	  activeButtons[row][col] = true;
                    	  //for MacOs
                    	  tmpButton.setOpaque(true);
                    	  tmpButton.setBorderPainted(false);
                	  } else {
                    	  tabella.addLeftNumber(Integer.parseInt(tmpButton.getText()));
                    	  tmpButton.setBackground(null);
                    	  activeButtons[row][col] = false;
                    	  //for MacOs
                    	  tmpButton.setOpaque(false);
                    	  tmpButton.setBorderPainted(true);
                	  }
                  }
                });
              }
            }
        panel2.add(panel, BorderLayout.CENTER);
        add(panel2);
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
	public void fillLastNumberView(Integer number){
		String lastNumber = String.valueOf(number);
		lastNumberView.setText("Drawn Number: " + lastNumber);
	}
	public void fillDrawnNumbersView(){
		int i, j;
		i = j = 0;
		List<Integer> tmpDrawnNumbers = tabella.getDrawnNumbers();
		drawnNumbersView.setText("");
		for(i = 0; i < 10; i++) {
			for(j = 1; j < 11; j++) {
				Integer tmp = i*10+j;
				if(tmp == 0 || tmp > 90) {
					break;
				} else if(tmpDrawnNumbers.indexOf(tmp) > -1) {
					drawnNumbersView.append(tmp.toString()+"\t");
				} else {
					drawnNumbersView.append("- \t");
				}
			}
			drawnNumbersView.append("\n");
		}
	}
	public Integer[] returnMatrixIndex(Integer index) {
		Integer res[] = new Integer[2];
		int row = -1;
		int col = -1;
		row = index/9;
		col = index%9;
		res[0] = row;
		res[1] = col;
		return res;
	}
	public void initializeButtons() {
		int i, j;
		int index = 0;
		i = j = 0;
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++, index++) {
				activeButtons[i][j] = false;
				if(tabella.tabella[i][j] != -1) {
					buttonsString[index] = (tabella.tabella[i][j]).toString();
				} else {
					buttonsString[index] = ("-").toString();
				}
			}
		}
	}
	public void restoreStatus() {
		int i, j;
		i = j = 0;
		JButton tmpButton = null;
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++) {
				if(tabella.tabella[i][j] != -1) {
					tmpButton = buttons[9*i+j];
					if(tabella.getDrawnNumbers().indexOf(tabella.tabella[i][j]) > -1) { //drawn number, disable it
						tmpButton.setBackground(Color.GREEN);
						activeButtons[i][j] = true;
						// for MacOs
						tmpButton.setOpaque(true);
						tmpButton.setBorderPainted(false);
					} else {
						activeButtons[i][j] = false;
                  	  tmpButton.setBackground(null);
                  	  //for MacOs
                  	  tmpButton.setOpaque(false);
                  	  tmpButton.setBorderPainted(true);
					}
				}
			}
		}
	}
	public void setTabella(Tabella tabella) {
		this.tabella = tabella;
	}
	public void enable_drawButton() {
		drawButton.setEnabled(true);
		drawButton.setBackground(Color.ORANGE);
		// for MacOs
		drawButton.setOpaque(true);
		drawButton.setBorderPainted(false);
	}
	public void disable_drawButton() {
		drawButton.setEnabled(false);
		drawButton.setBackground(null);
		// for MacOs
		drawButton.setOpaque(false);
		drawButton.setBorderPainted(true);
	}
	public void setInfoText(String text) {
		lastNumberView.setText(text);
	}
}
