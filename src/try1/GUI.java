package try1;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
	private List<Double> budgetB, budget, hrsLeft; //break vs non-break budget
	private List<Entry> work;
	private List<List<Integer>> assign;
	private JLabel msg;
	private static final long serialVersionUID = 1L;
	private File constFile, paramFile, schoolFile;
	private double consts[];
	private double daily;
	private DateTimeFormatter formatter;
	private JButton toggleBreak;
	private boolean onBreak;
	private int weekDayIdeal = 1;
	private int weekEndIdeal = 6;
	private int oldWorkSize;
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
					distrAlg();
					printBudget();
					printDates();
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
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr)]);
			System.out.println(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + ":" + budget.get(i));
			//System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
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
		for (int i = 0; i < oldWorkSize; i++) //print sorted homework entries
			System.out.println(work.get(i).toString());
		
		distrAlg();
		printDates();
		
		System.out.println("Name\t\tDifficulty Hrs\tDeadline\tFixed?");
		for (int i = 0; i < work.size(); i++) //print sorted homework entries
			System.out.println(work.get(i).toString());
	}
	
	int Ideal(LocalDate x) {
		int y = dayToIndex(x);
		if (y == 5 || y == 6) return weekEndIdeal;
		return weekDayIdeal;
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

	void printDates() {
		System.out.println("printing dates, remaining budget out of max, assigned work");
		if (onBreak) {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + "\n\t");
				for (int j = 0; j < assign.get(i).size(); j++) System.out.println(work.get(assign.get(i).get(j)));
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + budgetB.get(i) + "h free\n\n");
			}
		}
		else {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + "\n");
				for (int j = 0; j < assign.get(i).size(); j++) System.out.println("\t" + work.get(assign.get(i).get(j)));
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + budget.get(i) + "h free\n\n");
			}
		
		}
	}
	private void distrAlg() {	
		if (onBreak) hrsLeft = new ArrayList<Double>(budgetB);
		else hrsLeft = new ArrayList<Double>(budget);
		assign = new ArrayList<List<Integer>>(); //
		
		for (int i = 0; i < 7; i++)
		{
			ArrayList<Integer> temp = new ArrayList<Integer>();
			assign.add(temp);
		}
		
		for (int i = 0; i < 7; i++) System.out.println(hrsLeft.get(i));
		
		for (int i = 0; i < oldWorkSize; i++) {
			Entry curr = work.get(i);
			System.out.println(formatter.format(curr.deadline) + ":" + ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline) + "days " + dayToIndex(curr.deadline));
			long n = ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline);
			if (n >= 0 && n < 7) {
				System.out.println("HI");
				for (int p = 0; p < 7;p++) System.out.println(hrsLeft.get(p));
				//verify n is corresponding week index for curr.deadline System.out.println(formatter.format(curr.deadline) + "=?"+ formatter.format(week.get((int) n)));
				if (curr.isFixed) 
				{
					System.out.println("\tfixed");
					assign.get((int) n).add(i); //index of work assigned to date n's list
					hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
					
				}
				else {
					int idealN = -1;
					
					for (int j = 0; j < n; j++) //assign today? tmrw? day after? 
					{
						if (hrsLeft.get(j) - curr.hr >= 0)
						{
							if (idealN == -1) idealN = j;
							else {
								double id = hrsLeft.get(idealN) - curr.hr - Ideal(week.get(idealN));
								double now = hrsLeft.get(j) - curr.hr - Ideal(week.get(j)); //budget if assigned to j AND do ideal leet+play for that weekday/end
								if (now > id) idealN = j;
							}
						}
					}
					if (idealN == -1) //all dates before deadline no time even without leet+play
					{ //forced to assign assign the day of deadline
									
						System.out.println("\n\tforced");
						if (hrsLeft.get((int) n) - curr.hr < 0) //if need to split bc deadline also not enough time
						{
							
							//work.add(new Entry(work.get(i).name, work.get(i).diff, work.get(i).hr, work.get(i).deadline, work.get(i).isFixed)); //make copy of original entry at back of work[], cannot override original (for break toggle)
							boolean doCont = true;
							for (int j = 0; j < n && doCont; j++) //assign today? tmrw? day after? 
							{
								if (hrsLeft.get(j) > 0) //has some time to squeeze this task
								{
									System.out.println("hey! day "+  week.get(j) + " has hrs: " + hrsLeft.get(j));
									if (work.get(work.size()-1).hr <= hrsLeft.get(j)) { //done! all parts fitted
										System.out.println("it's okay, " + work.get(work.size()-1).name + " only needs " + work.get(work.size()-1).hr);
										doCont = false;
										assign.get(j).add(work.size()-1); //direct assign to day j
										hrsLeft.set(j, hrsLeft.get(j) - work.get(work.size()-1).hr); //update hours of day j
									}
									else { //gotta do more splitting
										work.add(new Entry(work.get(i).name, work.get(i).diff, work.get(i).hr, work.get(i).deadline, work.get(i).isFixed)); //copy
										work.get(work.size()-2).hr = hrsLeft.get(j); //assign previous copy to day j
										assign.get(j).add(work.size()-2);
										
										work.get(work.size()-1).hr -= hrsLeft.get(j); //save remaining portion of task in new copy
										hrsLeft.set(j, 0.0); //which eats up all of day j's hrs left.
										System.out.println("now: " + work.get(work.size()-2).hr + " and " + work.get(work.size()-1).hr);
									}
								}
							}
						}
						else { //deadline only day enough time, so assign to deadline
							assign.get((int) n).add(i); //index of work assigned to date n's list
							hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
						}
					}
					else
					{
						System.out.println("\tideal" + idealN + curr.name);
						assign.get(idealN).add(i); //index of work assigned to date idealN's list
						System.out.println(assign.get(idealN));
						hrsLeft.set(idealN, hrsLeft.get(idealN) - curr.hr);
					}
				}
			}
			
		}
		
		//weekDayIdeal
		
	}
	private int dayToIndex(LocalDate y) {
		DayOfWeek x = y.getDayOfWeek();
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
			oldWorkSize = work.size();
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
