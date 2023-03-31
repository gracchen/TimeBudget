package try1;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ChangeListener;


public class GUI extends JFrame {
	private JPanel home, tasks, settings;
	private List<LocalDate> week;
	private List<Double> breakBudget, budget, hrsLeft; //break vs non-break budget
	private List<Entry> work; 
	private List<List<Integer>> assign;
	private List<List<String>> doneReviews; //list of ReviewEntry indexes marked doneReviews, to be deleted upon closing app
	private Set<Integer> done; //list of indexes to "remove" from work upon exit
	private JLabel msg;
	private static final long serialVersionUID = 1L;
	private File dayOfWeekConstFile, dailyConstFile, schoolWorkFile, reviewClassesFile;
	private double consts[], daily;
	private DateTimeFormatter formatter;
	private JButton toggleBreak;
	private boolean onBreak;
	private int weekDayIdeal = 1, weekEndIdeal = 6, oldWorkSize;
	private List<String> dateChoices;
	private JComboBox<String> drop;
	private JPanel show;
	private JLabel stats, leet, play;
	private List<SimpleEntry<JCheckBox, Integer>> checks;
	private JTabbedPane tabPane;
	private LocalDate week1 = LocalDate.of(2023, 3, 20); //spring quarter instruction starts April 3, this is dummy test var
	private JTable list;
	private boolean editTasks = false;
	public GUI () {
		super("TimeBudget");
		pack();
		setLocationRelativeTo(null);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				onExit();
			}
		});

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
		ChangeListener changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
		        
		        int index = tabPane.getSelectedIndex();
		        System.out.println("Tab changed to: " + tabPane.getSelectedIndex());
				if (tabPane.getSelectedIndex() == 0 && editTasks) {
					System.out.println("need to reschedule");
					editTasks = false;
					workScheduler(); //recalculate once detect edits made + switch back to home
					showSelected(drop.getSelectedIndex());
				}
			}
		};
		tabPane.addChangeListener(changeListener);

		onBreak = false; //default break mode
		formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
		dayOfWeekConstFile = new File("constantsbyDayOfWeek.txt");
		dailyConstFile = new File("dailyConstants.txt");
		schoolWorkFile = new File("schoolWork.txt");
		reviewClassesFile = new File("reviewClasses.txt");
		consts = new double[7];
		daily = 0;

		week = new LinkedList<LocalDate>();
		budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
		work = new ArrayList<Entry>();
		doneReviews = new ArrayList<List<String>>();
		done = new HashSet<Integer>();
		readDayOfWeekConstants();
		readDailyConsts();
		readSchoolWork();
		addReviewToDo();

		LocalDate curr = LocalDate.now();
		double weekendBudget = 0.0;
		dateChoices = new ArrayList<String>();
		dateChoices.add("Today");
		dateChoices.add("Tomorrow");
		for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
		{
			week.add(curr);
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr)]);
			if (curr.getDayOfWeek() == DayOfWeek.SATURDAY) weekendBudget = budget.get(i);
			if (i >= 2) dateChoices.add((curr.getDayOfWeek()) + ", " + formatter.format(curr));
		}

		breakBudget = new ArrayList<Double>(Collections.nCopies(7,weekendBudget));

		Collections.sort(work, new EntrySort());

		workScheduler();

		//GUI PART!!!msg = new JLabel("hi");
		msg = new JLabel();
		home.add(msg);

		toggleBreak = new JButton("off break");
		toggleBreak.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						onBreak = !onBreak; //toggle bool
						System.out.println(onBreak);
						toggleBreak.setText(onBreak? "on break" : "off break");
						workScheduler();
						printBudget();
						printAssigned();
						printWork();
						showSelected(drop.getSelectedIndex()); //update
					}
				}
				);

		home.add(toggleBreak);
		//DROPDOWN GUI:
		drop = new JComboBox<String>(dateChoices.toArray(new String[dateChoices.size()])); //param = array of options
		home.add(drop);
		showSelected(0); //default show today
		drop.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent event) {
						if(event.getStateChange() == ItemEvent.SELECTED) {

							showSelected(drop.getSelectedIndex());
						}
							
					}
				}
				);

		//tasks
		list = new JTable(new Table()) ;
		tasks.add(list);

		//settings

		//writeWork();
		System.out.println("\nNow Printing Work[]: ");
		printWork();
	}


	private class Table extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String getColumnName(int col) {
			return (col == 0 ? "Name":"Deadline");
		}

		public int getRowCount() { return oldWorkSize; }

		public int getColumnCount() { return 2; }

		public Object getValueAt(int row, int col) {
			if (col == 0) return work.get(row).name;
			else return formatter.format(work.get(row).deadline);
		}

		public boolean isCellEditable(int row, int col) { return true;}

		public void setValueAt(Object value, int row, int col) {
			if (!editTasks) editTasks = true;
			if (col == 0) work.get(row).name = (String) value;
			if (col == 1) {
				try {
					LocalDate d = LocalDate.parse((String) value, formatter);
					work.get(row).deadline = d;
				} catch (Exception e) {
					System.out.println(value+"invalid date");
				}
			}
		}
	}

	public void onExit() {
		commitDone();
		commitWork();

		System.err.println("Exit");
		System.exit(0);
	}

	void showSelected(int index) { 
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

		class checkHandler implements ItemListener { 
			public void itemStateChanged(ItemEvent event) { 
				for (int i = 0; i < checks.size(); ) {
					if (checks.get(i).getKey().isSelected()) {
						int curr = checks.get(i).getValue();
						if (work.get(curr) instanceof DupeEntry) //currently checked off is dupe
						{
							int parent = ((DupeEntry)work.get(curr)).parent; //parent
							System.out.println("work["+curr+"] is dupe of work["+parent+"]");
							work.get(parent).hr -= work.get(curr).hr; //INSTEAD OF OVERCOMPLEX MERGING DUPES
							if (work.get(parent).hr <= 0) { //last dupe of entry
								if (work.get(parent) instanceof ReviewEntry) {
									int classIndex = ((ReviewEntry)work.get(parent)).classIndex;//mark parent review as doneReviews
									String temp[] = work.get(parent).name.split(" ");
									doneReviews.get(classIndex).add(temp[temp.length-1]); //get week#.lect# of name
									System.out.println("\tfinished " + String.valueOf(temp[temp.length-1]) + " to class " + classIndex);
								}
								else done.add(parent); //if last dupe of non-review entry, also mark to remove upon exit
							}
						}
						else
						{
							if (work.get(curr) instanceof ReviewEntry)
							{
								System.out.println("work[" + checks.get(i).getValue() +"] is review");
								int classIndex = ((ReviewEntry)work.get(curr)).classIndex; //mark review as doneReviews
								String temp[] = work.get(curr).name.split(" ");
								doneReviews.get(classIndex).add(temp[temp.length-1]); //get week#.lect# of name
								System.out.println("\tfinished " + String.valueOf(temp[temp.length-1]) + " to class " + classIndex);
							}
							else {
								System.out.println("work[" + curr +"] NOT review");
								done.add(curr); //if non-review entry, remove checked upon exit
							}
						}
						//unintended awesome design: don't have to edit work[ ] itself, messing up indexes. Simply
						//unassign from assign [ ], voila! 

						assign.get(drop.getSelectedIndex()).remove(checks.get(i).getValue()); //unassign checked item
						show.remove(checks.get(i).getKey());
						checks.remove(i);
						revalidate(); //to refresh removal
					}
					else i++;
				}
			}
		}

		c.fill = GridBagConstraints.HORIZONTAL;    //fill entire cell with text to center
		c.gridwidth = 4; c.gridx = 0; c.gridy = 0;   //coords + width of msg element
		checks = new ArrayList<SimpleEntry<JCheckBox, Integer>>(); //handler needs to check if is ReviewEntry, so also store int (index to assign)
		checkHandler handler = new checkHandler();
		for (int i = 0; i < assign.get(index).size(); i++) {

			checks.add(new SimpleEntry<JCheckBox, Integer>(new JCheckBox(work.get(assign.get(index).get(i)).name + ",  " + work.get(assign.get(index).get(i)).hr + "h"), assign.get(index).get(i)));
			checks.get(checks.size()-1).getKey().setToolTipText("due " + formatter.format(work.get(assign.get(index).get(i)).deadline));
			//System.out.println(":(" + checks.get(checks.size()-1).getKey().getText() + " " + checks.get(checks.size()-1).getValue());

			checks.get(checks.size()-1).getKey().addItemListener(handler);
			show.add(checks.get(checks.size()-1).getKey(), c);
			c.gridy++;
		}

		stats = new JLabel(hrsLeft.get(index) + "/" + (onBreak? breakBudget.get(index) : budget.get(index)) + "h free"); 
		show.add(stats, c);
		if (hrsLeft.get(index) > 0)
		{
			c.gridy++; //%.2f to format double show 2 decimal places max
			leet = new JLabel(formatDuration(hrsLeft.get(index) * 0.6) + " for CS");
			show.add(leet,c);
			c.gridy++;
			play = new JLabel(String.format("%.1fh to play", hrsLeft.get(index) * 0.4));
			play = new JLabel(formatDuration(hrsLeft.get(index) * 0.4) + " to play");
			show.add(play,c);
		}	
		
		revalidate();
	}

	String formatDuration(double x) {
		if (Math.ceil(x) == Math.floor(x)) //no decimal part
			return (String.format("%.0fh", x));
		return (String.format("%.0fh %.0fm", x, (x - Math.floor(x))*60));
	}

	class EntrySort implements Comparator<Entry> { //MY FIRST JAVA CUSTOM SORT FUNC!
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
			System.out.println(i + ":" + work.get(i).toString());

		if (work.size() != oldWorkSize)
			System.out.println("________________________");

		for (int i = oldWorkSize; i < work.size(); i++) //print sorted homework entries
			System.out.println(i + ":" + work.get(i).toString());
	}

	void printBudget() {
		LocalDate curr = LocalDate.now();
		System.out.println("Date\tdayOfWeek  budget");
		if (onBreak) {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + breakBudget.get(i));
		}
		else {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
		}
	}

	void printAssigned() {
		System.out.println("printing dates, remaining budget out of max, assigned work");
		if (onBreak) {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + "\n\t");
				for (int j = 0; j < assign.get(i).size(); j++) System.out.println(work.get(assign.get(i).get(j)));
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + breakBudget.get(i) + "h free\n\n");
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

	private void workScheduler() {	
		if (onBreak) hrsLeft = new ArrayList<Double>(breakBudget);
		else hrsLeft = new ArrayList<Double>(budget);
		work.subList(oldWorkSize, work.size()).clear(); //removes any copies of originally read work
		//that was inserted after, from indexes oldWorkSize, oldWorkSize+1......work.size()-1
		assign = new ArrayList<List<Integer>>(); //reset assign

		for (int i = 0; i < 7; i++)
		{
			ArrayList<Integer> temp = new ArrayList<Integer>();
			assign.add(temp);
		}

		//for (int i = 0; i < 7; i++) System.out.println(hrsLeft.get(i));
		List<Integer> reviewToDo = new ArrayList<Integer>(); //make it very last priority
		for (int i = 0; i < oldWorkSize; i++) {
			Entry curr = work.get(i);
			//System.out.println(formatter.format(curr.deadline) + ":" + ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline) + "days " + dayToIndex(curr.deadline));
			long n = ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline);
			if (work.get(i) instanceof ReviewEntry) reviewToDo.add(i); //deal with all review entries at end less priority than true deadlines
			else if (n >= 0 && n < 7) {
				System.out.println("assignEntry(" + i + ", f);");
				assignEntry(i, false);
			}
		}
		for (int i = 0; i < reviewToDo.size(); i++) {
			System.out.println("assignEntry(" + reviewToDo.get(i) + ", t);");
			assignEntry(reviewToDo.get(i), true);
		}
	}


	private void assignEntry(Integer i, boolean isReview) { 
		Entry curr = work.get(i);
		int n = (int) ChronoUnit.DAYS.between(LocalDate.now(), curr.deadline);
		if (n<=0 && isReview) n = 8; //deadline passed, assign review to any day this week

		//System.out.println("HI");
		//for (int p = 0; p < 7;p++) System.out.println(hrsLeft.get(p));
		//verify n is corresponding week index for curr.deadline System.out.println(formatter.format(curr.deadline) + "=?"+ formatter.format(week.get((int) n)));
		if (!isReview && curr.isFixed) 
		{
			//System.out.println("\tfixed");
			assign.get((int) n).add(i); //index of work assigned to date n's list
			hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
		}
		else {
			if (n==0) n++;
			int idealN = -1;

			for (int j = 0; j < n-1; j++) //assign today? tmrw? day after? 
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
				if (hrsLeft.get((int) n-1) - curr.hr < 0) //if need to split bc deadline also not enough time
				{
					List<Integer> temp = new ArrayList<Integer>();
					temp.add(i); //first element is parent original

					work.add(new DupeEntry(i, work.get(i).name, work.get(i).diff, work.get(i).hr, work.get(i).deadline, work.get(i).isFixed)); //make copy of original entry at back of work[], cannot override original (for break toggle)
					int numDupes = 1;
					temp.add(work.size()-1); //record first child of original
					boolean doCont = true;
					if (isReview) n=8; //Review is soft deadline and no ideal days, so try fit all days before negative hours
					for (int j = 0; j < n-1 && doCont; j++) //assign today? tmrw? day after? 
					{
						if (hrsLeft.get(j) > 0) //has some time to squeeze this task
						{
							//System.out.println("hey! day "+  week.get(j) + " has hrs: " + hrsLeft.get(j));
							if (work.get(work.size()-1).hr <= hrsLeft.get(j)) { //doneReviews! all parts fitted
								//System.out.println("it's okay, " + work.get(work.size()-1).name + " only needs " + work.get(work.size()-1).hr);
								doCont = false;
								assign.get(j).add(work.size()-1); //direct assign to day j
								hrsLeft.set(j, hrsLeft.get(j) - work.get(work.size()-1).hr); //update hours of day j
							}
							else { //gotta do more splitting
								numDupes++;
								work.add(new DupeEntry(i, work.get(i).name, work.get(i).diff, work.get(i).hr, work.get(i).deadline, work.get(i).isFixed)); //copy
								temp.add(work.size()-1); //save new child
								work.get(work.size()-2).hr = hrsLeft.get(j); //assign previous copy to day j
								assign.get(j).add(work.size()-2);

								work.get(work.size()-1).hr -= hrsLeft.get(j); //save remaining portion of task in new copy
								hrsLeft.set(j, 0.0); //which eats up all of day j's hrs left.
								//System.out.println("now: " + work.get(work.size()-2).hr + " and " + work.get(work.size()-1).hr);
							}
						}
					}
					if (doCont || numDupes==1) //still need to assign remaining portion, last resort is to deadline
					{
						System.out.println( work.get(i).name + "checking numDupes as "+ numDupes);
						if (numDupes==1) {
							if (n==8) n--;
							work.remove(work.size()-1); //remove dupe, no need, just assign original to deadline
							assign.get((int) n-1).add(i); //index of remaining portion assigned to date n's list
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - work.get(i).hr); //subtract remain portion time from deadline hrsleft
						}
						else {
							assign.get((int) n-1).add(work.size()-1); //index of remaining portion assigned to date n's list
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - work.get(work.size()-1).hr); //subtract remain portion time from deadline hrsleft
						}
					}
					/*else if (numDupes==1)
					{
						work.remove(work.size()-1); //remove dupe, no need, just assign original to deadline
					}*/
				}
				else { //deadline only day enough time, so assign to deadline
					assign.get((int) n-1).add(i); //index of work assigned to date n's list
					hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - curr.hr);
				}

				//System.out.println("AFTER");
				//printDates();
			}
			else
			{
				//System.out.println("\tideal" + idealN + curr.name);
				assign.get(idealN).add(i); //index of work assigned to date idealN's list
				//System.out.println(assign.get(idealN));
				hrsLeft.set(idealN, hrsLeft.get(idealN) - curr.hr);
			}
		}
	}

	private int dayToIndex(LocalDate y) {
		return dayToIndex(y.getDayOfWeek());
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

	private void readDayOfWeekConstants() {
		Scanner getX = null;
		try {
			getX = new Scanner(dayOfWeekConstFile);
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

	private void readDailyConsts() {
		Scanner getX = null;
		try {
			getX = new Scanner(dailyConstFile);
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

	private void readSchoolWork() {
		//System.out.println("schoolWork.txt:");
		Scanner getX = null;

		try {
			getX = new Scanner(schoolWorkFile);
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

	private int daysBetweenDayOfWeeks(String x, String y) { //crappy slow way of implementing this
		LocalDate now = LocalDate.now();
		DayOfWeek a = initialDayOfWeek(x);
		DayOfWeek b = initialDayOfWeek(y);
		int A=0; 
		for (; now.plusDays(A).getDayOfWeek() != a; A++);
		int B=A;
		for (; now.plusDays(B).getDayOfWeek() != b; B++);
		//System.out.println("daysBetweenDayOfWeeks("+x+","+y+")="+(B-A));
		return (B-A);
	}

	private void addReviewToDo() {
		System.out.println("Generating Review Sessions...");
		LocalDate Now = LocalDate.now();
		Scanner getX = null;
		Scanner getY = null;
		try {
			getX = new Scanner(reviewClassesFile);
			getY = new Scanner(new File("doneReviews.txt"));
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }

		if (getX != null && getY != null)
		{
			int classIndex = 0;
			while(getX.hasNextLine())
			{
				List<String> temp = new ArrayList<String>();
				doneReviews.add(temp);
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

				String temp1[] = line.split(",");
				Set<String> dontAdd = null;
				if (doneSplit != null) {
					dontAdd = new HashSet<String>(Arrays.asList(doneSplit));
				}

				for (int i = 1; i < temp1.length; i++) 
				{
					//first class at this day of week: week1.plusDays(dayToIndex(initialDayOfWeek(temp[i])));
					int nDeadline;
					int n = dayToIndex(initialDayOfWeek(temp1[i]));
					if (i+1 == temp1.length) nDeadline = daysBetweenDayOfWeeks(temp1[i], temp1[1]);
					else nDeadline  = daysBetweenDayOfWeeks(temp1[i], temp1[i+1]);
					//System.out.println("\t" + nDeadline + " " + i);
					//if class happened already
					while (n >= 0 && week1.plusDays(n).isBefore(Now) || week1.plusDays(n).isEqual(Now)) {

						String lectID = (n/7+1)+"."+i; //week.lect#  i.e. 4.1 means 1st lecture of week 4

						if (dontAdd != null && dontAdd.contains(lectID)) { //marked as doneReviews
							System.out.println("\t finished" + new Entry(name +" Lecture "+lectID, 1.0, Double.valueOf(temp1[0]), week1.plusDays(n+nDeadline), false) + "@" + week1.plusDays(n).getDayOfWeek());
						}
						else{
							work.add(new ReviewEntry(classIndex, name +" Lecture "+lectID, 1.0, Double.valueOf(temp1[0]), week1.plusDays(n+nDeadline), false)) ; //add class to review todo list
							System.out.println("\t" + work.get(work.size()-1) + "@" + week1.plusDays(n+nDeadline).getDayOfWeek());
						}
						n += 7; //check next week
					}
				}
				classIndex++;
			}
			getX.close();
			getY.close();
			oldWorkSize = work.size();
			return;
		}
		return;
	}

	private void commitWork() {
		Formatter newDone = null;
		Scanner oldDone = null;
		File oldDoneFile = null;
		Iterator itr = done.iterator();
		while (itr.hasNext())
			System.out.println(itr.next());

		try {
			oldDoneFile = new File("schoolWork.txt");
			oldDone = new Scanner(oldDoneFile);
			newDone = new Formatter("~schoolWork.txt");
			//System.out.println("You created a file");
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (newDone != null)
		{
			for (int i = 0; i < oldWorkSize; i++) {
				if (!done.contains(i) && !(work.get(i) instanceof ReviewEntry)) newDone.format("%s\n", work.get(i).toCSV());
			}
			newDone.close();
			oldDone.close();

			oldDoneFile.delete();

			if (oldDoneFile.exists()) System.out.println("unable to edit schoolWork.txt");
			File edit = new File ("~schoolWork.txt");
			edit.renameTo(oldDoneFile);
		}
	}

	private void commitDone() {
		Formatter newDone = null;
		Scanner oldDone = null;
		File oldDoneFile = null;
		try {
			oldDoneFile = new File("doneReviews.txt");
			oldDone = new Scanner(oldDoneFile);
			newDone = new Formatter("~doneReviews.txt");
			//System.out.println("You created a file");
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (newDone != null)
		{
			int i = 0;
			while(oldDone.hasNextLine())
			{
				String newDoneLine = oldDone.nextLine();

				for (int j = 0; j < doneReviews.get(i).size(); j++)
					newDoneLine+=","+doneReviews.get(i).get(j);

				i++;
				newDone.format("%s\n", newDoneLine); //update icon path
			}
			newDone.close();
			oldDone.close();

			oldDoneFile.delete();

			if (oldDoneFile.exists()) System.out.println("unable to edit doneReviews.txt");
			File edit = new File ("~doneReviews.txt");
			edit.renameTo(oldDoneFile);
		}
	}


	private void append(String filename, final String s) throws IOException {
		try
		{
			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
			fw.write("add a line\n");//appends the string to the file
			fw.close();
		}
		catch(IOException ioe)
		{
			System.err.println("IOException: " + ioe.getMessage());
		}
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
			return String.format("\"" + name + "\"\t" + diff + "\t" + hr + "\t" + formatter.format(deadline) + "\t" + isFixed);
		}

		public String toCSV() {
			return String.format("\"" + name + "\"," + diff + "," + hr + "," + formatter.format(deadline) + (isFixed? "," + isFixed : ""));
		}
	}

	private class ReviewEntry extends Entry {
		int classIndex;
		ReviewEntry(int c, String n, double di, double h, LocalDate de, boolean t) {
			super(n,di,h,de,t);
			classIndex = c;
		}
	}

	private class DupeEntry extends Entry {
		int parent; //saves index of original Entry
		DupeEntry(int p, String n, double di, double h, LocalDate de, boolean t) {
			super(n,di,h,de,t);
			parent = p;
		}
	}
}
