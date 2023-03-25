package try1;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class GUI extends JFrame {
	private List<LocalDate> week;
	private List<Double> budgetB, budget; //break vs non-break budget
	private List<Entry> work;
	private JLabel msg;
	private static final long serialVersionUID = 1L;
	private File constFile, paramFile, schoolFile;
	private double consts[];
	private double daily;
	private DateTimeFormatter formatter;
	private JButton toggleBreak;
	private boolean onBreak;
	public GUI () {
		super("TimeBudget");
		pack();
		setLocationRelativeTo(null);
		setLayout(new FlowLayout());
		onBreak = false;
		formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		constFile = new File("constants.txt");
		paramFile = new File("params.txt");
		schoolFile = new File("school.txt");
		consts = new double[7];
		daily = 0;
		msg = new JLabel("hi");
		add(msg);
		
		toggleBreak = new JButton("off break");
		toggleBreak.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onBreak = !onBreak; //toggle bool
					System.out.println(onBreak);
					toggleBreak.setText(onBreak? "on break" : "off break");
					printBudget();
				}
			}
		);
		
		add(toggleBreak);
		week = new LinkedList<LocalDate>();
		budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
		work = new ArrayList<Entry>();
		readConstants();
		readParams();
		readSchool();
		
		LocalDate curr = LocalDate.now();
		double weekendBudget = 0.0;
		for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
		{
			week.add(curr);
			//System.out.println(consts[dayToIndex(curr.getDayOfWeek())]);
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr.getDayOfWeek())]);
			System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
			if (curr.getDayOfWeek() == DayOfWeek.SATURDAY) weekendBudget = budget.get(i);
		}
		
		budgetB = new ArrayList<Double>(Collections.nCopies(7,weekendBudget));
		
		class CustomSort implements Comparator<Entry> { //MY FIRST JAVA CUSTOM SORT FUNC!
			public int compare(Entry o1, Entry o2) {
				if (o1.isFixed && !o2.isFixed) return -1; //(1) first make fixed event go to top
				else if (!o1.isFixed && o2.isFixed) return 1; 
				if (o1.deadline.isEqual(o2.deadline)) { //(3) by harder difficulty first
					System.out.println(o1.deadline + "=" + o2.deadline);
					return Double.compare(o2.diff,o1.diff);
				}
				return o1.deadline.compareTo(o2.deadline); //(2) sort by deadline
			}
		}
		
		
		Collections.sort(work, new CustomSort());
		System.out.println("Name\t\tDifficulty Hrs\tDeadline\tFixed?");
		for (int i = 0; i < work.size(); i++) //print sorted homework entries
			System.out.println(work.get(i).toString());
	}
	
	
	void printBudget() {
		LocalDate curr = LocalDate.now();
		System.out.println("Date\tdayOfWeek  budget");
		if (onBreak) {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + budgetB.get(i));
		}
		else {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
		}
	}

	private int dayToIndex(DayOfWeek x) {
		switch (x) { 
		case MONDAY:
			return 0;
		case TUESDAY:
			return 1;
		case WEDNESDAY:
			return 2;
		case THURSDAY:
			return 3;
		case FRIDAY:
			return 4;
		case SATURDAY:
			return 5;
		case SUNDAY:
			return 6;
		};
		return -1;
	}
	private void readConstants() {
		Scanner getX = null;
		try {
			getX = new Scanner(constFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX != null)
		{
			for (int i = 0; i < 7 && getX.hasNextLine(); i++)
			{
				double total = 0;
				String temp[] = getX.nextLine().split(",");
				for (int j = 0; j < temp.length; j++)
				{
					try {
						total += Double.valueOf(temp[j]);
					} catch (Exception e) {  };
				}
				//System.out.println(total);
				consts[i] = total;
			}
			getX.close();
			return;
		}
		return;
	}
	private void readParams() {
		Scanner getX = null;
		try {
			getX = new Scanner(paramFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX != null)
		{
			while(getX.hasNextLine())
			{
				String temp[] = getX.nextLine().split(",");
				//System.out.println(temp[0]);
				try {
					daily += Double.valueOf(temp[1]);
				} catch (Exception e) {  };
			}
			getX.close();
			//System.out.println("daily " + daily);
			return;
		}
	}
	private void readSchool() {
		System.out.println("school.txt:");
		Scanner getX = null;
		
		try {
			getX = new Scanner(schoolFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		
		if (getX != null)
		{
			while(getX.hasNextLine())
			{
				String line = getX.nextLine();
				String name = line.substring(1, line.indexOf("\"",1));
				line = line.substring(line.indexOf("\"", 1)+2);
				//System.out.println(name);
				String temp[] = line.split(",");
				boolean isFixed = (temp.length > 3);

				try {
					work.add(new Entry(name, Double.valueOf(temp[0]), Double.valueOf(temp[1]),LocalDate.parse(temp[2], formatter), isFixed));
				} catch (Exception e) {  };
			}
			getX.close();
			return;
		}
		return;
	}
	private class Entry {
		String name;
		double diff;
		double hr;
		LocalDate deadline;
		boolean isFixed;
		Entry(String n, double di, double h, LocalDate de, boolean t) {
			name = n;
			diff = di;
			hr = h;
			deadline = de;
			isFixed = t;
		}
		
		public String toString() {
			return String.format(name + "\t" + diff + "\t" + hr + "\t" + formatter.format(deadline) + "\t" + isFixed);
		}
	}
}