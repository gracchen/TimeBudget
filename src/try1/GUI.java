package try1;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class GUI extends JFrame {
	private JPanel home, tasks, settings;
	private List<LocalDate> week;
	private List<Double> budgetB, budget, hrsLeft; //break vs non-break budget
	private List<Entry> work, review; //list of review entries
	private List<List<Integer>> assign;
	private List<Integer> done; //list of entry indexes marked done, to be deleted upon closing app
	private List<List<Integer>> split; //list of entry indexes that were split (1st elem = original, rest is split children)
	private JLabel msg;
	private static final long serialVersionUID = 1L;
	private File constFile, paramFile, schoolFile, classFile;
	private double consts[];
	private double daily;
	private DateTimeFormatter formatter;
	private JButton toggleBreak;
	private boolean onBreak;
	private int weekDayIdeal = 1;
	private int weekEndIdeal = 6;
	private int oldWorkSize;
	private List<String> choices;
	private JComboBox<String> drop;
	private JPanel show;
	private JLabel stats, leet, play;
	private List<JCheckBox> checks;
	private JTabbedPane tabPane;
	private LocalDate week1 = LocalDate.of(2023, 3, 20); //spring quarter instruction starts April 3
	public GUI () {
		super("TimeBudget");
		pack();
		setLocationRelativeTo(null);
		home = new JPanel();
		add (home);
		home.setLayout(new FlowLayout());
		tasks = new JPanel();
		settings = new JPanel();

		tabPane = new JTabbedPane();
		tabPane.addTab("Home", home);
		tabPane.addTab("Tasks", tasks);
		tabPane.addTab("Settings", settings);
		add(tabPane);

		onBreak = false; //default break mode
		formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
		constFile = new File("constants.txt");
		paramFile = new File("params.txt");
		schoolFile = new File("school.txt");
		classFile = new File("class.txt");
		consts = new double[7];
		daily = 0;

		week = new LinkedList<LocalDate>();
		budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
		work = new ArrayList<Entry>();
		readConstants();
		readParams();
		readSchool();

		//for (int i = 0; i < )

		LocalDate curr = LocalDate.now();
		double weekendBudget = 0.0;
		choices = new ArrayList<String>();
		choices.add("Today");
		choices.add("Tomorrow");
		for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
		{
			week.add(curr);
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr)]);
			if (curr.getDayOfWeek() == DayOfWeek.SATURDAY) weekendBudget = budget.get(i);
			if (i >= 2) choices.add((curr.getDayOfWeek()) + ", " + formatter.format(curr));
		}

		budgetB = new ArrayList<Double>(Collections.nCopies(7,weekendBudget));

		Collections.sort(work, new CustomSort());
		System.out.println("Name\t\tDifficulty Hrs\tDeadline\tFixed?");
		for (int i = 0; i < oldWorkSize; i++) //print sorted homework entries
			System.out.println(work.get(i).toString());

		distrAlg();
		printDates();
		printWork();
		readClass();
		//generate all review needs:

		//GUI PART!!!

		msg = new JLabel("hi");
		home.add(msg);

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
						printWork();
						showDate(drop.getSelectedIndex()); //update
					}
				}
				);

		home.add(toggleBreak);
		//DROPDOWN GUI:
		drop = new JComboBox<String>(choices.toArray(new String[choices.size()])); //param = array of options
		home.add(drop);
		showDate(0); //default show today
		drop.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent event) {
						if(event.getStateChange() == ItemEvent.SELECTED)
							showDate(drop.getSelectedIndex());
					}
				}
				);

		//settings
		done = new ArrayList<Integer>();
		//writeWork();
	}

	private void writeFinished(){
		FileWriter fw = null;
		try
		{
			String filename= "done.txt";
			fw = new FileWriter(filename,true); //the true will append the new data
			for (int i = 0; i < done.size(); i++)
			{
				fw.write(work.get(done.get(i)).toString() + "," + formatter.format(LocalDate.now()) + "\n");//appends the string to the file
				work.remove((int)done.get(i));
			}
		}
		catch(IOException ioe)
		{
			System.err.println("IOException: " + ioe.getMessage());
			return;
		}
		finally
		{
			try {
				fw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/*private void commitWork() {
		int offset = 0;
		Collections.sort(split, null);
		for (int i = 0; i < split.size(); i++, offset++)
			work.remove((int)(split.get(i)-offset));
	}*/

	private void writeWork() {
		class sortByFirst implements Comparator<List<Integer>> { //MY FIRST JAVA CUSTOM SORT FUNC!
			public int compare(List<Integer> o1, List<Integer> o2) {
				return Integer.compare(o1.get(0),o2.get(0));
			}
		}
		Collections.sort(split, new sortByFirst()); 
		for (int i = 0; i < split.size(); i++)
		{
			for (int j = 0; j < split.get(i).size(); j++)
				System.out.print(split.get(i).get(j) + " ");
			System.out.println();
		}
		Formatter y = null;
		try {
			y = new Formatter("~school.txt");
			//System.out.println("You created a file");
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (y != null)
		{
			int j = 0; //pointer to split
			for (int i = 0; i < work.size(); i++)
			{
				if (j < split.size() && i == split.get(j).get(0)) //is work element parent of dupes?
				{
					System.out.println(work.get(i).name);
					j++;
				}
				y.format("%s\n", work.get(i).toString());
			}

			y.close();

			schoolFile.delete();

			if (schoolFile.exists()) System.out.println("unable to edit school.txt");
			File edit = new File ("~school.txt");

			edit.renameTo(new File ("school.txt"));

			schoolFile = new File ("school.txt");
		}
	}
	void showDate(int index) { 
		//shows both leetcode + play and assigned stuff
		msg.setText(String.valueOf(index));
		if (show != null) {
			show.removeAll(); //remove previous display
			remove(show);
		}
		else show = new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		show.setLayout(new GridBagLayout());

		home.add(show);

		c.fill = GridBagConstraints.HORIZONTAL;    //fill entire cell with text to center
		c.gridwidth = 4; c.gridx = 0; c.gridy = 0;   //coords + width of msg element
		checks = new ArrayList<JCheckBox>();
		for (int i = 0; i < assign.get(index).size(); i++) {
			checks.add(new JCheckBox(work.get(assign.get(index).get(i)).name + ", " + work.get(assign.get(index).get(i)).hr + "h"));
			show.add(checks.get(checks.size()-1), c);
			c.gridy++;
		}

		stats = new JLabel(hrsLeft.get(index) + "/" + (onBreak? budgetB.get(index) : budget.get(index)) + "h free"); 
		show.add(stats, c);
		if (hrsLeft.get(index) > 0)
		{
			c.gridy++; //%.2f to format double show 2 decimal places max
			leet = new JLabel(niceDur(hrsLeft.get(index) * 0.6) + " for CS");
			show.add(leet,c);
			c.gridy++;
			play = new JLabel(String.format("%.1fh to play", hrsLeft.get(index) * 0.4));
			play = new JLabel(niceDur(hrsLeft.get(index) * 0.4) + " to play");
			show.add(play,c);
		}	
	}

	String niceDur(double x) {
		if (Math.ceil(x) == Math.floor(x)) //no decimal part
			return (String.format("%.0fh", x));
		return (String.format("%.0fh %.0fm", x, (x - Math.floor(x))*60));
	}


	class CustomSort implements Comparator<Entry> { //MY FIRST JAVA CUSTOM SORT FUNC!
		public int compare(Entry o1, Entry o2) {
			if (o1.isFixed && !o2.isFixed) return -1; //(1) first make fixed event go to top
			else if (!o1.isFixed && o2.isFixed) return 1; 
			if (o1.deadline.isEqual(o2.deadline)) { //(3) by harder difficulty first
				//System.out.println(o1.deadline + "=" + o2.deadline);
				return Double.compare(o2.diff,o1.diff);
			}
			return o1.deadline.compareTo(o2.deadline); //(2) sort by deadline
		}
	}

	int Ideal(LocalDate x) {
		int y = dayToIndex(x);
		if (y == 5 || y == 6) return weekEndIdeal;
		return weekDayIdeal;
	}

	void printWork() {
		System.out.println("Name\t\tDifficulty Hrs\tDeadline\tFixed?");
		for (int i = 0; i < oldWorkSize; i++) //print sorted homework entries
			System.out.println(work.get(i).toString());

		if (work.size() != oldWorkSize)
			System.out.println("________________________");

		for (int i = oldWorkSize; i < work.size(); i++) //print sorted homework entries
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
		split  = new ArrayList<List<Integer>>();  //reset split
		work.subList(oldWorkSize, work.size()).clear(); //removes any copies of originally read work
		//that was inserted after, from indexes oldWorkSize, oldWorkSize+1......work.size()-1
		assign = new ArrayList<List<Integer>>(); //reset assign

		for (int i = 0; i < 7; i++)
		{
			ArrayList<Integer> temp = new ArrayList<Integer>();
			assign.add(temp);
		}

		//for (int i = 0; i < 7; i++) System.out.println(hrsLeft.get(i));

		for (int i = 0; i < oldWorkSize; i++) {
			Entry curr = work.get(i);
			//System.out.println(formatter.format(curr.deadline) + ":" + ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline) + "days " + dayToIndex(curr.deadline));
			long n = ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline);
			if (n >= 0 && n < 7) {
				//System.out.println("HI");
				//for (int p = 0; p < 7;p++) System.out.println(hrsLeft.get(p));
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

						//System.out.println("\n\tforcing: " + work.get(i) +"\n\t BEFORE:");
						//printDates();
						if (hrsLeft.get((int) n) - curr.hr < 0) //if need to split bc deadline also not enough time
						{
							List<Integer> temp = new ArrayList<Integer>();
							temp.add(i); //first element is parent original

							work.add(new Entry(work.get(i).name, work.get(i).diff, work.get(i).hr, work.get(i).deadline, work.get(i).isFixed)); //make copy of original entry at back of work[], cannot override original (for break toggle)
							temp.add(work.size()-1); //record first child of original
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
										temp.add(work.size()-1); //save new child
										work.get(work.size()-2).hr = hrsLeft.get(j); //assign previous copy to day j
										assign.get(j).add(work.size()-2);

										work.get(work.size()-1).hr -= hrsLeft.get(j); //save remaining portion of task in new copy
										hrsLeft.set(j, 0.0); //which eats up all of day j's hrs left.
										System.out.println("now: " + work.get(work.size()-2).hr + " and " + work.get(work.size()-1).hr);
									}
								}
							}
							if (doCont) //still need to assign remaining portion, last resort is to deadline
							{
								assign.get((int) n).add(work.size()-1); //index of remaining portion assigned to date n's list
								hrsLeft.set((int) n, hrsLeft.get((int) n) - work.get(work.size()-1).hr); //subtract remain portion time from deadline hrsleft
							}
							split.add(temp); //record the original's index as having been split + its dupe children
						}
						else { //deadline only day enough time, so assign to deadline
							assign.get((int) n).add(i); //index of work assigned to date n's list
							hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
						}

						//System.out.println("AFTER");
						//printDates();
					}
					else
					{
						System.out.println("\tideal" + idealN + curr.name);
						assign.get(idealN).add(i); //index of work assigned to date idealN's list
						//System.out.println(assign.get(idealN));
						hrsLeft.set(idealN, hrsLeft.get(idealN) - curr.hr);
					}
				}
			}

		}
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
	
	private int dayToIndex(DayOfWeek y) {
		
		switch (y) { 
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

	private void readDone() {
		Scanner getX = null;
		try {
			getX = new Scanner(new File("done.txt"));
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
		//System.out.println("school.txt:");
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

	private void readClass() {
		review = new ArrayList<Entry>();
		//System.out.println("school.txt:");
		LocalDate Now = LocalDate.now();
		Scanner getX = null;
		Scanner getY = null;
		try {
			getX = new Scanner(classFile);
			getY = new Scanner(new File("done.txt"));
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }

		if (getX != null && getY != null)
		{
			while(getX.hasNextLine())
			{
				String doneStat = getY.nextLine();
				String line = getX.nextLine();
				String name = line.substring(1, line.indexOf("\"",1));
				String doneSplit[] = null;
				line = line.substring(line.indexOf("\"", 1)+2); //remove name portion
				if (doneStat.indexOf("\"", 1)+2 < doneStat.length()) 
				{
					doneStat = doneStat.substring(doneStat.indexOf("\"", 1)+2); //remove name portion
					//System.out.println("FUCKING COME ON " + doneStat);
					doneSplit = doneStat.split(",");
					for (int i =0; i < doneSplit.length; i++) {
						//System.out.println(LocalDate.parse(doneSplit[i], formatter));
						//System.out.println("hi");
					}
					//System.out.println("bye");
				}
				
				String temp[] = line.split(",");
				Set<LocalDate> dontAdd = new HashSet<LocalDate>();
				if (doneSplit != null) {
					for (int i = 0; i < doneSplit.length; i++)
						dontAdd.add(LocalDate.parse(doneSplit[i], formatter));
				}
				
				for (int i = 1; i < temp.length; i++) 
				{
					//first class at this day of week: week1.plusDays(dayToIndex(initialDayOfWeek(temp[i])));
					int n = dayToIndex(initialDayOfWeek(temp[i]));
					//if class happened already
					while (n >= 0 && week1.plusDays(n).isBefore(Now) || week1.plusDays(n).isEqual(Now)) {
						if (dontAdd.contains(week1.plusDays(n))) { //marked as done
							System.out.println("---------DONE$$$$" + new Entry(name, 1.0, Double.valueOf(temp[0]), week1.plusDays(n), false) + "@" + week1.plusDays(n).getDayOfWeek());
						}
						else{
							review.add(new Entry(name, 1.0, Double.valueOf(temp[0]), week1.plusDays(n), false)) ; //add class to review todo list
							System.out.println("$$$$$$$$$$$$$$$$$" + review.get(review.size()-1) + "@" + week1.plusDays(n).getDayOfWeek());
							
						}
						n += 7; //check next week
					}
				}

			}
			getX.close();
			oldWorkSize = work.size();
			return;
		}
		return;
	}

	DayOfWeek initialDayOfWeek(String a) {
		switch(a) {
			case "M": 
				return DayOfWeek.MONDAY;
			case "T":
				return DayOfWeek.TUESDAY;
			case "W":
				return DayOfWeek.WEDNESDAY;
			case "R":
				return DayOfWeek.THURSDAY;
			default:
				return DayOfWeek.FRIDAY;
		}
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
			return String.format("\"" + name + "\"," + diff + "," + hr + "," + formatter.format(deadline));
		}
	}
}
