package table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import gui.Gui;
public class Tabella {
	public Integer tabella[][];
	Integer tabellaNumbers[];
	int maxColElements;
	int maxRowElements;
	public List<Integer> drawnNumbers;
	List<Integer> leftNumbers; //numbers still to draw for winning
	Random random;
	boolean win;
	int myLastDrawnNumber;
	public Tabella() {
		this.myLastDrawnNumber = -1;
		tabella = new Integer[3][9];
		tabellaNumbers = new Integer[15];
		random = new Random();
		win = false;
		maxColElements = 2; //no more than 2 number per column
		maxRowElements = 5; //no more than 5 number per row
		drawnNumbers = new ArrayList<Integer>();
		leftNumbers = null;
		initializeTabella();
		printTabella();
	}
	public void addNumber(Integer number) {
		if(drawnNumbers.indexOf(number) < 0 && number != -1) {
		if(win == true) { //accept new numbers only if I have not won
			return;
		}
		drawnNumbers.add(number);
		//System.out.println(drawnNumbers.toString());
		} else {
			System.out.println("Number "+number+" already in queue");
		}
	}
	public void removeLeftNumber(Integer number) {
		int index = leftNumbers.indexOf(number);
		if(index > -1) {
			leftNumbers.remove(index);
		}
	}
	public void addLeftNumber(Integer number) {
		leftNumbers.add(number);
	}
	public void initializeTabella() {
		//no more than 2 numbers per column
		List<Integer> tmp_numbers = new ArrayList<Integer>();
		List<Integer> recovery_numbers = new ArrayList<Integer>();
		Integer usedNumbers[] = new Integer[90];
		int maxNumber = 89;
		int n = -1;
		int i = 0;
		int j = 0;
		int counter = 0;
		//no number is used
		for(i = 0; i < 90; i++) {
			usedNumbers[i] = 0;
		}
		i = 0;
		j = 0;
		//set default tabella
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++) {
				tabella[i][j] = -1;
			}
		}
		for(i = 0; i < 15; i++) {
			tabellaNumbers[i] = -1;
		}
		i = 0;
		j = 0;
		counter = 0;
		//at least one number per column
		while(i < 9) {
			n = random.nextInt(9);
			n += i*10;
			if(n == 0 && i == 0 ) { //handle the 0 case
				continue;
			}
			if(usedNumbers[n-1] > 0) { //number already drawn
				continue;
			}
			usedNumbers[n-1] = 1; //set the number as used
			tmp_numbers.add(n);
			i++;
		}
		i = 0;
		while(i < 6) {
			n = random.nextInt(maxNumber-1)+1;
			if(n == 0) {
				continue;
			}
			if(usedNumbers[n-1] > 0) { //number already drawn
				continue;
			}
			int tmp_d = n/10;
			if(n == 90) { //handle 90 since it would give a different tmp_d
				tmp_d = 8;
			}
			counter = 0;
			for(j = 0; j < 10; j++) { //count how many numbers per column
				int index = (tmp_d*10)+j;
				if(index == 0) {
					continue;
				}
				if(usedNumbers[index-1] > 0) {
					counter++;
				}
			}
			if(counter >= maxColElements) { //it should never be greater than
				continue;
			}
			usedNumbers[n-1] = 1;
			tmp_numbers.add(n);
			i++;
		}
		recovery_numbers = new ArrayList<Integer>(tmp_numbers);
		Collections.shuffle(tmp_numbers, random); //shuffle drawn numbers
		Collections.shuffle(recovery_numbers, random); //shuffle drawn numbers
		int d = -1;
		//fill the tabella
		i = 0;
		while(!tmp_numbers.isEmpty()) { //while there are numbers to be put inside the table
				int number = tmp_numbers.get(0); //take the first number
				int row = random.nextInt(3); //random row to insert into
				boolean ok = false;
				d = number/10; //ten of number
				if(d == 9) { //handle 90 case
					d = 8;
				}
				//fill the row
				for(j = 0; j < 5; j++) { //check if there are already numbers of the same column in its row
					if(tabella[row][j] == -1) {
						ok = true;
						break;
					}
					if((tabella[row][j])/10 == d) { //there is already a number of the same ten
						ok = false;
						break;
					}
					if(j == 4) {
						ok = false;
					}
				}
				if(ok) {
					tabella[row][j] = tmp_numbers.remove(0);
				} else {
					i++;
					if(i > 20) { //after 10 attempts restart again
						//System.exit(0);
						tmp_numbers = null;
						tmp_numbers = new ArrayList<Integer>(recovery_numbers);
						for(i = 0; i < 3; i++) {
							for(j = 0; j < 9; j++) {
								tabella[i][j] = -1;
							}
						}
						i = 0;
					}
				}
		}
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++) {
				int tmp = -1;
				if(tabella[i][j] == -1) {
					continue;
				}
				d = tabella[i][j]/10;
				if(j == d) {
					continue;
				} else {
					int tmp_d = tabella[i][j]/10;
					if(tmp_d == 9) {
						tmp_d = 8;
					}
					tmp = tabella[i][tmp_d];
					tabella[i][tmp_d] = tabella[i][j];
					tabella[i][j] = tmp;
					j = -1; //restart from the beginning
				}
			}
		}
		int t = 0;
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++) {
				if(tabella[i][j] != -1) {
					tabellaNumbers[t] = tabella[i][j];
					t++;
				}
			}

		}
		leftNumbers = new ArrayList<Integer>(Arrays.asList(tabellaNumbers));
		System.out.println(Arrays.toString(tabellaNumbers));
	}
	public Integer[] getTabellaNumbers() {
		return tabellaNumbers;
	}
	public void printTabella() {
		int i, j;
		i = j = 0;
		for(i = 0; i < 3; i++) {
			for(j = 0; j < 9; j++) {
				if(tabella[i][j] != -1) {
					System.out.print(tabella[i][j]);
				} else {
					System.out.print("-");
				}
				System.out.print("\t");
			}
			System.out.println("\n");
		}
	}
	public int draw() {
		int n = 0;
		while(n == 0 || drawnNumbers.indexOf(n) > -1) { //till number is already in queue or it is the first draw
			n = random.nextInt(90)+1;
		}
		return n;
	}
	public List<Integer> getDrawnNumbers() {
		return drawnNumbers;
	}
	public boolean checkWin() {
		boolean res = false;
		if(leftNumbers.size() == 0) {
			win = true;
			res = true;
		}
		return res;
	}
	public void setWin(boolean what) {
		win = what;
	}
	public boolean getWin() {
		return win;
	}
	public void restoreStatus() {
		int index = -1;
		//restore leftNumbers
		leftNumbers = new ArrayList<Integer>(Arrays.asList(tabellaNumbers));
		for(Integer n : drawnNumbers) {
			index = leftNumbers.indexOf(n);
			if(index > -1) {
				leftNumbers.remove(index);
			}
		}
	}
	public int getMyLastDrawnNumber() {
		return myLastDrawnNumber;
	}
	public void setMyLastDrawnNumber(int myLastDrawnNumber) {
		this.myLastDrawnNumber = myLastDrawnNumber;
	}
}
